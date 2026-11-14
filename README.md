# Nova — Real-Time Offline Android Voice Assistant

Nova is a production-oriented, 100% offline Android voice assistant built in Kotlin and Jetpack Compose. Nova delivers a premium, immersive voice assistant experience with:

- **True Screen-Off Wake-Word Activation** ("Hey Nova")
- **Lock-Screen Voice Responses** without requiring phone unlocking
- **Real-Time Streaming Speech Recognition** with partial live transcription
- **Natural Human Pause & Continuation Detection** (handles mid-sentence thinking pauses)
- **Continuous Follow-Up Conversations** (no repeated wake words needed during follow-up sessions)
- **Deterministic Offline NLU Engine** (20 intent handlers, no cloud AI or external APIs)
- **Offline Spoken Responses** via Android TextToSpeech
- **Original Glowing Orb UI** with organic fluid deformation and microphone audio reactivity
- **Android Default Digital Assistant Integration** (`VoiceInteractionService`)

> **IMPORTANT PRIVACY GUARANTEE:**
> Nova has **ABSOLUTELY NO CLOUD AI OR EXTERNAL AI APIs** (No OpenAI, ChatGPT, Gemini, Claude, or online voice processors). All wake-word detection, speech-to-text recognition, command interpretation, and speech synthesis execute 100% locally on-device. The application functions completely in Airplane Mode.

---

## 1. Project Architecture

Nova follows modern Android MVVM and Clean Architecture patterns:

```
com.example.nova/
├── NovaApplication.kt                   # Application subclass & notification channels
├── MainActivity.kt                      # Lock-screen aware Activity & Compose host
├── assistant/
│   ├── AssistantState.kt                # Idle, WakeDetected, Listening, Speaking, etc.
│   ├── AssistantStateMachine.kt         # Thread-safe state transitions
│   ├── AssistantCoordinator.kt          # Lifecycle coordination across subsystems
│   └── AssistantViewModel.kt            # StateFlow UI bindings
├── audio/
│   ├── AudioRecorder.kt                 # 16kHz 16-bit mono PCM capture
│   ├── AudioLevelMonitor.kt             # RMS dBFS & smoothed 0.0-1.0 orb reactivity
│   ├── MicrophoneManager.kt             # Mutual exclusion & ownership handover
│   └── AudioFocusManager.kt             # Transient assistant audio focus & ducking
├── wakeword/
│   ├── WakeWordConfig.kt                # Sensitivity & debounce configuration
│   ├── WakeWordEngine.kt                # Local inference interface
│   ├── AndroidWakeWordEngine.kt         # Keyword grammar + acoustic pattern spotter
│   ├── WakeWordManager.kt               # Handover to Vosk on trigger
│   └── WakeWordForegroundService.kt     # Screen-off foreground microphone service
├── stt/
│   ├── SpeechConfig.kt                  # Pause & silence duration thresholds
│   ├── SpeechResult.kt                  # Partial, Final, Error results
│   ├── SpeechRecognizer.kt              # Streaming speech recognizer contract
│   ├── VoskSpeechRecognizer.kt          # Offline Kaldi/Vosk speech recognition
│   ├── ModelManager.kt                  # Model extraction & health checks
│   ├── VoiceActivityDetector.kt         # Real-time VAD & silence calculator
│   ├── UtteranceManager.kt              # Mid-sentence pause accumulator
│   └── SpeechSessionManager.kt          # Orchestrator for active listening turn
├── nlu/
│   ├── AssistantIntent.kt               # Strongly typed intent definitions
│   ├── IntentResult.kt                  # Spoken response & follow-up flags
│   ├── IntentHandler.kt                 # Intent handler contract
│   ├── CommandRegistry.kt               # Central registration of all 20 handlers
│   ├── IntentParser.kt                  # Deterministic offline token & regex parser
│   ├── CommandExecutor.kt               # Safe handler invocation
│   └── handlers/                        # 20 Individual Intent Handlers
├── actions/                             # Android system intent bridges (Alarm, Timer, Contacts, etc.)
├── conversation/
│   ├── ConversationConfig.kt            # Follow-up timeouts & auto-listen config
│   ├── ConversationSession.kt           # Turn tracking & disambiguation state
│   ├── SpeechTurnManager.kt             # Multi-turn conversation manager
│   ├── FollowUpListener.kt              # Post-TTS follow-up window timer
│   └── ConversationManager.kt           # Master conversation lifecycle
├── tts/
│   ├── TtsConfig.kt                     # Voice & speech rate configuration
│   ├── TtsEngine.kt                     # Speech synthesis contract
│   ├── AndroidTtsEngine.kt              # Android TextToSpeech with offline voices
│   └── TtsManager.kt                    # TTS queue & mic suppression during output
├── service/
│   ├── NovaVoiceInteractionService.kt   # System VoiceInteractionService
│   ├── NovaVoiceInteractionSession.kt   # Assistant gesture / button session
│   └── NovaRecognitionService.kt        # System RecognitionService integration
├── permissions/
│   └── PermissionManager.kt             # Progressive runtime permission checks
├── ui/
│   ├── theme/                           # Electric Blue, Cyan, Violet, Magenta AMOLED theme
│   ├── components/
│   │   ├── NovaOrb.kt                   # Multi-layer canvas glowing animated orb
│   │   ├── MicActivityIndicator.kt      # Glowing audio level waveform
│   │   └── LiveTranscriptionView.kt     # Partial speech transcription overlay
│   ├── assistant/AssistantScreen.kt     # Main interactive assistant screen
│   └── settings/SettingsScreen.kt       # Full configuration dashboard
└── di/                                  # Dagger Hilt dependency injection modules
```

---

## 2. How to Open the Project in Android Studio

1. Download or clone this repository:
   ```bash
   git clone https://github.com/your-username/nova-siri.git
   ```
2. Open **Android Studio** (Hedgehog 2023.1.1 or Iguana/Jellyfish/Koala).
3. Select **Open** and select the root directory (`d:\nova-siri` or repository folder).
4. Android Studio will automatically recognize Gradle and run Gradle Sync.
5. Ensure **JDK 17** is configured in **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.

---

## 3. How to Build the APK

### Via GitHub Actions (Continuous Integration)
This repository includes a fully automated workflow located at `.github/workflows/build.yml`.
- Every push to `main` or pull request automatically:
  1. Checks out the code.
  2. Sets up JDK 17 and Android SDK 34.
  3. Executes all unit tests: `./gradlew testDebugUnitTest`.
  4. Builds the debug APK: `./gradlew assembleDebug`.
  5. Uploads the generated `nova-debug-apk` and test reports to the GitHub Actions Run page.

### Via Command Line (Gradle)
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```
The generated APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 4. How to Install the APK

Install on your connected Android device or emulator via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. How to Download and Install the Vosk Model

Nova uses the ultra-compact, high-accuracy offline English model: `vosk-model-small-en-us-0.15`.

1. Download the model zip archive (approx. 40MB):
   ```bash
   curl -LO https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
   ```
2. Unzip the archive. You will see a directory named `vosk-model-small-en-us-0.15` containing folders like `am/`, `conf/`, `graph/`, and `ivector/`.
3. Rename the unzipped folder to `vosk-model`.
4. Push the directory to Nova's internal app files storage via ADB:
   ```bash
   adb push vosk-model /data/data/com.example.nova/files/vosk-model
   ```
   *Alternative:* You can place `vosk-model-small-en-us-0.15.zip` directly into `app/src/main/assets/` before compiling, and Nova will automatically unpack it on first launch.

---

## 6. How to Obtain and Install the Wake-Word Model

Nova's `AndroidWakeWordEngine` includes:
1. **Integrated Vosk Keyword Spotter**: Once the Vosk model is in place, Nova configures a constrained grammar detector for `"hey nova"`.
2. **Acoustic Phoneme Feature Matcher**: Detects speech bursts matching the phonetic profile of "Hey Nova".
3. **Custom Model Path**: You can place any custom TFLite or ONNX wake-word model into `/data/data/com.example.nova/files/hey_nova.onnx` or `hey_nova.tflite` to enable dedicated deep acoustic inference.

---

## 7. How to Enable the Wake-Word Service

1. Launch Nova on your device.
2. Grant the requested **Microphone** permission.
3. Grant **Notification** permission (Android 13+).
4. The persistent notification `"Nova is listening for 'Hey Nova'"` will appear in your status bar.
5. The foreground service `WakeWordForegroundService` is now active and monitoring for the wake phrase.

---

## 8. How to Configure Permissions

Nova practices **progressive permission disclosure**:
- **RECORD_AUDIO**: Required on startup to listen for the wake phrase and speech commands.
- **POST_NOTIFICATIONS**: Required on Android 13+ to maintain the foreground microphone service.
- **READ_CONTACTS & CALL_PHONE**: Requested only when you issue calling commands (e.g. "Call Mom").
- **SEND_SMS**: Requested only when sending messages directly.
- **SCHEDULE_EXACT_ALARM**: Requested by Android when creating timed alarms.

To configure all permissions in one place:
1. Open Nova Settings (gear icon in the top right).
2. Scroll to the **PERMISSIONS** section.
3. Review status indicators.

---

## 9. How to Allow Microphone Foreground Service Operation

Android 14 (API 34) introduced strict rules for foreground services using the microphone:
- `WakeWordForegroundService` is explicitly declared with `android:foregroundServiceType="microphone"`.
- A persistent notification is displayed while capture is running.
- If you start the app, the foreground service is initiated from a visible Activity component, fulfilling Android 14's runtime requirements.

---

## 10. How to Configure Battery Optimization

For reliable background and screen-off operation:
1. Open Nova Settings.
2. Under **SYSTEM & INTEGRATION**, tap **Battery Optimization**.
3. Locate **Nova** in the app list and select **Don't optimize** (or **Unrestricted**).
4. This prevents Android's Doze mode from killing the foreground wake-word listener when the phone is idle.

---

## 11. How to Select Nova as Default Android Assistant

1. Open Nova Settings.
2. Tap **Default Digital Assistant**.
3. Select **Default digital assistant app** in Android Settings.
4. Choose **Nova**.
5. You can now invoke Nova by:
   - Long-pressing the home button / power button (depending on device settings).
   - Swiping diagonally from the bottom corner (gesture navigation).
   - Saying "Hey Nova".

---

## 12. How to Enable Screen-Off Activation

1. Ensure the persistent notification `"Nova is listening for 'Hey Nova'"` is visible.
2. Turn off the screen (lock the device).
3. Say: **"Hey Nova"**.
4. Nova detects the wake phrase without waking the display or requiring an unlock.
5. Nova answers: **"Yes?"**
6. Speak your request (e.g., *"What time is it?"*).
7. Nova speaks the answer (*"It's 9:30 PM."*) through the speaker with the display remaining off.

---

## 13. How to Configure Follow-Up Conversation

Nova includes a configurable **Conversation Mode** (Scenario D):
1. In Nova Settings, toggle **Follow-Up Conversation** ON.
2. Set the follow-up window (default: **10 seconds**).
3. When Nova finishes answering, it automatically re-opens the microphone for follow-up questions without requiring "Hey Nova" again.
4. If no speech is detected before the timeout, Nova closes the session and resumes background wake-word detection.

---

## 14. How to Adjust Speech Pause Detection

Nova's `UtteranceManager` and `VoiceActivityDetector` handle natural pauses (Scenario C):
- **Short pause (0.5s – 1.5s):** User is thinking mid-sentence; continues listening.
- **Normal pause (1.5s – 2.5s):** Buffers speech and waits for continuation.
- **Final silence (2.8s):** Finalizes and executes the complete sentence.
- You can tune these thresholds in `SpeechConfig.kt`.

---

## 15. How to Test Continuous Conversation

Try this multi-turn dialogue without touching the phone:
```
User: "Hey Nova."
Nova: "Yes?"
User: "What's my battery?"
Nova: "Your battery is at 82 percent."
[Follow-up window opens automatically]
User: "What time is it?"
Nova: "It's 10:15 PM."
[Follow-up window opens automatically]
User: "Set a timer for five minutes."
Nova: "Timer set for 5 minutes."
[Wait 10 seconds without speaking]
Nova returns to idle wake-word detection.
```

---

## 16. 30 Practical Offline Voice Command Examples

### Alarms & Timers
1. *"Hey Nova, set an alarm for 6 AM."*
2. *"Wake me at 7:30."*
3. *"Set an alarm for tomorrow morning at 8."*
4. *"Cancel my alarm."*
5. *"Set a timer for ten minutes."*
6. *"Start a five minute timer."*
7. *"Stop the timer."*

### Phone & Communication
8. *"Call Mom."*
9. *"Call John."*
10. *"Send a message to Mom saying I will be late."*
11. *"Text John hello."*

### Applications
12. *"Open YouTube."*
13. *"Open WhatsApp."*
14. *"Open Chrome."*
15. *"Open Spotify."*
16. *"Open Camera."*
17. *"Open Calculator."*
18. *"Open Settings."*

### Time, Date & Battery
19. *"What time is it?"*
20. *"Tell me the current time."*
21. *"What's today's date?"*
22. *"What day is it?"*
23. *"What's my battery percentage?"*
24. *"Is my battery low?"*

### Hardware Controls
25. *"Turn on the flashlight."*
26. *"Turn off the flashlight."*
27. *"Volume up."*
28. *"Set volume to 50 percent."*
29. *"Play music."*
30. *"Pause music."*
31. *"Next song."*

### Navigation & Reminders
32. *"Navigate to Chennai."*
33. *"Take me to the airport."*
34. *"Remind me to call John."*
35. *"Add a meeting tomorrow at 10 AM."*

### Follow-Up & Control
36. *"What else can you do?"*
37. *"Stop listening."*
38. *"Cancel."*

---

## 17. Device-Specific Android Restrictions

- **Google Pixel:** Best background microphone retention. Ensure battery optimization is disabled for Nova.
- **Samsung One UI:** Samsung aggressive background task cleaner requires putting Nova in "Never sleeping apps" in Settings > Battery and device care > Background usage limits.
- **Xiaomi (MIUI / HyperOS):** Must enable "Autostart" permission in app settings and set Battery saver to "No restrictions".
- **OnePlus (OxygenOS):** Disable "Deep optimization" in Battery settings.

---

## 18. Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| Model Not Installed indicator | Vosk model missing from internal files | Download `vosk-model-small-en-us-0.15` and place it in `/data/data/com.example.nova/files/vosk-model` |
| Mic does not trigger screen-off | Battery saver killed foreground service | Disable Battery Optimization for Nova in Android Settings |
| Audio conflicts or crash | Dual AudioRecord contention | Nova's `MicrophoneManager` enforces strict exclusive ownership; ensure permissions are granted |
| Spoken voice sounds robotic | Default system TTS engine voice | Open Android Text-to-Speech settings and select a high-quality local offline voice pack |

---

## 19. Unit Testing

Run the included JUnit tests locally or in CI:
```bash
./gradlew testDebugUnitTest
```
Tests included in `app/src/test/java/com/example/nova/`:
- `IntentParserTest`: Verifies all 20 commands, natural variations, and fallbacks.
- `SpeechSessionTest`: Verifies Scenario C mid-sentence pause buffering and continuation.
- `MicrophoneManagerTest`: Verifies mutual exclusion and safe ownership handovers.
- `ConversationManagerTest`: Verifies multi-turn session tracking and state machine transitions.
- `WakeWordManagerTest`: Verifies wake word debounce timers and trigger windows.
