package com.example.nova.nlu.handlers

import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowUpIntentHandler @Inject constructor() : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.FollowUp

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.FollowUp) return IntentResult.failure("Invalid follow up intent")

        val cmd = intent.command.lowercase()
        return when {
            cmd.contains("what else") || cmd.contains("can you do") -> {
                IntentResult.success(
                    "I can set alarms, start timers, make phone calls, send messages, launch apps, report battery and time, toggle the flashlight, control volume and media, and navigate offline.",
                    requiresFollowUp = true
                )
            }
            cmd.contains("tell me more") -> {
                IntentResult.success(
                    "All my voice and command processing runs completely offline on your device, with no cloud servers involved.",
                    requiresFollowUp = true
                )
            }
            else -> {
                IntentResult.success("I am ready for your next request.", requiresFollowUp = true)
            }
        }
    }
}
