package com.example.nova.nlu

import com.example.nova.nlu.handlers.AlarmIntentHandler
import com.example.nova.nlu.handlers.BatteryIntentHandler
import com.example.nova.nlu.handlers.BluetoothIntentHandler
import com.example.nova.nlu.handlers.CalendarIntentHandler
import com.example.nova.nlu.handlers.CallIntentHandler
import com.example.nova.nlu.handlers.CancelIntentHandler
import com.example.nova.nlu.handlers.DateIntentHandler
import com.example.nova.nlu.handlers.FallbackIntentHandler
import com.example.nova.nlu.handlers.FlashlightIntentHandler
import com.example.nova.nlu.handlers.FollowUpIntentHandler
import com.example.nova.nlu.handlers.MediaIntentHandler
import com.example.nova.nlu.handlers.NavigationIntentHandler
import com.example.nova.nlu.handlers.OpenAppIntentHandler
import com.example.nova.nlu.handlers.ReminderIntentHandler
import com.example.nova.nlu.handlers.SettingsIntentHandler
import com.example.nova.nlu.handlers.SmsIntentHandler
import com.example.nova.nlu.handlers.TimeIntentHandler
import com.example.nova.nlu.handlers.TimerIntentHandler
import com.example.nova.nlu.handlers.VolumeIntentHandler
import com.example.nova.nlu.handlers.WifiIntentHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRegistry @Inject constructor(
    alarmHandler: AlarmIntentHandler,
    timerHandler: TimerIntentHandler,
    callHandler: CallIntentHandler,
    smsHandler: SmsIntentHandler,
    openAppHandler: OpenAppIntentHandler,
    timeHandler: TimeIntentHandler,
    dateHandler: DateIntentHandler,
    batteryHandler: BatteryIntentHandler,
    flashlightHandler: FlashlightIntentHandler,
    wifiHandler: WifiIntentHandler,
    bluetoothHandler: BluetoothIntentHandler,
    volumeHandler: VolumeIntentHandler,
    mediaHandler: MediaIntentHandler,
    navigationHandler: NavigationIntentHandler,
    reminderHandler: ReminderIntentHandler,
    calendarHandler: CalendarIntentHandler,
    settingsHandler: SettingsIntentHandler,
    followUpHandler: FollowUpIntentHandler,
    cancelHandler: CancelIntentHandler,
    val fallbackHandler: FallbackIntentHandler
) {
    val handlers: List<IntentHandler> = listOf(
        alarmHandler,
        timerHandler,
        callHandler,
        smsHandler,
        openAppHandler,
        timeHandler,
        dateHandler,
        batteryHandler,
        flashlightHandler,
        wifiHandler,
        bluetoothHandler,
        volumeHandler,
        mediaHandler,
        navigationHandler,
        reminderHandler,
        calendarHandler,
        settingsHandler,
        followUpHandler,
        cancelHandler
    )

    fun findHandler(intent: AssistantIntent): IntentHandler {
        return handlers.firstOrNull { it.canHandle(intent) } ?: fallbackHandler
    }
}
