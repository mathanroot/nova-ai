package com.example.nova.nlu

sealed interface AssistantIntent {
    // 1. Alarm
    data class SetAlarm(val hour: Int, val minute: Int, val isPm: Boolean? = null, val label: String? = null) : AssistantIntent
    object CancelAlarm : AssistantIntent

    // 2. Timer
    data class SetTimer(val durationSeconds: Int, val label: String? = null) : AssistantIntent
    object StopTimer : AssistantIntent

    // 3. Call
    data class MakeCall(val contactName: String) : AssistantIntent

    // 4. SMS
    data class SendSms(val contactName: String, val message: String? = null) : AssistantIntent

    // 5. Open App
    data class OpenApp(val appName: String) : AssistantIntent

    // 6. Time
    object GetTime : AssistantIntent

    // 7. Date
    object GetDate : AssistantIntent

    // 8. Battery
    object GetBattery : AssistantIntent

    // 9. Flashlight
    data class ToggleFlashlight(val enable: Boolean) : AssistantIntent

    // 10. Wi-Fi
    data class ToggleWifi(val enable: Boolean) : AssistantIntent

    // 11. Bluetooth
    data class ToggleBluetooth(val enable: Boolean) : AssistantIntent

    // 12. Volume
    enum class VolumeDirection { UP, DOWN, MUTE, SET }
    data class SetVolume(val direction: VolumeDirection, val percent: Int? = null) : AssistantIntent

    // 13. Media
    enum class MediaAction { PLAY, PAUSE, RESUME, NEXT, PREVIOUS, STOP }
    data class MediaControl(val action: MediaAction) : AssistantIntent

    // 14. Navigation
    data class Navigate(val destination: String) : AssistantIntent

    // 15. Reminder
    data class SetReminder(val reminderText: String, val timeHint: String? = null) : AssistantIntent

    // 16. Calendar
    data class AddCalendarEvent(val title: String, val dateOrTime: String? = null) : AssistantIntent

    // 17. Settings
    data class OpenSettings(val settingName: String? = null) : AssistantIntent

    // 18. FollowUp
    data class FollowUp(val command: String) : AssistantIntent

    // 19. Cancel
    object Cancel : AssistantIntent

    // 20. Fallback
    data class Fallback(val rawText: String) : AssistantIntent
}
