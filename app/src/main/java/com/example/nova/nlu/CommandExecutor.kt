package com.example.nova.nlu

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandExecutor @Inject constructor(
    private val commandRegistry: CommandRegistry
) {
    private val tag = "CommandExecutor"

    suspend fun execute(intent: AssistantIntent): IntentResult {
        Log.d(tag, "Executing intent: $intent")
        val handler = commandRegistry.findHandler(intent)
        return try {
            handler.handle(intent)
        } catch (e: Exception) {
            Log.e(tag, "Exception during command execution for intent: $intent", e)
            IntentResult.failure("An error occurred while executing that command.")
        }
    }
}
