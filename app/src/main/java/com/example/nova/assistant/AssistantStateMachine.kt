package com.example.nova.assistant

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantStateMachine @Inject constructor() {

    private val tag = "AssistantStateMachine"
    private val _currentState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val currentState: StateFlow<AssistantState> = _currentState.asStateFlow()

    fun transitionTo(newState: AssistantState): Boolean {
        val previousState = _currentState.value
        if (previousState == newState) {
            return false
        }

        // Validate state transitions
        val isValid = when (newState) {
            is AssistantState.Idle -> true // Can always transition to Idle
            is AssistantState.Error -> true // Can always report an error
            is AssistantState.WakeDetected -> previousState is AssistantState.Idle || previousState is AssistantState.FollowUpListening
            is AssistantState.Listening -> previousState is AssistantState.WakeDetected ||
                    previousState is AssistantState.SpeechPaused ||
                    previousState is AssistantState.FollowUpListening ||
                    previousState is AssistantState.Idle
            is AssistantState.SpeechPaused -> previousState is AssistantState.Listening
            is AssistantState.Processing -> previousState is AssistantState.Listening || previousState is AssistantState.SpeechPaused
            is AssistantState.Speaking -> previousState is AssistantState.Processing || previousState is AssistantState.WakeDetected
            is AssistantState.FollowUpListening -> previousState is AssistantState.Speaking
        }

        if (isValid) {
            Log.d(tag, "Transition: $previousState -> $newState")
            _currentState.value = newState
            return true
        } else {
            Log.w(tag, "Invalid transition attempted: $previousState -> $newState (forcing transition)")
            _currentState.value = newState
            return true
        }
    }

    fun reset() {
        _currentState.value = AssistantState.Idle
    }
}
