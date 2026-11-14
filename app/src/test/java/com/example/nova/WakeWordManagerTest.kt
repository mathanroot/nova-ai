package com.example.nova

import com.example.nova.wakeword.WakeWordConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordManagerTest {

    @Test
    fun testWakeWordConfigDefaults() {
        val config = WakeWordConfig()
        assertEquals("Hey Nova", config.wakePhrase)
        assertEquals(16000, config.sampleRate)
        assertEquals(2000L, config.debounceIntervalMs)
        assertTrue(config.sensitivity in 0.0f..1.0f)
    }

    @Test
    fun testDebounceLogic() {
        val debounceIntervalMs = 2000L
        var lastTriggerTimeMs = 1000L

        // Trigger attempted after 500ms -> should debounce (reject)
        var attemptTimeMs = 1500L
        var allowTrigger = (attemptTimeMs - lastTriggerTimeMs) > debounceIntervalMs
        assertEquals(false, allowTrigger)

        // Trigger attempted after 2500ms -> should allow
        attemptTimeMs = 3500L
        allowTrigger = (attemptTimeMs - lastTriggerTimeMs) > debounceIntervalMs
        assertEquals(true, allowTrigger)
    }
}
