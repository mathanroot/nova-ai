package com.example.nova.wakeword

data class WakeWordConfig(
    val wakePhrase: String = "Hey Nova",
    val sensitivity: Float = 0.65f,
    val sampleRate: Int = 16000,
    val frameDurationMs: Int = 30,
    val debounceIntervalMs: Long = 2000L
)
