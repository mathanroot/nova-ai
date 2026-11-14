package com.example.nova.nlu.handlers

import com.example.nova.actions.CallAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallIntentHandler @Inject constructor(
    private val callAction: CallAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.MakeCall

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.MakeCall) return IntentResult.failure("Invalid call intent")

        val query = intent.contactName.trim()
        val contacts = callAction.findContacts(query)

        return when {
            contacts.isEmpty() -> {
                // If it looks like a direct phone number
                if (query.replace(Regex("[^0-9+]"), "").length >= 3) {
                    callAction.makeCall(query)
                    IntentResult.success("Calling $query.")
                } else {
                    IntentResult.failure("I couldn't find a contact named $query.")
                }
            }
            contacts.size == 1 -> {
                val match = contacts[0]
                val success = callAction.makeCall(match.phoneNumber)
                if (success) {
                    IntentResult.success("Calling ${match.name}.")
                } else {
                    IntentResult.failure("I couldn't initiate the call to ${match.name}.")
                }
            }
            else -> {
                // Disambiguation
                val count = contacts.size
                val names = contacts.map { "${it.name} (${it.phoneNumber})" }
                IntentResult.clarification(
                    prompt = "I found $count contacts matching '$query'. Which one would you like to call?",
                    options = names
                )
            }
        }
    }
}
