package com.example.nova.conversation

data class ConversationConfig(
    val followUpTimeoutSeconds: Int = 10,
    val isContinuousConversationEnabled: Boolean = true,
    val activationResponseEnabled: Boolean = true,
    val activationPhrase: String = "Yes?"
)
