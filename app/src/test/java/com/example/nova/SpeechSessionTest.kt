package com.example.nova

import com.example.nova.stt.SpeechConfig
import com.example.nova.stt.UtteranceManager
import com.example.nova.stt.VoiceActivityDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeechSessionTest {

    private lateinit var utteranceManager: UtteranceManager
    private lateinit var config: SpeechConfig

    @Before
    fun setup() {
        config = SpeechConfig(
            shortPauseThresholdMs = 1000L,
            normalPauseThresholdMs = 2000L,
            finalSilenceTimeoutMs = 2800L,
            maxUtteranceDurationMs = 30000L
        )
        utteranceManager = UtteranceManager(config)
    }

    @Test
    fun testNaturalPauseAndContinuation() {
        // User says: "Hey Nova, set an alarm"
        utteranceManager.onPartialResult("set an alarm")
        utteranceManager.onChunkFinalResult("set an alarm")

        // User pauses for 2 seconds (2000 ms)
        val pauseDurationMs = 2000L
        val totalSpeechMs = 3500L

        // Should NOT finalize on a 2-second pause!
        val shouldFinalizeDuringPause = utteranceManager.shouldFinalize(pauseDurationMs, totalSpeechMs)
        assertFalse("Utterance should not finalize during a natural 2-second pause", shouldFinalizeDuringPause)

        // User resumes: "...for tomorrow at six in the morning"
        utteranceManager.onPartialResult("for tomorrow at six in the morning")
        utteranceManager.onChunkFinalResult("for tomorrow at six in the morning")

        // Final silence: 3 seconds (3000 ms) > 2800 ms final silence threshold
        val finalSilenceMs = 3000L
        val shouldFinalizeAfterSilence = utteranceManager.shouldFinalize(finalSilenceMs, 7500L)
        assertTrue("Utterance should finalize after final silence timeout", shouldFinalizeAfterSilence)

        val fullSentence = utteranceManager.finalizeUtterance()
        assertEquals("set an alarm for tomorrow at six in the morning", fullSentence)
    }

    @Test
    fun testVoiceActivityDetectorCalculation() {
        val vad = VoiceActivityDetector(speechEnergyThresholdDb = -40.0f)
        assertFalse(vad.isSpeechActive())

        // Loud frame (simulated speech PCM)
        val loudBuffer = ShortArray(512) { 15000 }
        vad.processFrame(loudBuffer, 512, 1000L)
        assertTrue(vad.isSpeechActive())

        // Silence frame (zero PCM)
        val silentBuffer = ShortArray(512) { 0 }
        vad.processFrame(silentBuffer, 512, 2500L)
        assertEquals(1500L, vad.getSilenceDurationMs(2500L))
    }
}
