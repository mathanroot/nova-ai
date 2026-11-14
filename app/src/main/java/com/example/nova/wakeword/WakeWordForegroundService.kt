package com.example.nova.wakeword

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nova.MainActivity
import com.example.nova.NovaApplication
import com.example.nova.R
import com.example.nova.assistant.AssistantCoordinator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordForegroundService : Service() {

    private val tag = "WakeWordService"
    private val scope = CoroutineScope(Dispatchers.Main)
    private var serviceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Inject
    lateinit var wakeWordManager: WakeWordManager

    @Inject
    lateinit var assistantCoordinator: AssistantCoordinator

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.nova.action.START_WAKE_SERVICE"
        const val ACTION_STOP = "com.example.nova.action.STOP_WAKE_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "WakeWordForegroundService onCreate")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(tag, "Received ACTION_STOP")
                stopForegroundService()
                return START_NOT_STICKY
            }
            else -> {
                Log.d(tag, "Starting foreground notification and wake-word listener")
                startAsForeground()
                startWakeWordDetection()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NovaApplication.WAKE_WORD_CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(getString(R.string.foreground_service_notification_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startWakeWordDetection() {
        serviceJob?.cancel()
        serviceJob = scope.launch {
            // Listen for wake word detections
            launch {
                wakeWordManager.wakeWordDetected.collect {
                    Log.d(tag, "Wake word detected in foreground service! Activating assistant session...")
                    assistantCoordinator.onWakeWordTriggered()
                }
            }
            wakeWordManager.startListening()
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Nova::WakeWordServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // 10 minutes timeout per lock acquisition, refreshed as needed
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun stopForegroundService() {
        serviceJob?.cancel()
        serviceJob = null
        scope.launch {
            wakeWordManager.stopListening()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "WakeWordForegroundService onDestroy")
        stopForegroundService()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
