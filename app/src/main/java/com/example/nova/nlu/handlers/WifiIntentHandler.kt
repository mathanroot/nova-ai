package com.example.nova.nlu.handlers

import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.ToggleWifi

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        // Modern Android requires navigating user to Wi-Fi settings panel for toggling
        deviceControlAction.openWifiSettings()
        val action = if ((intent as? AssistantIntent.ToggleWifi)?.enable == true) "enable" else "manage"
        return IntentResult.success("Opening Wi-Fi settings to $action connection.")
    }
}
