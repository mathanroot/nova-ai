package com.example.nova.assistant

import android.content.Context
import android.util.Log
import com.example.nova.conversation.ConversationManager
import com.example.nova.wakeword.WakeWordForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationManager: ConversationManager,
    private val stateMachine: AssistantStateMachine
) {
    private val tag = "AssistantCoordinator"
    private val scope = CoroutineScope(Dispatchers.Main)

    fun onWakeWordTriggered() {
        Log.d(tag, "AssistantCoordinator: wake word triggered")
        conversationManager.startConversationFromWakeWord()
    }

    fun startManualListening() {
        Log.d(tag, "AssistantCoordinator: manual listening requested")
        conversationManager.beginListeningTurn()
    }

    fun stopOrCancel() {
        Log.d(tag, "AssistantCoordinator: stop or cancel requested")
        conversationManager.interruptOrCancel()
    }

    fun startForegroundWakeService() {
        WakeWordForegroundService.startService(context)
    }

    fun stopForegroundWakeService() {
        WakeWordForegroundService.stopService(context)
    }
}
