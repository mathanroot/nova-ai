package com.example.nova.wakeword

import android.util.Log
import com.example.nova.audio.AudioRecorder
import com.example.nova.audio.MicrophoneManager
import com.example.nova.audio.MicrophoneOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeWordManager @Inject constructor(
    private val microphoneManager: MicrophoneManager,
    private val wakeWordEngine: AndroidWakeWordEngine
) {
    private val tag = "WakeWordManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isListeningForWakeWord = MutableStateFlow(false)
    val isListeningForWakeWord: StateFlow<Boolean> = _isListeningForWakeWord.asStateFlow()

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected.asSharedFlow()

    private var audioRecorder: AudioRecorder? = null
    private var lastTriggerTimeMs = 0L
    private val debounceIntervalMs = 2000L

    suspend fun startListening(): Boolean {
        if (_isListeningForWakeWord.value) {
            Log.d(tag, "Wake-word listening is already running")
            return true
        }

        val ownershipGranted = microphoneManager.requestOwnership(MicrophoneOwner.WAKE_WORD)
        if (!ownershipGranted) {
            Log.w(tag, "Failed to get microphone ownership for WakeWord")
            return false
        }

        wakeWordEngine.initEngine()

        val recorder = AudioRecorder()
        audioRecorder = recorder
        microphoneManager.attachRecorder(recorder)

        val success = recorder.start(scope) { buffer, readSize ->
            val detected = wakeWordEngine.processAudioFrame(buffer, readSize)
            if (detected) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTimeMs > debounceIntervalMs) {
                    lastTriggerTimeMs = now
                    Log.d(tag, "Wake phrase 'Hey Nova' triggered!")
                    scope.launch {
                        stopListening()
                        _wakeWordDetected.emit(Unit)
                    }
                }
            }
        }

        if (success) {
            _isListeningForWakeWord.value = true
            Log.d(tag, "Wake-word detection active")
            return true
        } else {
            Log.e(tag, "Failed to start recorder for wake word")
            microphoneManager.releaseOwnership(MicrophoneOwner.WAKE_WORD)
            return false
        }
    }

    suspend fun stopListening() {
        if (!_isListeningForWakeWord.value) return
        _isListeningForWakeWord.value = false

        audioRecorder?.stopAndRelease()
        audioRecorder = null
        wakeWordEngine.reset()
        microphoneManager.releaseOwnership(MicrophoneOwner.WAKE_WORD)
        Log.d(tag, "Wake-word listening stopped")
    }

    fun release() {
        scope.launch {
            stopListening()
        }
        wakeWordEngine.release()
    }
}
