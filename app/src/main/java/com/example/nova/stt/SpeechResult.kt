package com.example.nova.stt

sealed interface SpeechResult {
    data class Partial(val text: String) : SpeechResult
    data class Final(val text: String, val confidence: Float = 1.0f) : SpeechResult
    data class Error(val error: String, val throwable: Throwable? = null) : SpeechResult
    object Empty : SpeechResult
    object EndOfUtterance : SpeechResult
}
