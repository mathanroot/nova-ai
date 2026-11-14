package com.example.nova.audio

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

enum class MicrophoneOwner {
    NONE,
    WAKE_WORD,
    VOSK_RECOGNITION,
    RELEASED
}

@Singleton
class MicrophoneManager @Inject constructor() {

    private val tag = "MicrophoneManager"
    private val mutex = Mutex()

    private val _currentOwner = MutableStateFlow(MicrophoneOwner.NONE)
    val currentOwner: StateFlow<MicrophoneOwner> = _currentOwner.asStateFlow()

    private var activeRecorder: AudioRecorder? = null

    /**
     * Safely request ownership of the microphone.
     * If another component currently holds ownership, it will be instructed to release first.
     */
    suspend fun requestOwnership(
        requester: MicrophoneOwner,
        onReleasePrevious: (suspend (MicrophoneOwner) -> Unit)? = null
    ): Boolean = mutex.withLock {
        Log.d(tag, "requestOwnership requested by $requester. Current owner is ${_currentOwner.value}")
        if (_currentOwner.value == requester) {
            return true
        }

        val previousOwner = _currentOwner.value
        if (previousOwner != MicrophoneOwner.NONE && previousOwner != MicrophoneOwner.RELEASED) {
            Log.d(tag, "Releasing previous owner: $previousOwner before granting to $requester")
            try {
                onReleasePrevious?.invoke(previousOwner)
            } catch (e: Exception) {
                Log.e(tag, "Error releasing previous owner $previousOwner", e)
            }
            activeRecorder?.stopAndRelease()
            activeRecorder = null
            _currentOwner.value = MicrophoneOwner.RELEASED
        }

        _currentOwner.value = requester
        Log.d(tag, "Microphone ownership successfully transferred to $requester")
        return true
    }

    /**
     * Release ownership from a component.
     */
    suspend fun releaseOwnership(owner: MicrophoneOwner) = mutex.withLock {
        if (_currentOwner.value == owner) {
            Log.d(tag, "Releasing microphone ownership from $owner")
            activeRecorder?.stopAndRelease()
            activeRecorder = null
            _currentOwner.value = MicrophoneOwner.NONE
        }
    }

    /**
     * Register active recorder for safety release.
     */
    fun attachRecorder(recorder: AudioRecorder) {
        activeRecorder = recorder
    }

    fun detachRecorder(recorder: AudioRecorder) {
        if (activeRecorder == recorder) {
            activeRecorder = null
        }
    }

    fun isOwner(owner: MicrophoneOwner): Boolean {
        return _currentOwner.value == owner
    }
}
