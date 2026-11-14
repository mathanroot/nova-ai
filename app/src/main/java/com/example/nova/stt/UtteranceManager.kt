package com.example.nova.stt

import android.util.Log

class UtteranceManager(
    private val config: SpeechConfig = SpeechConfig()
) {
    private val tag = "UtteranceManager"
    private val accumulatedTextBuilder = StringBuilder()
    private var latestPartialText: String = ""
    private var isFinalized = false

    fun onPartialResult(partialText: String): String {
        if (partialText.isNotBlank()) {
            latestPartialText = partialText.trim()
        }
        val fullCurrent = if (accumulatedTextBuilder.isNotEmpty()) {
            "${accumulatedTextBuilder.toString().trim()} $latestPartialText".trim()
        } else {
            latestPartialText
        }
        return fullCurrent
    }

    fun onChunkFinalResult(chunkText: String) {
        val clean = chunkText.trim()
        if (clean.isNotBlank()) {
            if (accumulatedTextBuilder.isNotEmpty()) {
                accumulatedTextBuilder.append(" ")
            }
            accumulatedTextBuilder.append(clean)
            latestPartialText = ""
            Log.d(tag, "Appended chunk to utterance: '$clean'. Full text so far: '$accumulatedTextBuilder'")
        }
    }

    fun shouldFinalize(silenceDurationMs: Long, totalSpeechDurationMs: Long): Boolean {
        if (isFinalized) return false

        val fullText = getFullTranscript()
        if (fullText.isBlank()) {
            return false
        }

        // Check if silence exceeded the final silence timeout
        if (silenceDurationMs >= config.finalSilenceTimeoutMs) {
            Log.d(tag, "Silence ($silenceDurationMs ms) >= threshold (${config.finalSilenceTimeoutMs} ms). Finalizing utterance.")
            return true
        }

        // Check if maximum utterance duration was reached
        if (totalSpeechDurationMs >= config.maxUtteranceDurationMs) {
            Log.d(tag, "Max speech duration ($totalSpeechDurationMs ms) reached. Finalizing.")
            return true
        }

        return false
    }

    fun finalizeUtterance(): String {
        isFinalized = true
        // If there was leftover partial text that was not committed to accumulatedTextBuilder
        if (latestPartialText.isNotBlank()) {
            if (accumulatedTextBuilder.isNotEmpty()) {
                accumulatedTextBuilder.append(" ")
            }
            accumulatedTextBuilder.append(latestPartialText)
            latestPartialText = ""
        }
        val finalResult = accumulatedTextBuilder.toString().trim()
        Log.d(tag, "Finalized utterance: '$finalResult'")
        return finalResult
    }

    fun getFullTranscript(): String {
        return if (latestPartialText.isNotBlank()) {
            if (accumulatedTextBuilder.isNotEmpty()) {
                "${accumulatedTextBuilder.toString().trim()} $latestPartialText".trim()
            } else {
                latestPartialText
            }
        } else {
            accumulatedTextBuilder.toString().trim()
        }
    }

    fun isFinal(): Boolean = isFinalized

    fun reset() {
        accumulatedTextBuilder.setLength(0)
        latestPartialText = ""
        isFinalized = false
    }
}
