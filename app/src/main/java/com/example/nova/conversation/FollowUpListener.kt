package com.example.nova.conversation

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowUpListener @Inject constructor(
    private val sessionTimeoutManager: SessionTimeoutManager
) {
    private val tag = "FollowUpListener"

    fun startFollowUpWindow(timeoutSeconds: Int, onTimeout: () -> Unit) {
        Log.d(tag, "Follow-up listening window open for $timeoutSeconds seconds")
        sessionTimeoutManager.startTimeout(timeoutSeconds) {
            Log.d(tag, "Follow-up listening window expired")
            onTimeout()
        }
    }

    fun onUserSpeechDetected() {
        Log.d(tag, "User speech detected during follow-up; cancelling timeout")
        sessionTimeoutManager.cancelTimeout()
    }

    fun cancelFollowUp() {
        sessionTimeoutManager.cancelTimeout()
    }
}
