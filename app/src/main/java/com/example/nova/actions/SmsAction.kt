package com.example.nova.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAction @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callAction: CallAction
) {
    private val tag = "SmsAction"

    fun sendSmsDirect(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: SecurityException) {
            Log.w(tag, "SEND_SMS permission not granted; falling back to SMS app intent")
            openSmsApp(phoneNumber, message)
        } catch (e: Exception) {
            Log.e(tag, "Error sending SMS", e)
            false
        }
    }

    fun openSmsApp(phoneNumber: String?, message: String?): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber ?: ""}")
                if (!message.isNullOrBlank()) {
                    putExtra("sms_body", message)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to open SMS app", e)
            false
        }
    }

    fun findContactPhone(nameQuery: String): List<ContactMatch> {
        return callAction.findContacts(nameQuery)
    }
}
