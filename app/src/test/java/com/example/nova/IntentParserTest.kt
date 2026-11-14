package com.example.nova

import com.example.nova.nlu.AssistantIntent
import com.example.nova.nlu.IntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntentParserTest {

    private lateinit var parser: IntentParser

    @Before
    fun setup() {
        parser = IntentParser()
    }

    @Test
    fun testAlarmParsing() {
        // "Set an alarm for 6 AM"
        val intent1 = parser.parse("Set an alarm for 6 AM")
        assertTrue(intent1 is AssistantIntent.SetAlarm)
        assertEquals(6, (intent1 as AssistantIntent.SetAlarm).hour)
        assertEquals(false, intent1.isPm)

        // "Wake me at 7:30"
        val intent2 = parser.parse("Wake me at 7:30")
        assertTrue(intent2 is AssistantIntent.SetAlarm)
        val alarm2 = intent2 as AssistantIntent.SetAlarm
        assertEquals(7, alarm2.hour)
        assertEquals(30, alarm2.minute)

        // "Cancel my alarm"
        val intent3 = parser.parse("Cancel my alarm")
        assertTrue(intent3 is AssistantIntent.CancelAlarm)
    }

    @Test
    fun testTimerParsing() {
        // "Set a timer for ten minutes"
        val intent1 = parser.parse("Set a timer for ten minutes")
        assertTrue(intent1 is AssistantIntent.SetTimer)
        assertEquals(600, (intent1 as AssistantIntent.SetTimer).durationSeconds)

        // "Start a five minute timer"
        val intent2 = parser.parse("Start a five minute timer")
        assertTrue(intent2 is AssistantIntent.SetTimer)
        assertEquals(300, (intent2 as AssistantIntent.SetTimer).durationSeconds)

        // "Stop the timer"
        val intent3 = parser.parse("Stop the timer")
        assertTrue(intent3 is AssistantIntent.StopTimer)
    }

    @Test
    fun testCallParsing() {
        val intent1 = parser.parse("Call Mom")
        assertTrue(intent1 is AssistantIntent.MakeCall)
        assertEquals("mom", (intent1 as AssistantIntent.MakeCall).contactName)

        val intent2 = parser.parse("Hey Nova, call John")
        assertTrue(intent2 is AssistantIntent.MakeCall)
        assertEquals("john", (intent2 as AssistantIntent.MakeCall).contactName)
    }

    @Test
    fun testSmsParsing() {
        val intent1 = parser.parse("Send a message to Mom saying I will be late")
        assertTrue(intent1 is AssistantIntent.SendSms)
        val sms = intent1 as AssistantIntent.SendSms
        assertEquals("mom", sms.contactName)
        assertEquals("i will be late", sms.message)

        val intent2 = parser.parse("Text John hello")
        assertTrue(intent2 is AssistantIntent.SendSms)
        val sms2 = intent2 as AssistantIntent.SendSms
        assertEquals("john", sms2.contactName)
        assertEquals("hello", sms2.message)
    }

    @Test
    fun testOpenAppParsing() {
        val intent1 = parser.parse("Open YouTube")
        assertTrue(intent1 is AssistantIntent.OpenApp)
        assertEquals("youtube", (intent1 as AssistantIntent.OpenApp).appName)

        val intent2 = parser.parse("Open WhatsApp")
        assertTrue(intent2 is AssistantIntent.OpenApp)
        assertEquals("whatsapp", (intent2 as AssistantIntent.OpenApp).appName)

        val intent3 = parser.parse("Launch Camera")
        assertTrue(intent3 is AssistantIntent.OpenApp)
        assertEquals("camera", (intent3 as AssistantIntent.OpenApp).appName)
    }

    @Test
    fun testTimeAndDateParsing() {
        val timeIntent = parser.parse("What time is it?")
        assertTrue(timeIntent is AssistantIntent.GetTime)

        val dateIntent = parser.parse("What's today's date?")
        assertTrue(dateIntent is AssistantIntent.GetDate)
    }

    @Test
    fun testBatteryParsing() {
        val intent = parser.parse("What's my battery percentage?")
        assertTrue(intent is AssistantIntent.GetBattery)
    }

    @Test
    fun testFlashlightParsing() {
        val intentOn = parser.parse("Turn on the flashlight")
        assertTrue(intentOn is AssistantIntent.ToggleFlashlight)
        assertTrue((intentOn as AssistantIntent.ToggleFlashlight).enable)

        val intentOff = parser.parse("Turn off the flashlight")
        assertTrue(intentOff is AssistantIntent.ToggleFlashlight)
        assertEquals(false, (intentOff as AssistantIntent.ToggleFlashlight).enable)
    }

    @Test
    fun testWifiAndBluetoothParsing() {
        val wifiIntent = parser.parse("Turn Wi-Fi on")
        assertTrue(wifiIntent is AssistantIntent.ToggleWifi)

        val btIntent = parser.parse("Turn Bluetooth off")
        assertTrue(btIntent is AssistantIntent.ToggleBluetooth)
    }

    @Test
    fun testVolumeAndMediaParsing() {
        val volUp = parser.parse("Volume up")
        assertTrue(volUp is AssistantIntent.SetVolume)
        assertEquals(AssistantIntent.VolumeDirection.UP, (volUp as AssistantIntent.SetVolume).direction)

        val volSet = parser.parse("Set volume to 50 percent")
        assertTrue(volSet is AssistantIntent.SetVolume)
        assertEquals(50, (volSet as AssistantIntent.SetVolume).percent)

        val mediaPlay = parser.parse("Play music")
        assertTrue(mediaPlay is AssistantIntent.MediaControl)
        assertEquals(AssistantIntent.MediaAction.PLAY, (mediaPlay as AssistantIntent.MediaControl).action)

        val mediaNext = parser.parse("Next song")
        assertTrue(mediaNext is AssistantIntent.MediaControl)
        assertEquals(AssistantIntent.MediaAction.NEXT, (mediaNext as AssistantIntent.MediaControl).action)
    }

    @Test
    fun testNavigationParsing() {
        val nav = parser.parse("Navigate to Chennai")
        assertTrue(nav is AssistantIntent.Navigate)
        assertEquals("chennai", (nav as AssistantIntent.Navigate).destination)
    }

    @Test
    fun testReminderAndCalendarParsing() {
        val reminder = parser.parse("Remind me to call John")
        assertTrue(reminder is AssistantIntent.SetReminder)
        assertTrue((reminder as AssistantIntent.SetReminder).reminderText.contains("call john"))

        val calendar = parser.parse("Add a meeting tomorrow at 10 AM")
        assertTrue(calendar is AssistantIntent.AddCalendarEvent)
    }

    @Test
    fun testCancelAndFallback() {
        val cancel = parser.parse("Stop listening")
        assertTrue(cancel is AssistantIntent.Cancel)

        val fallback = parser.parse("What is the meaning of quantum entanglement?")
        assertTrue(fallback is AssistantIntent.Fallback)
    }
}
