package com.example.nova

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.nova.assistant.AssistantCoordinator
import com.example.nova.assistant.AssistantViewModel
import com.example.nova.conversation.ConversationManager
import com.example.nova.permissions.PermissionManager
import com.example.nova.stt.ModelManager
import com.example.nova.tts.TtsManager
import com.example.nova.ui.assistant.AssistantScreen
import com.example.nova.ui.settings.SettingsScreen
import com.example.nova.ui.theme.Black
import com.example.nova.ui.theme.NovaTheme
import com.example.nova.wakeword.WakeWordForegroundService
import com.example.nova.wakeword.WakeWordManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class Screen {
    ASSISTANT,
    SETTINGS
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val assistantViewModel: AssistantViewModel by viewModels()

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var assistantCoordinator: AssistantCoordinator

    @Inject
    lateinit var conversationManager: ConversationManager

    @Inject
    lateinit var wakeWordManager: WakeWordManager

    @Inject
    lateinit var modelManager: ModelManager

    @Inject
    lateinit var ttsManager: TtsManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            WakeWordForegroundService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        configureLockScreenFlags()
        requestInitialPermissions()

        if (intent?.getBooleanExtra("EXTRA_VOICE_TRIGGER", false) == true) {
            assistantCoordinator.onWakeWordTriggered()
        }

        setContent {
            NovaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Black
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.ASSISTANT) }

                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.ASSISTANT -> {
                                AssistantScreen(
                                    viewModel = assistantViewModel,
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    conversationManager = conversationManager,
                                    wakeWordManager = wakeWordManager,
                                    modelManager = modelManager,
                                    permissionManager = permissionManager,
                                    ttsManager = ttsManager,
                                    onBack = { currentScreen = Screen.ASSISTANT }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionManager.hasRecordAudioPermission()) {
            WakeWordForegroundService.startService(this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockScreenFlags()
        if (intent?.getBooleanExtra("EXTRA_VOICE_TRIGGER", false) == true) {
            assistantCoordinator.onWakeWordTriggered()
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun requestInitialPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (!permissionManager.hasRecordAudioPermission()) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionManager.hasNotificationPermission()) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            WakeWordForegroundService.startService(this)
        }
    }
}
