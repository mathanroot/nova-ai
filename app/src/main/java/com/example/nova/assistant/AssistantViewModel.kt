package com.example.nova.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nova.audio.AudioLevelMonitor
import com.example.nova.conversation.ConversationManager
import com.example.nova.nlu.CommandExecutor
import com.example.nova.nlu.IntentParser
import com.example.nova.stt.ModelManager
import com.example.nova.stt.ModelStatus
import com.example.nova.stt.SpeechSessionManager
import com.example.nova.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val stateMachine: AssistantStateMachine,
    private val assistantCoordinator: AssistantCoordinator,
    private val speechSessionManager: SpeechSessionManager,
    private val audioLevelMonitor: AudioLevelMonitor,
    private val modelManager: ModelManager,
    private val intentParser: IntentParser,
    private val commandExecutor: CommandExecutor,
    private val ttsManager: TtsManager,
    private val conversationManager: ConversationManager
) : ViewModel() {

    val assistantState: StateFlow<AssistantState> = stateMachine.currentState
    val liveTranscription: StateFlow<String> = speechSessionManager.liveTranscription
    val audioLevel: StateFlow<Float> = audioLevelMonitor.normalizedLevel
    val modelStatus: StateFlow<ModelStatus> = modelManager.modelStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelStatus.UNINITIALIZED)

    init {
        ttsManager.init()
        viewModelScope.launch {
            modelManager.getOrLoadModel()
        }
    }

    fun onOrbClicked() {
        when (assistantState.value) {
            is AssistantState.Idle -> {
                assistantCoordinator.startManualListening()
            }
            is AssistantState.Listening,
            is AssistantState.SpeechPaused,
            is AssistantState.FollowUpListening -> {
                assistantCoordinator.stopOrCancel()
            }
            is AssistantState.Speaking -> {
                ttsManager.stop()
                assistantCoordinator.stopOrCancel()
            }
            else -> {
                assistantCoordinator.stopOrCancel()
            }
        }
    }

    fun submitTextCommand(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            stateMachine.transitionTo(AssistantState.Processing)
            val intent = intentParser.parse(text)
            val result = commandExecutor.execute(intent)
            stateMachine.transitionTo(AssistantState.Speaking(result.spokenResponse))
            ttsManager.speak(
                text = result.spokenResponse,
                onDone = {
                    stateMachine.transitionTo(AssistantState.Idle)
                }
            )
        }
    }

    fun cancel() {
        assistantCoordinator.stopOrCancel()
    }
}
