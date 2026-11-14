package com.example.nova.tts

interface TtsEngine {
    fun initTts(onReady: (Boolean) -> Unit)
    fun speak(text: String, utteranceId: String, onStart: () -> Unit, onDone: () -> Unit, onError: (String) -> Unit)
    fun stop()
    fun shutdown()
    fun isSpeaking(): Boolean
}
