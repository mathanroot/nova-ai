package com.example.nova.conversation

import android.util.Log
import com.example.nova.assistant.AssistantState
import com.example.nova.assistant.AssistantStateMachine
import com.example.nova.audio.AudioCueManager
import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.CommandExecutor
import com.example.nova.nlu.IntentParser
import com.example.nova.nlu.IntentResult
import com.example.nova.stt.SpeechSessionManager
import com.example.nova.tts.TtsManager
import com.example.nova.wakeword.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor(
    private val stateMachine: AssistantStateMachine,
    private val speechSessionManager: SpeechSessionManager,
    private val intentParser: IntentParser,
    private val commandExecutor: CommandExecutor,
    private val ttsManager: TtsManager,
    private val wakeWordManager: WakeWordManager,
    private val speechTurnManager: SpeechTurnManager,
    private val followUpListener: FollowUpListener,
    private val audioCueManager: AudioCueManager
) {
    private val tag = "ConversationManager"
    private val scope = CoroutineScope(Dispatchers.Main)
    private var utteranceCollectionJob: Job? = null

    private val _config = MutableStateFlow(ConversationConfig())
    val config: StateFlow<ConversationConfig> = _config.asStateFlow()

    fun updateConfig(newConfig: ConversationConfig) {
        _config.value = newConfig
    }

    /**
     * Called when "Hey Nova" is detected (either screen on or screen off)
     */
    fun startConversationFromWakeWord() {
        val currentState = stateMachine.currentState.value
        if (currentState !is AssistantState.Idle) {
            Log.d(tag, "startConversationFromWakeWord ignored: assistant is not idle ($currentState)")
            return
        }
        Log.d(tag, "startConversationFromWakeWord invoked")
        audioCueManager.playActivationChime()
        scope.launch {
            stateMachine.transitionTo(AssistantState.WakeDetected)
            speechTurnManager.startNewSession()

            if (_config.value.activationResponseEnabled) {
                // Short "Yes?" activation response
                stateMachine.transitionTo(AssistantState.Speaking(_config.value.activationPhrase))
                ttsManager.speak(
                    text = _config.value.activationPhrase,
                    onDone = {
                        beginListeningTurn()
                    },
                    onError = {
                        beginListeningTurn()
                    }
                )
            } else {
                beginListeningTurn()
            }
        }
    }

    /**
     * Start listening for user speech
     */
    fun beginListeningTurn() {
        scope.launch {
            stateMachine.transitionTo(AssistantState.Listening)
            val started = speechSessionManager.startListeningSession()
            if (!started) {
                Log.e(tag, "Failed to start speech session. Informing user.")
                stateMachine.transitionTo(AssistantState.Error("Microphone unavailable"))
                ttsManager.speak("Microphone is unavailable. Please grant microphone permission.")
                delay(2000)
                endConversation()
                return@launch
            }

            // Monitor speech pauses & utterance completion
            utteranceCollectionJob?.cancel()
            utteranceCollectionJob = launch {
                launch {
                    speechSessionManager.isUserPaused.collect { isPaused ->
                        if (isPaused && stateMachine.currentState.value is AssistantState.Listening) {
                            stateMachine.transitionTo(AssistantState.SpeechPaused)
                        } else if (!isPaused && stateMachine.currentState.value is AssistantState.SpeechPaused) {
                            stateMachine.transitionTo(AssistantState.Listening)
                        }
                    }
                }

                speechSessionManager.utteranceCompleted.collect { completedUtterance ->
                    Log.d(tag, "Utterance completed: '$completedUtterance'")
                    onUtteranceReceived(completedUtterance)
                }
            }
        }
    }

    /**
     * Process received user utterance
     */
    private fun onUtteranceReceived(utteranceText: String) {
        scope.launch {
            utteranceCollectionJob?.cancel()
            utteranceCollectionJob = null

            if (utteranceText.isBlank()) {
                Log.d(tag, "Received blank utterance; ending conversation.")
                endConversation()
                return@launch
            }

            speechTurnManager.registerTurn(utteranceText)
            audioCueManager.playConfirmationChime()
            stateMachine.transitionTo(AssistantState.Processing)

            // Deterministic offline intent parsing
            val intent = intentParser.parse(utteranceText)

            if (intent is AssistantIntent.Cancel) {
                ttsManager.speak("Cancelled.", onDone = { endConversation() })
                return@launch
            }

            // Command execution
            val result = commandExecutor.execute(intent)
            speechTurnManager.completeTurn(
                response = result.spokenResponse,
                requiresFollowUp = result.requiresFollowUp,
                clarificationPrompt = result.followUpPrompt
            )

            // Voice response
            stateMachine.transitionTo(AssistantState.Speaking(result.spokenResponse))
            ttsManager.speak(
                text = result.spokenResponse,
                onDone = {
                    handlePostSpeech(result)
                },
                onError = {
                    handlePostSpeech(result)
                }
            )
        }
    }

    /**
     * After Nova speaks, decide whether to open the follow-up listening window (Scenario D)
     */
    private fun handlePostSpeech(lastResult: IntentResult) {
        scope.launch {
            val shouldFollowUp = _config.value.isContinuousConversationEnabled &&
                    (lastResult.requiresFollowUp || _config.value.isContinuousConversationEnabled)

            if (shouldFollowUp) {
                stateMachine.transitionTo(AssistantState.FollowUpListening)
                delay(300) // Brief handover pause before re-opening microphone
                beginListeningTurn()

                followUpListener.startFollowUpWindow(_config.value.followUpTimeoutSeconds) {
                    Log.d(tag, "Follow-up window expired without user speech; closing conversation.")
                    endConversation()
                }
            } else {
                endConversation()
            }
        }
    }

    /**
     * Complete and end the conversation session, returning to idle wake-word detection
     */
    fun endConversation() {
        scope.launch {
            Log.d(tag, "Ending conversation session and returning to Idle")
            utteranceCollectionJob?.cancel()
            utteranceCollectionJob = null
            followUpListener.cancelFollowUp()
            speechSessionManager.cancelSession()
            ttsManager.stop()
            speechTurnManager.endSession()
            stateMachine.transitionTo(AssistantState.Idle)

            // Resume background wake-word listening
            wakeWordManager.startListening()
        }
    }

    fun interruptOrCancel() {
        scope.launch {
            ttsManager.stop()
            endConversation()
        }
    }
}
