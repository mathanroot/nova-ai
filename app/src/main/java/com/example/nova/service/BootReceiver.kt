package com.example.nova.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.nova.wakeword.WakeWordForegroundService

class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(tag, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val audioPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (audioPermission) {
                Log.d(tag, "Microphone permission confirmed. Auto-starting WakeWordForegroundService...")
                WakeWordForegroundService.startService(context)
            } else {
                Log.w(tag, "Cannot auto-start WakeWordForegroundService on boot: RECORD_AUDIO permission not granted")
            }
        }
    }
}
