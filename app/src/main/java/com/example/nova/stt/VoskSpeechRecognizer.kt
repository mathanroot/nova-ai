package com.example.nova.stt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskSpeechRecognizer @Inject constructor(
    private val modelManager: ModelManager
) : SpeechRecognizer {

    private val tag = "VoskSpeechRecognizer"
    private val _speechResults = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 64)
    override val speechResults: SharedFlow<SpeechResult> = _speechResults.asSharedFlow()

    private var voskRecognizer: Recognizer? = null
    private val isListeningActive = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default)

    override suspend fun startListening(): Boolean {
        if (isListeningActive.get()) {
            return true
        }

        val model = modelManager.getOrLoadModel()
        if (model == null) {
            Log.e(tag, "Cannot start listening: Vosk model is not loaded")
            _speechResults.emit(SpeechResult.Error("Vosk speech model is not ready. Please install model."))
            return false
        }

        try {
            voskRecognizer = Recognizer(model, 16000.0f)
            isListeningActive.set(true)
            Log.d(tag, "Vosk Recognizer started listening")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to instantiate Vosk Recognizer", e)
            _speechResults.emit(SpeechResult.Error("Failed to initialize speech recognizer", e))
            return false
        }
    }

    override fun feedAudio(buffer: ShortArray, readSize: Int) {
        if (!isListeningActive.get() || voskRecognizer == null) return

        try {
            val recognizer = voskRecognizer ?: return
            val isFinalChunk = recognizer.acceptWaveForm(buffer, readSize)

            if (isFinalChunk) {
                val jsonStr = recognizer.result
                val text = parseVoskText(jsonStr)
                if (text.isNotBlank()) {
                    scope.launch {
                        _speechResults.emit(SpeechResult.Final(text))
                    }
                }
            } else {
                val jsonStr = recognizer.partialResult
                val partialText = parseVoskPartial(jsonStr)
                if (partialText.isNotBlank()) {
                    scope.launch {
                        _speechResults.emit(SpeechResult.Partial(partialText))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error feeding audio to Vosk", e)
        }
    }

    override suspend fun stopListening() {
        if (!isListeningActive.get()) return
        isListeningActive.set(false)

        try {
            voskRecognizer?.let { recognizer ->
                val finalJson = recognizer.finalResult
                val finalText = parseVoskText(finalJson)
                if (finalText.isNotBlank()) {
                    _speechResults.emit(SpeechResult.Final(finalText))
                }
                recognizer.close()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error closing Vosk Recognizer", e)
        } finally {
            voskRecognizer = null
            _speechResults.emit(SpeechResult.EndOfUtterance)
            Log.d(tag, "Vosk Recognizer stopped listening")
        }
    }

    override fun cancel() {
        isListeningActive.set(false)
        try {
            voskRecognizer?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error cancelling recognizer", e)
        } finally {
            voskRecognizer = null
        }
    }

    override fun isListening(): Boolean = isListeningActive.get()

    private fun parseVoskText(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseVoskPartial(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            json.optString("partial", "")
        } catch (e: Exception) {
            ""
        }
    }
}
