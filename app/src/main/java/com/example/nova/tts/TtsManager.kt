package com.example.nova.tts

import android.util.Log
import com.example.nova.audio.AudioFocusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    private val ttsEngine: AndroidTtsEngine,
    private val audioFocusManager: AudioFocusManager
) {
    private val tag = "TtsManager"
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init() {
        ttsEngine.initTts { ready ->
            Log.d(tag, "TTS ready status: $ready")
        }
    }

    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        audioFocusManager.requestTransientFocus()
        val utteranceId = UUID.randomUUID().toString()

        ttsEngine.speak(
            text = text,
            utteranceId = utteranceId,
            onStart = {
                _isSpeaking.value = true
                onStart?.invoke()
            },
            onDone = {
                _isSpeaking.value = false
                audioFocusManager.abandonFocus()
                onDone?.invoke()
            },
            onError = { err ->
                _isSpeaking.value = false
                audioFocusManager.abandonFocus()
                Log.e(tag, "TTS speaking error: $err")
                onError?.invoke(err)
            }
        )
    }

    fun stop() {
        ttsEngine.stop()
        _isSpeaking.value = false
        audioFocusManager.abandonFocus()
    }

    fun shutdown() {
        ttsEngine.shutdown()
        _isSpeaking.value = false
        audioFocusManager.abandonFocus()
    }
}
