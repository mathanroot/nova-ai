package com.example.nova.nlu.handlers

import com.example.nova.actions.SmsAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsIntentHandler @Inject constructor(
    private val smsAction: SmsAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.SendSms

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.SendSms) return IntentResult.failure("Invalid SMS intent")

        val query = intent.contactName.trim()
        val message = intent.message ?: ""
        val contacts = smsAction.findContactPhone(query)

        return when {
            contacts.isEmpty() -> {
                smsAction.openSmsApp(null, message)
                IntentResult.success("Opening messages for $query.")
            }
            contacts.size == 1 -> {
                val match = contacts[0]
                val success = smsAction.openSmsApp(match.phoneNumber, message)
                if (success) {
                    IntentResult.success("Preparing message to ${match.name}.")
                } else {
                    IntentResult.failure("Could not prepare message.")
                }
            }
            else -> {
                IntentResult.clarification(
                    prompt = "I found ${contacts.size} contacts for '$query'. Which one should I message?",
                    options = contacts.map { "${it.name} (${it.phoneNumber})" }
                )
            }
        }
    }
}
