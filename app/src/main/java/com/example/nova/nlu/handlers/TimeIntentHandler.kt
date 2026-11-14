package com.example.nova.nlu.handlers

import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentHandler
import com.example.nova.nlu.IntentResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeIntentHandler @Inject constructor() : IntentHandler {

    override fun canHandle(intent: AssistantIntent): Boolean = intent is AssistantIntent.GetTime

    override suspend fun handle(intent: AssistantIntent): IntentResult {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        val currentTime = sdf.format(Date())
        return IntentResult.success("It's $currentTime.")
    }
}
