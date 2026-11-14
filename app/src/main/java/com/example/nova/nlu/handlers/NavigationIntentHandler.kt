package com.example.nova.nlu.handlers

import com.example.nova.actions.NavigationAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationIntentHandler @Inject constructor(
    private val navigationAction: NavigationAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.Navigate

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.Navigate) return IntentResult.failure("Invalid navigation intent")

        val destination = intent.destination.trim()
        val success = navigationAction.navigateTo(destination)
        return if (success) {
            IntentResult.success("Navigating to $destination.")
        } else {
            IntentResult.failure("I couldn't start navigation to $destination.")
        }
    }
}
