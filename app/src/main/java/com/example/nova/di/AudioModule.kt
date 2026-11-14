package com.example.nova.di

import android.content.Context
import com.example.nova.audio.AudioFocusManager
import com.example.nova.audio.AudioLevelMonitor
import com.example.nova.audio.MicrophoneManager
import com.example.nova.stt.ModelManager
import com.example.nova.stt.SpeechSessionManager
import com.example.nova.stt.VoskSpeechRecognizer
import com.example.nova.tts.AndroidTtsEngine
import com.example.nova.tts.TtsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideMicrophoneManager(): MicrophoneManager {
        return MicrophoneManager()
    }

    @Provides
    @Singleton
    fun provideAudioLevelMonitor(): AudioLevelMonitor {
        return AudioLevelMonitor()
    }

    @Provides
    @Singleton
    fun provideAudioFocusManager(@ApplicationContext context: Context): AudioFocusManager {
        return AudioFocusManager(context)
    }

    @Provides
    @Singleton
    fun provideModelManager(@ApplicationContext context: Context): ModelManager {
        return ModelManager(context)
    }

    @Provides
    @Singleton
    fun provideVoskSpeechRecognizer(modelManager: ModelManager): VoskSpeechRecognizer {
        return VoskSpeechRecognizer(modelManager)
    }

    @Provides
    @Singleton
    fun provideAndroidSpeechRecognizer(@ApplicationContext context: Context): com.example.nova.stt.AndroidSpeechRecognizer {
        return com.example.nova.stt.AndroidSpeechRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideSpeechSessionManager(
        microphoneManager: MicrophoneManager,
        voskSpeechRecognizer: VoskSpeechRecognizer,
        androidSpeechRecognizer: com.example.nova.stt.AndroidSpeechRecognizer,
        audioLevelMonitor: AudioLevelMonitor,
        modelManager: ModelManager
    ): SpeechSessionManager {
        return SpeechSessionManager(
            microphoneManager,
            voskSpeechRecognizer,
            androidSpeechRecognizer,
            audioLevelMonitor,
            modelManager
        )
    }

    @Provides
    @Singleton
    fun provideAndroidTtsEngine(@ApplicationContext context: Context): AndroidTtsEngine {
        return AndroidTtsEngine(context)
    }

    @Provides
    @Singleton
    fun provideTtsManager(
        ttsEngine: AndroidTtsEngine,
        audioFocusManager: AudioFocusManager
    ): TtsManager {
        return TtsManager(ttsEngine, audioFocusManager)
    }
}
