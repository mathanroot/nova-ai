package com.example.nova.assistant

sealed interface AssistantState {
    object Idle : AssistantState
    object WakeDetected : AssistantState
    object Listening : AssistantState
    object SpeechPaused : AssistantState
    object Processing : AssistantState
    data class Speaking(val responseText: String) : AssistantState
    object FollowUpListening : AssistantState
    data class Error(val errorMessage: String) : AssistantState
}

enum class OrbVisualMode {
    IDLE,
    WAKE_DETECTED,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

fun AssistantState.toOrbMode(): OrbVisualMode = when (this) {
    is AssistantState.Idle -> OrbVisualMode.IDLE
    is AssistantState.WakeDetected -> OrbVisualMode.WAKE_DETECTED
    is AssistantState.Listening,
    is AssistantState.SpeechPaused,
    is AssistantState.FollowUpListening -> OrbVisualMode.LISTENING
    is AssistantState.Processing -> OrbVisualMode.PROCESSING
    is AssistantState.Speaking -> OrbVisualMode.SPEAKING
    is AssistantState.Error -> OrbVisualMode.ERROR
}
