package com.example.nova.conversation

import java.util.UUID

data class ConversationSession(
    val sessionId: String = UUID.randomUUID().toString(),
    var turnIndex: Int = 0,
    var lastUserUtterance: String? = null,
    var lastAssistantResponse: String? = null,
    var pendingClarificationQuestion: String? = null,
    var isAwaitingFollowUp: Boolean = false,
    val startedAtMs: Long = System.currentTimeMillis()
) {
    fun nextTurn(userText: String) {
        turnIndex++
        lastUserUtterance = userText
    }

    fun completeTurn(response: String, requiresFollowUp: Boolean, clarificationPrompt: String? = null) {
        lastAssistantResponse = response
        isAwaitingFollowUp = requiresFollowUp
        pendingClarificationQuestion = clarificationPrompt
    }
}
