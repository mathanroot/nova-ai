package com.example.nova.nlu.handlers

import com.example.nova.actions.TimerAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerIntentHandler @Inject constructor(
    private val timerAction: TimerAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean =
        intent is AssistantIntent.SetTimer || intent is AssistantIntent.StopTimer

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        return when (intent) {
            is AssistantIntent.SetTimer -> {
                val sec = intent.durationSeconds
                val success = timerAction.setTimer(sec, intent.label ?: "Nova Timer")
                val text = if (sec >= 60) {
                    val mins = sec / 60
                    "$mins ${if (mins == 1) "minute" else "minutes"}"
                } else {
                    "$sec seconds"
                }
                if (success) {
                    IntentResult.success("Timer set for $text.")
                } else {
                    IntentResult.failure("I couldn't start the timer.")
                }
            }
            is AssistantIntent.StopTimer -> {
                timerAction.dismissOrShowTimers()
                IntentResult.success("Opening your timers.")
            }
            else -> IntentResult.failure("Unsupported timer command.")
        }
    }
}
