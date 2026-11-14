package com.example.nova.nlu.handlers

import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CancelIntentHandler @Inject constructor() : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.Cancel

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        return IntentResult.success("Cancelled.")
    }
}
