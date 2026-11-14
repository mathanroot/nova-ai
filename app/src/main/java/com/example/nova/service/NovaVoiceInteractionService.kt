package com.example.nova.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

class NovaVoiceInteractionService : VoiceInteractionService() {

    private val tag = "NovaVoiceService"

    override fun onReady() {
        super.onReady()
        Log.d(tag, "NovaVoiceInteractionService is ready")
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d(tag, "NovaVoiceInteractionService is shutting down")
    }
}
