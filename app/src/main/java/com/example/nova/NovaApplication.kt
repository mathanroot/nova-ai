package com.example.nova

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaApplication : Application() {

    companion object {
        const val WAKE_WORD_CHANNEL_ID = "nova_wake_word_channel"
        const val ASSISTANT_CHANNEL_ID = "nova_assistant_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val wakeWordChannel = NotificationChannel(
                WAKE_WORD_CHANNEL_ID,
                "Nova Wake-Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground microphone monitoring for 'Hey Nova'"
                setShowBadge(false)
            }

            val assistantChannel = NotificationChannel(
                ASSISTANT_CHANNEL_ID,
                "Nova Assistant Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active voice assistant alerts and status"
            }

            notificationManager.createNotificationChannel(wakeWordChannel)
            notificationManager.createNotificationChannel(assistantChannel)
        }
    }
}
