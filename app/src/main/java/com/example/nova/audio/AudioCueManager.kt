package com.example.nova.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class AudioCueManager @Inject constructor(
    private val hapticManager: HapticManager
) {
    private val tag = "AudioCueManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays the Siri-style activation chime (Two-tone rising melodic chime: C5 -> E5)
     */
    fun playActivationChime() {
        hapticManager.playClick()
        scope.launch {
            try {
                // Two tones: 523.25 Hz (C5) for 90ms, then 659.25 Hz (E5) for 150ms
                val sampleRate = 44100
                val tone1Samples = generateTone(523.25, 0.08, sampleRate, 0.65f)
                val tone2Samples = generateTone(659.25, 0.16, sampleRate, 0.85f)
                val combined = ShortArray(tone1Samples.size + tone2Samples.size)
                System.arraycopy(tone1Samples, 0, combined, 0, tone1Samples.size)
                System.arraycopy(tone2Samples, 0, combined, tone1Samples.size, tone2Samples.size)

                playPcm(combined, sampleRate)
            } catch (e: Exception) {
                Log.e(tag, "Error playing activation chime", e)
            }
        }
    }

    /**
     * Plays confirmation chime when user finishes speaking
     */
    fun playConfirmationChime() {
        hapticManager.playDoublePulse()
        scope.launch {
            try {
                val sampleRate = 44100
                val tone = generateTone(783.99, 0.14, sampleRate, 0.70f) // G5 pleasant chime
                playPcm(tone, sampleRate)
            } catch (e: Exception) {
                Log.e(tag, "Error playing confirmation chime", e)
            }
        }
    }

    private fun generateTone(freqHz: Double, durationSec: Double, sampleRate: Int, volume: Float): ShortArray {
        val totalSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Sine wave with gentle decay envelope
            val envelope = exp(-4.0 * t / durationSec)
            val wave = sin(2.0 * PI * freqHz * t) * envelope
            buffer[i] = (wave * 32767.0 * volume).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun playPcm(pcm: ShortArray, sampleRate: Int) {
        val bufferSize = pcm.size * 2
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
    }
}
