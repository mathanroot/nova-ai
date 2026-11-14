package com.example.nova

import com.example.nova.assistant.AssistantState
import com.example.nova.assistant.AssistantStateMachine
import com.example.nova.conversation.SpeechTurnManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationManagerTest {

    private lateinit var stateMachine: AssistantStateMachine
    private lateinit var turnManager: SpeechTurnManager

    @Before
    fun setup() {
        stateMachine = AssistantStateMachine()
        turnManager = SpeechTurnManager()
    }

    @Test
    fun testStateMachineLifecycle() {
        assertEquals(AssistantState.Idle, stateMachine.currentState.value)

        // Wake detected
        stateMachine.transitionTo(AssistantState.WakeDetected)
        assertEquals(AssistantState.WakeDetected, stateMachine.currentState.value)

        // Listening
        stateMachine.transitionTo(AssistantState.Listening)
        assertEquals(AssistantState.Listening, stateMachine.currentState.value)

        // Speech paused
        stateMachine.transitionTo(AssistantState.SpeechPaused)
        assertEquals(AssistantState.SpeechPaused, stateMachine.currentState.value)

        // Resumed listening
        stateMachine.transitionTo(AssistantState.Listening)
        assertEquals(AssistantState.Listening, stateMachine.currentState.value)

        // Processing
        stateMachine.transitionTo(AssistantState.Processing)
        assertEquals(AssistantState.Processing, stateMachine.currentState.value)

        // Speaking
        stateMachine.transitionTo(AssistantState.Speaking("It's 9:30 PM"))
        assertTrue(stateMachine.currentState.value is AssistantState.Speaking)

        // Follow-up listening
        stateMachine.transitionTo(AssistantState.FollowUpListening)
        assertEquals(AssistantState.FollowUpListening, stateMachine.currentState.value)

        // Reset to Idle
        stateMachine.reset()
        assertEquals(AssistantState.Idle, stateMachine.currentState.value)
    }

    @Test
    fun testSpeechTurnManager() {
        val session = turnManager.startNewSession()
        assertTrue(turnManager.isSessionActive())
        assertEquals(0, session.turnIndex)

        turnManager.registerTurn("What time is it?")
        assertEquals(1, session.turnIndex)
        assertEquals("What time is it?", session.lastUserUtterance)

        turnManager.completeTurn("It's 9:30 PM.", requiresFollowUp = true)
        assertEquals("It's 9:30 PM.", session.lastAssistantResponse)

        turnManager.endSession()
        assertEquals(false, turnManager.isSessionActive())
    }
}
