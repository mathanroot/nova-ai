package com.example.nova.conversation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechTurnManager @Inject constructor() {
    private var currentSession: ConversationSession? = null

    fun startNewSession(): ConversationSession {
        val session = ConversationSession()
        currentSession = session
        return session
    }

    fun getCurrentSession(): ConversationSession? = currentSession

    fun registerTurn(userUtterance: String) {
        currentSession?.nextTurn(userUtterance)
    }

    fun completeTurn(response: String, requiresFollowUp: Boolean, clarificationPrompt: String? = null) {
        currentSession?.completeTurn(response, requiresFollowUp, clarificationPrompt)
    }

    fun endSession() {
        currentSession = null
    }

    fun isSessionActive(): Boolean = currentSession != null
}
