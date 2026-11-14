package com.example.nova.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class PendingSpeech(
    val text: String,
    val utteranceId: String,
    val onStart: () -> Unit,
    val onDone: () -> Unit,
    val onError: (String) -> Unit
)

@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine, TextToSpeech.OnInitListener {

    private val tag = "AndroidTtsEngine"
    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)
    private val initCallbacks = mutableListOf<(Boolean) -> Unit>()
    private val pendingQueue = mutableListOf<PendingSpeech>()

    private var activeOnStart: (() -> Unit)? = null
    private var activeOnDone: (() -> Unit)? = null
    private var activeOnError: ((String) -> Unit)? = null

    override fun initTts(onReady: (Boolean) -> Unit) {
        if (isInitialized.get() && tts != null) {
            onReady(true)
            return
        }

        synchronized(initCallbacks) {
            initCallbacks.add(onReady)
        }

        if (isInitializing.compareAndSet(false, true)) {
            Log.d(tag, "Instantiating TextToSpeech...")
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        isInitializing.set(false)
        if (status == TextToSpeech.SUCCESS) {
            val ttsInstance = tts ?: return

            val langResult = ttsInstance.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(tag, "Locale.US missing data, falling back to default device locale")
                ttsInstance.setLanguage(Locale.getDefault())
            }

            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                ttsInstance.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w(tag, "Failed to set audio attributes on TTS", e)
            }

            ttsInstance.setSpeechRate(1.0f)
            ttsInstance.setPitch(1.0f)

            setupProgressListener(ttsInstance)
            isInitialized.set(true)
            Log.d(tag, "TTS initialized and ready for playback!")

            synchronized(initCallbacks) {
                initCallbacks.forEach { it.invoke(true) }
                initCallbacks.clear()
            }

            // Flush any pending speeches
            synchronized(pendingQueue) {
                while (pendingQueue.isNotEmpty()) {
                    val req = pendingQueue.removeAt(0)
                    speak(req.text, req.utteranceId, req.onStart, req.onDone, req.onError)
                }
            }
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech (status: $status)")
            isInitialized.set(false)
            synchronized(initCallbacks) {
                initCallbacks.forEach { it.invoke(false) }
                initCallbacks.clear()
            }
            synchronized(pendingQueue) {
                pendingQueue.forEach { it.onError("TTS initialization failed with code: $status") }
                pendingQueue.clear()
            }
        }
    }

    private fun setupProgressListener(ttsInstance: TextToSpeech) {
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(tag, "TTS started speaking: $utteranceId")
                activeOnStart?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                Log.d(tag, "TTS finished speaking: $utteranceId")
                activeOnDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(tag, "TTS error on: $utteranceId")
                activeOnError?.invoke("TTS playback error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(tag, "TTS error $errorCode on: $utteranceId")
                activeOnError?.invoke("TTS error: $errorCode")
            }
        })
    }

    override fun speak(
        text: String,
        utteranceId: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onDone()
            return
        }

        val ttsInstance = tts
        if (ttsInstance == null || !isInitialized.get()) {
            Log.d(tag, "TTS not ready yet; queuing request: '$text'")
            synchronized(pendingQueue) {
                pendingQueue.add(PendingSpeech(text, utteranceId, onStart, onDone, onError))
            }
            initTts { ready ->
                if (!ready) {
                    onError("TTS could not be initialized")
                }
            }
            return
        }

        activeOnStart = onStart
        activeOnDone = onDone
        activeOnError = onError

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        Log.d(tag, "Speaking text: '$text' (id: $utteranceId)")
        val result = ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(tag, "tts.speak returned error code: $result")
            onError("TTS speak error code: $result")
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        } finally {
            tts = null
            isInitialized.set(false)
            isInitializing.set(false)
        }
    }

    override fun isSpeaking(): Boolean = tts?.isSpeaking == true
}
