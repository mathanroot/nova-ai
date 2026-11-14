package com.example.nova.nlu.handlers

import com.example.nova.actions.AppLaunchAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsIntentHandler @Inject constructor(
    private val appLaunchAction: AppLaunchAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.OpenSettings

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        appLaunchAction.launchAppByName("settings")
        return IntentResult.success("Opening system settings.")
    }
}
