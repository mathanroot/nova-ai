package com.example.nova.nlu

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentParser @Inject constructor() {

    fun parse(rawText: String): AssistantIntent {
        var clean = rawText.lowercase(Locale.US).trim()

        // Strip leading wake phrase if present
        clean = clean.removePrefix("hey nova").removePrefix("nova").trim()
        clean = clean.removePrefix(",").trim()

        if (clean.isBlank()) {
            return AssistantIntent.Fallback("")
        }

        // 1. Cancel / Stop
        if (clean == "cancel" || clean == "stop" || clean == "stop listening" || clean == "be quiet" || clean == "dismiss") {
            return AssistantIntent.Cancel
        }

        // 2. Follow-up / Help
        if (clean.contains("what else can you do") || clean.contains("what can you do") ||
            clean.contains("tell me more") || clean == "repeat that" || clean == "do that again"
        ) {
            return AssistantIntent.FollowUp(clean)
        }

        // 3. Time
        if (clean.contains("what time is it") || clean.contains("current time") || clean == "time" || clean.contains("what is the time") || clean.contains("tell me the time")) {
            return AssistantIntent.GetTime
        }

        // 4. Date
        if (clean.contains("what's today's date") || clean.contains("what is today's date") ||
            clean.contains("what day is it") || clean.contains("today's date") || clean == "date"
        ) {
            return AssistantIntent.GetDate
        }

        // 5. Battery
        if (clean.contains("battery") || clean.contains("is my battery low")) {
            return AssistantIntent.GetBattery
        }

        // 6. Flashlight
        if (clean.contains("flashlight") || clean.contains("torch")) {
            val enable = !clean.contains("off")
            return AssistantIntent.ToggleFlashlight(enable)
        }

        // 7. Wi-Fi
        if (clean.contains("wifi") || clean.contains("wi-fi")) {
            val enable = !clean.contains("off")
            return AssistantIntent.ToggleWifi(enable)
        }

        // 8. Bluetooth
        if (clean.contains("bluetooth")) {
            val enable = !clean.contains("off")
            return AssistantIntent.ToggleBluetooth(enable)
        }

        // 9. Volume
        if (clean.contains("volume") || clean.contains("mute")) {
            if (clean.contains("up") || clean.contains("increase") || clean.contains("raise")) {
                return AssistantIntent.SetVolume(AssistantIntent.VolumeDirection.UP)
            }
            if (clean.contains("down") || clean.contains("decrease") || clean.contains("lower")) {
                return AssistantIntent.SetVolume(AssistantIntent.VolumeDirection.DOWN)
            }
            if (clean.contains("mute") || clean.contains("silence")) {
                return AssistantIntent.SetVolume(AssistantIntent.VolumeDirection.MUTE)
            }
            val percentMatch = Regex("(\\d+)\\s*(?:percent|%)?").find(clean)
            if (percentMatch != null) {
                val percent = percentMatch.groupValues[1].toIntOrNull() ?: 50
                return AssistantIntent.SetVolume(AssistantIntent.VolumeDirection.SET, percent)
            }
        }

        // 10. Media controls
        if (clean.contains("play music") || clean == "play") {
            return AssistantIntent.MediaControl(AssistantIntent.MediaAction.PLAY)
        }
        if (clean.contains("pause music") || clean == "pause") {
            return AssistantIntent.MediaControl(AssistantIntent.MediaAction.PAUSE)
        }
        if (clean.contains("resume music") || clean == "resume") {
            return AssistantIntent.MediaControl(AssistantIntent.MediaAction.RESUME)
        }
        if (clean.contains("next song") || clean.contains("next track") || clean == "skip") {
            return AssistantIntent.MediaControl(AssistantIntent.MediaAction.NEXT)
        }
        if (clean.contains("previous song") || clean.contains("previous track")) {
            return AssistantIntent.MediaControl(AssistantIntent.MediaAction.PREVIOUS)
        }

        // 11. Alarm
        if (clean.contains("alarm")) {
            if (clean.contains("cancel") || clean.contains("stop") || clean.contains("dismiss")) {
                return AssistantIntent.CancelAlarm
            }
            val alarmParsed = parseAlarmTime(clean)
            if (alarmParsed != null) {
                return alarmParsed
            }
            return AssistantIntent.SetAlarm(7, 0, false, "Morning Alarm")
        }
        if (clean.startsWith("wake me")) {
            val alarmParsed = parseAlarmTime(clean)
            if (alarmParsed != null) {
                return alarmParsed
            }
            return AssistantIntent.SetAlarm(7, 0, false, "Wake up")
        }

        // 12. Timer
        if (clean.contains("timer")) {
            if (clean.contains("stop") || clean.contains("cancel")) {
                return AssistantIntent.StopTimer
            }
            val seconds = parseTimerDuration(clean)
            return AssistantIntent.SetTimer(seconds)
        }

        // 13. Call
        if (clean.startsWith("call ") || clean.startsWith("phone ") || clean.startsWith("dial ")) {
            val contact = clean.removePrefix("call ").removePrefix("phone ").removePrefix("dial ").trim()
            return AssistantIntent.MakeCall(contact)
        }

        // 14. SMS
        if (clean.startsWith("send a message") || clean.startsWith("send message") || clean.startsWith("text ")) {
            return parseSms(clean)
        }

        // 15. Open App
        if (clean.startsWith("open ") || clean.startsWith("launch ")) {
            val appName = clean.removePrefix("open ").removePrefix("launch ").trim()
            return AssistantIntent.OpenApp(appName)
        }

        // 16. Navigation
        if (clean.startsWith("navigate to ") || clean.startsWith("take me to ") || clean.startsWith("directions to ")) {
            val dest = clean.removePrefix("navigate to ")
                .removePrefix("take me to ")
                .removePrefix("directions to ")
                .trim()
            return AssistantIntent.Navigate(dest)
        }

        // 17. Reminder
        if (clean.startsWith("remind me ") || clean.startsWith("set a reminder")) {
            val text = clean.removePrefix("remind me ").removePrefix("set a reminder").removePrefix("to ").trim()
            return AssistantIntent.SetReminder(text)
        }

        // 18. Calendar
        if (clean.contains("calendar") || clean.contains("meeting") || clean.contains("event")) {
            val title = clean.removePrefix("add a ").removePrefix("create a ").removePrefix("schedule a ").trim()
            return AssistantIntent.AddCalendarEvent(title)
        }

        // 19. Settings
        if (clean == "settings" || clean.contains("open settings")) {
            return AssistantIntent.OpenSettings("all")
        }

        return AssistantIntent.Fallback(rawText)
    }

    private fun parseAlarmTime(text: String): AssistantIntent.SetAlarm? {
        val isPm = when {
            text.contains("pm") || text.contains("evening") || text.contains("night") -> true
            text.contains("am") || text.contains("morning") -> false
            else -> null
        }

        // Look for pattern "H:M" or "H"
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?")
        val match = timeRegex.find(text)
        if (match != null) {
            val hour = match.groupValues[1].toIntOrNull() ?: 7
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            return AssistantIntent.SetAlarm(hour, minute, isPm)
        }

        // Word numbers: "six", "seven", etc.
        val wordNumbers = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "eleven" to 11, "twelve" to 12
        )
        for ((word, num) in wordNumbers) {
            if (text.contains(word)) {
                return AssistantIntent.SetAlarm(num, 0, isPm)
            }
        }

        return null
    }

    private fun parseTimerDuration(text: String): Int {
        var totalSeconds = 0

        val wordNums = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "fifteen" to 15, "twenty" to 20, "thirty" to 30, "forty-five" to 45
        )

        // Minutes regex
        val minMatch = Regex("(\\d+)\\s*(?:minute|min|m)").find(text)
        if (minMatch != null) {
            totalSeconds += (minMatch.groupValues[1].toIntOrNull() ?: 0) * 60
        } else {
            for ((word, num) in wordNums) {
                if (text.contains("$word minute") || text.contains("$word-minute")) {
                    totalSeconds += num * 60
                    break
                }
            }
        }

        // Seconds regex
        val secMatch = Regex("(\\d+)\\s*(?:second|sec|s)").find(text)
        if (secMatch != null) {
            totalSeconds += secMatch.groupValues[1].toIntOrNull() ?: 0
        } else {
            for ((word, num) in wordNums) {
                if (text.contains("$word second") || text.contains("$word-second")) {
                    totalSeconds += num
                    break
                }
            }
        }

        return if (totalSeconds > 0) totalSeconds else 300 // default 5 minutes
    }

    private fun parseSms(text: String): AssistantIntent.SendSms {
        // e.g. "send a message to Mom saying I will be late"
        // e.g. "text John hello"
        var working = text.removePrefix("send a message to ")
            .removePrefix("send message to ")
            .removePrefix("text ")
            .trim()

        if (working.contains(" saying ")) {
            val parts = working.split(" saying ", limit = 2)
            return AssistantIntent.SendSms(contactName = parts[0].trim(), message = parts[1].trim())
        }

        val tokens = working.split(" ", limit = 2)
        val contact = tokens[0].trim()
        val message = if (tokens.size > 1) tokens[1].trim() else null
        return AssistantIntent.SendSms(contactName = contact, message = message)
    }
}
