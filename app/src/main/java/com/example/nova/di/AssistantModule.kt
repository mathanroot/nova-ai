package com.example.nova.di

import android.content.Context
import com.example.nova.assistant.AssistantCoordinator
import com.example.nova.assistant.AssistantStateMachine
import com.example.nova.conversation.ConversationManager
import com.example.nova.conversation.FollowUpListener
import com.example.nova.conversation.SessionTimeoutManager
import com.example.nova.conversation.SpeechTurnManager
import com.example.nova.nlu.CommandExecutor
import com.example.nova.nlu.CommandRegistry
import com.example.nova.nlu.IntentParser
import com.example.nova.stt.ModelManager
import com.example.nova.stt.SpeechSessionManager
import com.example.nova.tts.TtsManager
import com.example.nova.wakeword.AndroidWakeWordEngine
import com.example.nova.wakeword.WakeWordManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AssistantModule {

    @Provides
    @Singleton
    fun provideAssistantStateMachine(): AssistantStateMachine {
        return AssistantStateMachine()
    }

    @Provides
    @Singleton
    fun provideAndroidWakeWordEngine(
        @ApplicationContext context: Context,
        modelManager: ModelManager
    ): AndroidWakeWordEngine {
        return AndroidWakeWordEngine(context, modelManager)
    }

    @Provides
    @Singleton
    fun provideSpeechTurnManager(): SpeechTurnManager {
        return SpeechTurnManager()
    }

    @Provides
    @Singleton
    fun provideSessionTimeoutManager(): SessionTimeoutManager {
        return SessionTimeoutManager()
    }

    @Provides
    @Singleton
    fun provideFollowUpListener(sessionTimeoutManager: SessionTimeoutManager): FollowUpListener {
        return FollowUpListener(sessionTimeoutManager)
    }

    @Provides
    @Singleton
    fun provideConversationManager(
        stateMachine: AssistantStateMachine,
        speechSessionManager: SpeechSessionManager,
        intentParser: IntentParser,
        commandExecutor: CommandExecutor,
        ttsManager: TtsManager,
        wakeWordManager: WakeWordManager,
        speechTurnManager: SpeechTurnManager,
        followUpListener: FollowUpListener,
        audioCueManager: com.example.nova.audio.AudioCueManager
    ): ConversationManager {
        return ConversationManager(
            stateMachine = stateMachine,
            speechSessionManager = speechSessionManager,
            intentParser = intentParser,
            commandExecutor = commandExecutor,
            ttsManager = ttsManager,
            wakeWordManager = wakeWordManager,
            speechTurnManager = speechTurnManager,
            followUpListener = followUpListener,
            audioCueManager = audioCueManager
        )
    }

    @Provides
    @Singleton
    fun provideAssistantCoordinator(
        @ApplicationContext context: Context,
        conversationManager: ConversationManager,
        stateMachine: AssistantStateMachine
    ): AssistantCoordinator {
        return AssistantCoordinator(context, conversationManager, stateMachine)
    }
}
