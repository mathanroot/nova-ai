package com.example.nova.nlu

interface IntentHandler {
    fun canHandle(intent: AssistantIntent): Boolean
    suspend fun handle(intent: AssistantIntent): IntentResult
}
