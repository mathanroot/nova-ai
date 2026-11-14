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
    private val speechRecognizer: VoskSpeechRecognizer,
    private val audioLevelMonitor: AudioLevelMonitor
) {
    private val tag = "SpeechSessionManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    private val speechConfig = SpeechConfig()
    private val vad = VoiceActivityDetector(speechConfig.speechEnergyThresholdDb)
    private val utteranceManager = UtteranceManager(speechConfig)

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
            Log.e(tag, "Failed to obtain microphone ownership for Vosk recognition")
            return false
        }

        vad.reset()
        utteranceManager.reset()
        _liveTranscription.value = ""
        _isUserPaused.value = false

        val recognizerReady = speechRecognizer.startListening()
        if (!recognizerReady) {
            Log.w(tag, "Speech recognizer failed to start. Release mic.")
            microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
            return false
        }

        val recorder = AudioRecorder(audioLevelMonitor)
        audioRecorder = recorder
        microphoneManager.attachRecorder(recorder)

        val recordSuccess = recorder.start(scope) { buffer, readSize ->
            vad.processFrame(buffer, readSize)
            speechRecognizer.feedAudio(buffer, readSize)
        }

        if (!recordSuccess) {
            Log.e(tag, "Failed to start AudioRecorder")
            speechRecognizer.stopListening()
            microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
            return false
        }

        startRecognitionCollector()
        startSilenceMonitoring()
        Log.d(tag, "Listening session started successfully")
        return true
    }

    private fun startRecognitionCollector() {
        recognitionCollectorJob?.cancel()
        recognitionCollectorJob = scope.launch {
            speechRecognizer.speechResults.collect { result ->
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
                        Log.e(tag, "SpeechResult error: ${result.error}")
                    }
                    is SpeechResult.EndOfUtterance -> {
                        // Handled by silence monitor / finalization
                    }
                    SpeechResult.Empty -> Unit
                }
            }
        }
    }

    private fun startSilenceMonitoring() {
        silenceMonitorJob?.cancel()
        silenceMonitorJob = scope.launch {
            while (isActive && audioRecorder?.isRecordingActive() == true) {
                delay(100) // Check every 100ms
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

        speechRecognizer.stopListening()
        microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)

        val finalUtterance = utteranceManager.finalizeUtterance()
        _isUserPaused.value = false
        return finalUtterance
    }

    fun cancelSession() {
        Log.d(tag, "Cancelling speech session")
        silenceMonitorJob?.cancel()
        silenceMonitorJob = null
        recognitionCollectorJob?.cancel()
        recognitionCollectorJob = null

        audioRecorder?.stopAndRelease()
        audioRecorder = null
        speechRecognizer.cancel()
        scope.launch {
            microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
        }
        vad.reset()
        utteranceManager.reset()
        _liveTranscription.value = ""
        _isUserPaused.value = false
    }

    fun isListening(): Boolean = audioRecorder?.isRecordingActive() == true
}
