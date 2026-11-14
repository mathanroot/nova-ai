package com.example.nova.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLaunchAction @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "AppLaunchAction"

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "spotify" to "com.spotify.music",
        "camera" to "camera_intent",
        "calculator" to "com.google.android.calculator",
        "settings" to "android.settings.SETTINGS",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "photos" to "com.google.android.apps.photos",
        "clock" to "com.google.android.deskclock"
    )

    fun launchAppByName(appName: String): Boolean {
        val lower = appName.trim().lowercase()

        if (lower == "camera") {
            return launchCamera()
        }
        if (lower == "settings") {
            return launchSettings()
        }

        // 1. Direct package lookup
        val directPackage = commonPackageMap[lower]
        if (directPackage != null && directPackage != "camera_intent" && !directPackage.startsWith("android.settings")) {
            val launched = launchByPackage(directPackage)
            if (launched) return true
        }

        // 2. Query PackageManager for installed applications matching label
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == lower || label.contains(lower)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }
        return false
    }

    private fun launchByPackage(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch $packageName", e)
            false
        }
    }

    private fun launchCamera(): Boolean {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchSettings(): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
