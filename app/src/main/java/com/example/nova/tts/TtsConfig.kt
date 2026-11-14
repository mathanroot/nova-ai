package com.example.nova.tts

data class TtsConfig(
    val speechRate: Float = 1.05f,
    val pitch: Float = 1.0f,
    val queueMode: Int = android.speech.tts.TextToSpeech.QUEUE_FLUSH
)
