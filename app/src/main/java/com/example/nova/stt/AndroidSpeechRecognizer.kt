package com.example.nova.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as SysSpeechRecognizer
import android.util.Log
import com.example.nova.audio.AudioLevelMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioLevelMonitor: AudioLevelMonitor
) : SpeechRecognizer {

    private val tag = "AndroidSpeechRecognizer"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _speechResults = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 64)
    override val speechResults: SharedFlow<SpeechResult> = _speechResults.asSharedFlow()

    private var speechRecognizer: SysSpeechRecognizer? = null
    private val isListeningActive = AtomicBoolean(false)

    override suspend fun startListening(): Boolean {
        if (isListeningActive.get()) return true

        return try {
            mainHandler.post {
                initAndStartSystemRecognizer()
            }
            isListeningActive.set(true)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start Android SpeechRecognizer", e)
            false
        }
    }

    private fun initAndStartSystemRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SysSpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(tag, "System SpeechRecognizer ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(tag, "System SpeechRecognizer: speech started")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 10f).coerceIn(0f, 1f)
                        audioLevelMonitor.setDirectLevel(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(tag, "System SpeechRecognizer: end of speech")
                        audioLevelMonitor.reset()
                        scope.launch {
                            _speechResults.emit(SpeechResult.EndOfUtterance)
                        }
                    }

                    override fun onError(error: Int) {
                        Log.w(tag, "System SpeechRecognizer error code: $error")
                        audioLevelMonitor.reset()
                        scope.launch {
                            if (error == SysSpeechRecognizer.ERROR_NO_MATCH ||
                                error == SysSpeechRecognizer.ERROR_SPEECH_TIMEOUT
                            ) {
                                _speechResults.emit(SpeechResult.EndOfUtterance)
                            } else {
                                _speechResults.emit(SpeechResult.Error("Recognition error: $error"))
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SysSpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        Log.d(tag, "System SpeechRecognizer final result: '$text'")
                        audioLevelMonitor.reset()
                        scope.launch {
                            if (text.isNotBlank()) {
                                _speechResults.emit(SpeechResult.Final(text))
                            }
                            _speechResults.emit(SpeechResult.EndOfUtterance)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SysSpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = matches?.firstOrNull() ?: ""
                        if (partialText.isNotBlank()) {
                            Log.d(tag, "System SpeechRecognizer partial: '$partialText'")
                            scope.launch {
                                _speechResults.emit(SpeechResult.Partial(partialText))
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error setting up system recognizer", e)
        }
    }

    override fun feedAudio(buffer: ShortArray, readSize: Int) {
        // System SpeechRecognizer captures audio directly from audio framework
    }

    override suspend fun stopListening() {
        if (!isListeningActive.get()) return
        isListeningActive.set(false)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(tag, "Error stopping system recognizer", e)
            }
        }
    }

    override fun cancel() {
        isListeningActive.set(false)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(tag, "Error cancelling system recognizer", e)
            }
        }
    }

    override fun isListening(): Boolean = isListeningActive.get()
}
