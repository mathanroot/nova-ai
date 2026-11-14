package com.example.nova.nlu.handlers

import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.ToggleBluetooth

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        deviceControlAction.openBluetoothSettings()
        return IntentResult.success("Opening Bluetooth settings.")
    }
}
