package com.example.nova.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Singleton
class AudioLevelMonitor @Inject constructor() {

    private val _normalizedLevel = MutableStateFlow(0.0f)
    val normalizedLevel: StateFlow<Float> = _normalizedLevel.asStateFlow()

    private val _isSpeakingDetected = MutableStateFlow(false)
    val isSpeakingDetected: StateFlow<Boolean> = _isSpeakingDetected.asStateFlow()

    // Thresholds in dBFS
    private val speechThresholdDb = -42.0f

    fun processAudioFrame(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return

        var sumSquare = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i]
            sumSquare += sample * sample
        }

        val rms = sqrt(sumSquare / readSize)
        // Convert RMS to dBFS (max 16-bit is 32767)
        val db = if (rms > 0) {
            20.0 * log10(rms / 32767.0)
        } else {
            -100.0
        }

        // Map roughly -60dB (silence) to 0dB (loud) into 0.0f .. 1.0f
        val clampedDb = max(-60.0, min(0.0, db))
        val normalized = ((clampedDb + 60.0) / 60.0).toFloat()

        // Exponential moving average for smooth visual orb reactivity
        val current = _normalizedLevel.value
        val smoothed = current * 0.4f + normalized * 0.6f

        _normalizedLevel.value = smoothed
        _isSpeakingDetected.value = db > speechThresholdDb
    }

    fun reset() {
        _normalizedLevel.value = 0.0f
        _isSpeakingDetected.value = false
    }
}
