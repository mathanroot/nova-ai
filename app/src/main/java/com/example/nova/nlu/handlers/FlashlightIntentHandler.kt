package com.example.nova.nlu.handlers

import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashlightIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.ToggleFlashlight

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.ToggleFlashlight) return IntentResult.failure("Invalid flashlight intent")
        val success = deviceControlAction.toggleFlashlight(intent.enable)
        return if (success) {
            val state = if (intent.enable) "on" else "off"
            IntentResult.success("Flashlight turned $state.")
        } else {
            IntentResult.failure("I couldn't control the flashlight.")
        }
    }
}
