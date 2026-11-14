package com.example.nova.nlu.handlers

import com.example.nova.actions.AlarmAction
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmIntentHandler @Inject constructor(
    private val alarmAction: AlarmAction
) : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean =
        intent is AssistantIntent.SetAlarm || intent is AssistantIntent.CancelAlarm

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        return when (intent) {
            is AssistantIntent.SetAlarm -> {
                var hour = intent.hour
                val minute = intent.minute
                if (intent.isPm == true && hour < 12) {
                    hour += 12
                } else if (intent.isPm == false && hour == 12) {
                    hour = 0
                }

                val success = alarmAction.setAlarm(hour, minute, intent.label ?: "Nova Alarm")
                val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
                if (success) {
                    IntentResult.success("Your alarm is set for $formattedTime.")
                } else {
                    IntentResult.failure("I couldn't set the alarm.")
                }
            }
            is AssistantIntent.CancelAlarm -> {
                alarmAction.dismissOrOpenAlarms()
                IntentResult.success("Opening your alarms.")
            }
            else -> IntentResult.failure("Unsupported alarm command.")
        }
    }
}
