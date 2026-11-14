package com.example.nova.nlu.handlers

import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FallbackIntentHandler @Inject constructor() : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.Fallback

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        val raw = (intent as? AssistantIntent.Fallback)?.rawText?.lowercase() ?: ""

        return when {
            raw.contains("weather") -> {
                IntentResult.success(
                    "I can't retrieve live weather offline, but I can help with other device commands.",
                    requiresFollowUp = true
                )
            }
            raw.contains("search") || raw.contains("google") -> {
                IntentResult.success(
                    "Web search requires internet connectivity. Nova operates strictly offline for your privacy.",
                    requiresFollowUp = false
                )
            }
            raw.isBlank() -> {
                IntentResult.failure("I didn't catch that. Say Hey Nova to try again.")
            }
            else -> {
                IntentResult.failure("I didn't understand '$raw'. Try asking for the time, setting an alarm, or opening an app.")
            }
        }
    }
}
