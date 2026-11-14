package com.example.nova.nlu.handlers

import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.GetBattery

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        val level = deviceControlAction.getBatteryLevel()
        val isCharging = deviceControlAction.isBatteryCharging()
        val chargeText = if (isCharging) " and currently charging" else ""
        return IntentResult.success("Your battery is at $level percent$chargeText.")
    }
}
