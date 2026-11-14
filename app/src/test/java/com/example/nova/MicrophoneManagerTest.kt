package com.example.nova

import com.example.nova.audio.MicrophoneManager
import com.example.nova.audio.MicrophoneOwner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MicrophoneManagerTest {

    private lateinit var microphoneManager: MicrophoneManager

    @Before
    fun setup() {
        microphoneManager = MicrophoneManager()
    }

    @Test
    fun testOwnershipTransition() = runBlocking {
        assertEquals(MicrophoneOwner.NONE, microphoneManager.currentOwner.value)

        // 1. Wake word acquires ownership
        val wakeWordAcquired = microphoneManager.requestOwnership(MicrophoneOwner.WAKE_WORD)
        assertTrue(wakeWordAcquired)
        assertEquals(MicrophoneOwner.WAKE_WORD, microphoneManager.currentOwner.value)
        assertTrue(microphoneManager.isOwner(MicrophoneOwner.WAKE_WORD))

        // 2. Transfer ownership to Vosk Recognition
        var releasedPreviousOwner: MicrophoneOwner? = null
        val voskAcquired = microphoneManager.requestOwnership(MicrophoneOwner.VOSK_RECOGNITION) { previous ->
            releasedPreviousOwner = previous
        }
        assertTrue(voskAcquired)
        assertEquals(MicrophoneOwner.WAKE_WORD, releasedPreviousOwner)
        assertEquals(MicrophoneOwner.VOSK_RECOGNITION, microphoneManager.currentOwner.value)

        // 3. Release ownership
        microphoneManager.releaseOwnership(MicrophoneOwner.VOSK_RECOGNITION)
        assertEquals(MicrophoneOwner.NONE, microphoneManager.currentOwner.value)
    }
}
