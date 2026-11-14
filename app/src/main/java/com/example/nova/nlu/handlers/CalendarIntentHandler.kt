package com.example.nova.nlu.handlers

import com.example.nova.actions.CalendarAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarIntentHandler @Inject constructor(
    private val calendarAction: CalendarAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.AddCalendarEvent

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.AddCalendarEvent) return IntentResult.failure("Invalid calendar intent")

        val title = intent.title
        val success = calendarAction.addEvent(title, intent.dateOrTime ?: "")
        return if (success) {
            IntentResult.success("Added $title to your calendar.")
        } else {
            IntentResult.failure("I couldn't create the calendar event.")
        }
    }
}
