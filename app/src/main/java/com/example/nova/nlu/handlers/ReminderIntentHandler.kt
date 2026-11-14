package com.example.nova.nlu.handlers

import com.example.nova.actions.ReminderAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderIntentHandler @Inject constructor(
    private val reminderAction: ReminderAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.SetReminder

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.SetReminder) return IntentResult.failure("Invalid reminder intent")

        val text = intent.reminderText
        val success = reminderAction.addReminder(text, intent.timeHint)
        return if (success) {
            IntentResult.success("Your reminder is set.")
        } else {
            IntentResult.failure("I couldn't save that reminder.")
        }
    }
}
