package com.example.nova.service

import android.content.Intent
import android.speech.RecognitionService
import android.util.Log

class NovaRecognitionService : RecognitionService() {

    private val tag = "NovaRecognitionService"

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.d(tag, "RecognitionService onStartListening")
    }

    override fun onCancel(listener: Callback?) {
        Log.d(tag, "RecognitionService onCancel")
    }

    override fun onStopListening(listener: Callback?) {
        Log.d(tag, "RecognitionService onStopListening")
    }
}
