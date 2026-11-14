package com.example.nova.stt

import kotlin.math.log10
import kotlin.math.sqrt

enum class VadState {
    SILENCE,
    SPEECH_STARTED,
    SPEECH_ONGOING,
    SPEECH_PAUSED
}

class VoiceActivityDetector(
    private val speechEnergyThresholdDb: Float = -40.0f
) {
    private var currentState = VadState.SILENCE
    private var speechStartTimeMs = 0L
    private var lastSpeechDetectedTimeMs = 0L
    private var hasSpeechStarted = false

    fun processFrame(buffer: ShortArray, readSize: Int, currentTimeMs: Long = System.currentTimeMillis()): VadState {
        if (readSize <= 0) return currentState

        var sumSquare = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i]
            sumSquare += sample * sample
        }
        val rms = sqrt(sumSquare / readSize)
        val db = if (rms > 0) 20.0 * log10(rms / 32767.0) else -100.0
        val isVoiceActive = db > speechEnergyThresholdDb

        if (isVoiceActive) {
            lastSpeechDetectedTimeMs = currentTimeMs
            if (!hasSpeechStarted) {
                hasSpeechStarted = true
                speechStartTimeMs = currentTimeMs
                currentState = VadState.SPEECH_STARTED
            } else {
                currentState = VadState.SPEECH_ONGOING
            }
        } else {
            if (hasSpeechStarted) {
                currentState = VadState.SPEECH_PAUSED
            } else {
                currentState = VadState.SILENCE
            }
        }

        return currentState
    }

    fun getSilenceDurationMs(currentTimeMs: Long = System.currentTimeMillis()): Long {
        if (!hasSpeechStarted) return 0L
        return if (lastSpeechDetectedTimeMs > 0) currentTimeMs - lastSpeechDetectedTimeMs else 0L
    }

    fun getTotalSpeechDurationMs(currentTimeMs: Long = System.currentTimeMillis()): Long {
        if (!hasSpeechStarted) return 0L
        return currentTimeMs - speechStartTimeMs
    }

    fun isSpeechActive(): Boolean = hasSpeechStarted

    fun reset() {
        currentState = VadState.SILENCE
        speechStartTimeMs = 0L
        lastSpeechDetectedTimeMs = 0L
        hasSpeechStarted = false
    }
}
