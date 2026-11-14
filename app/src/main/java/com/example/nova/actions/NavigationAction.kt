package com.example.nova.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationAction @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "NavigationAction"

    fun navigateTo(destination: String): Boolean {
        return try {
            val encoded = URLEncoder.encode(destination, "UTF-8")
            val uri = Uri.parse("google.navigation:q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback to geo URI or web maps
                val geoUri = Uri.parse("geo:0,0?q=$encoded")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start navigation to $destination", e)
            false
        }
    }
}
