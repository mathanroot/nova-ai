package com.example.nova.actions

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAction @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "ReminderAction"

    fun addReminder(text: String, timeHint: String? = null): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Reminder: $text")
                putExtra(CalendarContract.Events.DESCRIPTION, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to create reminder", e)
            false
        }
    }
}
