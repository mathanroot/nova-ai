package com.example.nova.nlu.handlers

import com.example.nova.actions.AppLaunchAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAppIntentHandler @Inject constructor(
    private val appLaunchAction: AppLaunchAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.OpenApp

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.OpenApp) return IntentResult.failure("Invalid open app intent")

        val appName = intent.appName
        val launched = appLaunchAction.launchAppByName(appName)
        return if (launched) {
            IntentResult.success("Opening $appName.")
        } else {
            IntentResult.failure("I couldn't find an app named $appName installed on this device.")
        }
    }
}
