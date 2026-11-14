package com.example.nova.stt

data class SpeechConfig(
    val sampleRate: Float = 16000.0f,
    val shortPauseThresholdMs: Long = 1000L,       // 1.0s: continue listening
    val normalPauseThresholdMs: Long = 2000L,      // 2.0s: wait for more speech
    val finalSilenceTimeoutMs: Long = 2800L,       // 2.8s: finalize utterance
    val maxUtteranceDurationMs: Long = 30000L,     // 30s: maximum sentence duration
    val speechEnergyThresholdDb: Float = -40.0f    // Voice energy threshold
)
