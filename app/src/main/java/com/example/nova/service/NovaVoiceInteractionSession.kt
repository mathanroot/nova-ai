package com.example.nova.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import com.example.nova.MainActivity

class NovaVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val tag = "NovaVoiceSession"

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(tag, "NovaVoiceInteractionSession shown. Launching Nova assistant...")
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("EXTRA_VOICE_TRIGGER", true)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start MainActivity from voice session", e)
        }
        hide()
    }
}
