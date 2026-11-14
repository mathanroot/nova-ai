package com.example.nova.nlu.handlers

import android.view.KeyEvent
import com.example.nova.actions.DeviceControlAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaIntentHandler @Inject constructor(
    private val deviceControlAction: DeviceControlAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.MediaControl

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        if (intent !is AssistantIntent.MediaControl) return IntentResult.failure("Invalid media intent")

        return when (intent.action) {
            AssistantIntent.MediaAction.PLAY -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
                IntentResult.success("Playing music.")
            }
            AssistantIntent.MediaAction.PAUSE -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
                IntentResult.success("Music paused.")
            }
            AssistantIntent.MediaAction.RESUME -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                IntentResult.success("Resuming playback.")
            }
            AssistantIntent.MediaAction.NEXT -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                IntentResult.success("Skipping to next track.")
            }
            AssistantIntent.MediaAction.PREVIOUS -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                IntentResult.success("Playing previous track.")
            }
            AssistantIntent.MediaAction.STOP -> {
                deviceControlAction.sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP)
                IntentResult.success("Stopped playback.")
            }
        }
    }
}
