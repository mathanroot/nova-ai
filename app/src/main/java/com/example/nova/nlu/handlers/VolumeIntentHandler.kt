package com.example.nova.nlu.handlers

import android.media.AudioManager
import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.SetVolume

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.SetVolume) return IntentResult.failure("Invalid volume intent")

        return when (intent.direction) {
            AssistantIntent.VolumeDirection.UP -> {
                deviceControlAction.adjustVolume(AudioManager.ADJUST_RAISE)
                IntentResult.success("Volume increased.")
            }
            AssistantIntent.VolumeDirection.DOWN -> {
                deviceControlAction.adjustVolume(AudioManager.ADJUST_LOWER)
                IntentResult.success("Volume decreased.")
            }
            AssistantIntent.VolumeDirection.MUTE -> {
                deviceControlAction.adjustVolume(AudioManager.ADJUST_MUTE)
                IntentResult.success("Volume muted.")
            }
            AssistantIntent.VolumeDirection.SET -> {
                val percent = intent.percent ?: 50
                deviceControlAction.setVolumePercent(percent)
                IntentResult.success("Volume set to $percent percent.")
            }
        }
    }
}
