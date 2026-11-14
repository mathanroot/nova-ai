package com.example.nova.stt

import kotlinx.coroutines.flow.SharedFlow

interface SpeechRecognizer {
    val speechResults: SharedFlow<SpeechResult>
    suspend fun startListening(): Boolean
    suspend fun stopListening()
    fun cancel()
    fun isListening(): Boolean
    fun feedAudio(buffer: ShortArray, readSize: Int)
}
