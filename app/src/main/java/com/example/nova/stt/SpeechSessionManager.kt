package com.example.nova.stt

import android.util.Log
import com.example.nova.audio.AudioLevelMonitor
import com.example.nova.audio.AudioRecorder
import com.example.nova.audio.MicrophoneManager
import com.example.nova.audio.MicrophoneOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechSessionManager @Inject constructor(
    private val microphoneManager: MicrophoneManager,
    private val voskSpeechRecognizer: VoskSpeechRecognizer,
    private val androidSpeechRecognizer: AndroidSpeechRecognizer,
    private val audioLevelMonitor: AudioLevelMonitor,
    private val modelManager: ModelManager
) {
    private val tag = "SpeechSessionManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    private val speechConfig = SpeechConfig()
    private val vad = VoiceActivityDetector(speechConfig.speechEnergyThresholdDb)
    private val utteranceManager = UtteranceManager(speechConfig)

    private var activeRecognizer: SpeechRecognizer = voskSpeechRecognizer
    private var audioRecorder: AudioRecorder? = null
    private var silenceMonitorJob: Job? = null
    private var recognitionCollectorJob: Job? = null

    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _isUserPaused = MutableStateFlow(false)
    val isUserPaused: StateFlow<Boolean> = _isUserPaused.asStateFlow()

    private val _utteranceCompleted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val utteranceCompleted: SharedFlow<String> = _utteranceCompleted.asSharedFlow()

    suspend fun startListeningSession(): Boolean {
        Log.d(tag, "Starting speech listening session...")

        val ownershipGranted = microphoneManager.requestOwnership(MicrophoneOwner.VOSK_RECOGNITION)
        if (!ownershipGranted) {
            Log.e(tag, "Failed to obtain microphone ownership for recognition")
            return false
        }

        vad.reset()
        utteranceManager.reset()
        _liveTranscription.value = ""
        _isUserPaused.value = false

        // Select best available offline recognizer
        var recognizerReady = false
        if (modelManager.isModelReady()) {
            recognizerReady = voskSpeechRecognizer.startListening()
            if (recognizerReady) {
                activeRecognizer = voskSpeechRecognizer
                Log.d(tag, "Using Vosk Speech Recognizer")
            }
        }

        if (!recognizerReady) {
            Log.d(tag, "Vosk model not loaded; using on-device Android SpeechRecognizer")
            recognizerReady = androidSpeechRecognizer.startListening()
            if (recognizerReady) {
                activeRecognizer = androidSpeechRecognizer
                Log.d(tag, "Using Android On-Device Speech Recognizer")
            }
        }

        if (!recognizerReady) {
            Log.w(tag, "Neither recognizer could start. Releasing mic ownership.")
            microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
            return false
        }

        if (activeRecognizer == voskSpeechRecognizer) {
            val recorder = AudioRecorder(audioLevelMonitor)
            audioRecorder = recorder
            microphoneManager.attachRecorder(recorder)

            val recordSuccess = recorder.start(scope) { buffer, readSize ->
                vad.processFrame(buffer, readSize)
                activeRecognizer.feedAudio(buffer, readSize)
            }

            if (!recordSuccess) {
                Log.e(tag, "Failed to start AudioRecorder")
                activeRecognizer.stopListening()
                microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
                return false
            }
        }

        startRecognitionCollector()
        startSilenceMonitoring()
        Log.d(tag, "Listening session started successfully with ${activeRecognizer::class.simpleName}")
        return true
    }

    private fun startRecognitionCollector() {
        recognitionCollectorJob?.cancel()
        recognitionCollectorJob = scope.launch {
            activeRecognizer.speechResults.collect { result ->
                when (result) {
                    is SpeechResult.Partial -> {
                        val currentText = utteranceManager.onPartialResult(result.text)
                        _liveTranscription.value = currentText
                        _isUserPaused.value = false
                    }
                    is SpeechResult.Final -> {
                        utteranceManager.onChunkFinalResult(result.text)
                        _liveTranscription.value = utteranceManager.getFullTranscript()
                    }
                    is SpeechResult.Error -> {
                        Log.w(tag, "SpeechResult error: ${result.error}")
                    }
                    is SpeechResult.EndOfUtterance -> {
                        val finalSentence = utteranceManager.finalizeUtterance()
                        if (finalSentence.isNotBlank()) {
                            _liveTranscription.value = finalSentence
                            _utteranceCompleted.emit(finalSentence)
                        }
                    }
                    SpeechResult.Empty -> Unit
                }
            }
        }
    }

    private fun startSilenceMonitoring() {
        silenceMonitorJob?.cancel()
        silenceMonitorJob = scope.launch {
            while (isActive && isListening()) {
                delay(100)
                val silenceMs = vad.getSilenceDurationMs()
                val totalSpeechMs = vad.getTotalSpeechDurationMs()

                if (vad.isSpeechActive()) {
                    if (silenceMs in speechConfig.shortPauseThresholdMs..speechConfig.finalSilenceTimeoutMs) {
                        _isUserPaused.value = true
                    } else if (silenceMs < speechConfig.shortPauseThresholdMs) {
                        _isUserPaused.value = false
                    }
                }

                if (utteranceManager.shouldFinalize(silenceMs, totalSpeechMs)) {
                    Log.d(tag, "Silence threshold reached. Finalizing utterance.")
                    val finalSentence = utteranceManager.finalizeUtterance()
                    _liveTranscription.value = finalSentence
                    _utteranceCompleted.emit(finalSentence)
                    stopListeningSession()
                    break
                }
            }
        }
    }

    suspend fun stopListeningSession(): String {
        Log.d(tag, "Stopping listening session")
        silenceMonitorJob?.cancel()
        silenceMonitorJob = null
        recognitionCollectorJob?.cancel()
        recognitionCollectorJob = null

        audioRecorder?.stopAndRelease()
        audioRecorder = null

        activeRecognizer.stopListening()
        microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)

        val finalUtterance = utteranceManager.finalizeUtterance()
        _isUserPaused.value = false
        return finalUtterance
    }

    fun forceFinalizeCurrentUtterance() {
        val sentence = utteranceManager.finalizeUtterance()
        if (sentence.isNotBlank()) {
            _liveTranscription.value = sentence
            scope.launch {
                _utteranceCompleted.emit(sentence)
                stopListeningSession()
            }
        } else {
            scope.launch {
                stopListeningSession()
            }
        }
    }

    fun cancelSession() {
        Log.d(tag, "Cancelling speech session")
        silenceMonitorJob?.cancel()
        silenceMonitorJob = null
        recognitionCollectorJob?.cancel()
        recognitionCollectorJob = null

        audioRecorder?.stopAndRelease()
        audioRecorder = null
        activeRecognizer.cancel()
        scope.launch {
            microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
        }
        vad.reset()
        utteranceManager.reset()
        _liveTranscription.value = ""
        _isUserPaused.value = false
    }

    fun isListening(): Boolean = audioRecorder?.isRecordingActive() == true || activeRecognizer.isListening()
}
