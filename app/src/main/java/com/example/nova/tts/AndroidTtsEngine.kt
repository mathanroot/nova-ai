package com.example.nova.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine, TextToSpeech.OnInitListener {

    private val tag = "AndroidTtsEngine"
    private var tts: TextToSpeech? = null
    private var isInitialized = AtomicBoolean(false)
    private var initCallback: ((Boolean) -> Unit)? = null

    private var activeOnStart: (() -> Unit)? = null
    private var activeOnDone: (() -> Unit)? = null
    private var activeOnError: ((String) -> Unit)? = null

    override fun initTts(onReady: (Boolean) -> Unit) {
        if (isInitialized.get() && tts != null) {
            onReady(true)
            return
        }
        initCallback = onReady
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsInstance = tts ?: return
            val langResult = ttsInstance.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(tag, "English US is not supported or missing data in TTS engine")
            }

            // Prefer high-quality offline voices
            try {
                val voices = ttsInstance.voices
                val offlineVoice = voices?.firstOrNull { voice ->
                    voice.locale.language == "en" &&
                            !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS)
                }
                if (offlineVoice != null) {
                    ttsInstance.voice = offlineVoice
                    Log.d(tag, "Selected offline TTS voice: ${offlineVoice.name}")
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not set custom voice", e)
            }

            // Set AudioAttributes for Assistant
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            ttsInstance.setAudioAttributes(audioAttributes)

            ttsInstance.setSpeechRate(1.05f)
            ttsInstance.setPitch(1.0f)

            setupProgressListener(ttsInstance)
            isInitialized.set(true)
            Log.d(tag, "TTS initialized successfully")
            initCallback?.invoke(true)
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech (status: $status)")
            isInitialized.set(false)
            initCallback?.invoke(false)
        }
        initCallback = null
    }

    private fun setupProgressListener(ttsInstance: TextToSpeech) {
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(tag, "TTS started speaking utterance: $utteranceId")
                activeOnStart?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                Log.d(tag, "TTS finished speaking utterance: $utteranceId")
                activeOnDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(tag, "TTS error on utterance: $utteranceId")
                activeOnError?.invoke("TTS playback error")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(tag, "TTS error $errorCode on utterance: $utteranceId")
                activeOnError?.invoke("TTS error code: $errorCode")
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
        val ttsInstance = tts
        if (ttsInstance == null || !isInitialized.get()) {
            initTts { ready ->
                if (ready) {
                    speak(text, utteranceId, onStart, onDone, onError)
                } else {
                    onError("TTS is not initialized")
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

        val result = ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(tag, "ttsInstance.speak failed with code: $result")
            onError("TTS speak failed with code: $result")
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
        }
    }

    override fun isSpeaking(): Boolean = tts?.isSpeaking == true
}
