package com.example.nova.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.conversation.ConversationConfig
import com.example.nova.conversation.ConversationManager
import com.example.nova.permissions.PermissionManager
import com.example.nova.stt.ModelManager
import com.example.nova.stt.ModelStatus
import com.example.nova.tts.TtsManager
import com.example.nova.ui.theme.Black
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.GlowAmber
import com.example.nova.ui.theme.GlowRed
import com.example.nova.ui.theme.SurfaceDark
import com.example.nova.ui.theme.SurfaceVariantDark
import com.example.nova.ui.theme.TextSecondary
import com.example.nova.ui.theme.TextTertiary
import com.example.nova.ui.theme.TextWhite
import com.example.nova.wakeword.WakeWordManager

@Composable
fun SettingsScreen(
    conversationManager: ConversationManager,
    wakeWordManager: WakeWordManager,
    modelManager: ModelManager,
    permissionManager: PermissionManager,
    ttsManager: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by conversationManager.config.collectAsState()
    val modelStatus by modelManager.modelStatus.collectAsState()
    val isWakeWordActive by wakeWordManager.isListeningForWakeWord.collectAsState()

    var wakeWordEnabled by remember { mutableStateOf(true) }
    var followUpEnabled by remember { mutableStateOf(config.isContinuousConversationEnabled) }
    var activationResponseEnabled by remember { mutableStateOf(config.activationResponseEnabled) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
                Text(
                    text = "Settings",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ASSISTANT SECTION
                item {
                    SectionHeader(title = "ASSISTANT")
                    SettingsCard {
                        SettingSwitchItem(
                            title = "Wake Word Detection",
                            subtitle = "Listen for 'Hey Nova'",
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                            }
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingInfoItem(
                            title = "Wake Phrase",
                            value = "Hey Nova"
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingSwitchItem(
                            title = "Follow-Up Conversation",
                            subtitle = "Keep listening after speaking response",
                            checked = followUpEnabled,
                            onCheckedChange = {
                                followUpEnabled = it
                                conversationManager.updateConfig(config.copy(isContinuousConversationEnabled = it))
                            }
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingInfoItem(
                            title = "Follow-Up Timeout",
                            value = "${config.followUpTimeoutSeconds} seconds"
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingSwitchItem(
                            title = "Activation Spoken Response",
                            subtitle = "Say 'Yes?' when wake phrase is recognized",
                            checked = activationResponseEnabled,
                            onCheckedChange = {
                                activationResponseEnabled = it
                                conversationManager.updateConfig(config.copy(activationResponseEnabled = it))
                            }
                        )
                    }
                }

                // SPEECH SECTION
                item {
                    SectionHeader(title = "OFFLINE SPEECH ENGINE")
                    SettingsCard {
                        SettingInfoItem(
                            title = "Vosk STT Model",
                            value = when (modelStatus) {
                                ModelStatus.READY -> "Ready (English US)"
                                ModelStatus.CHECKING -> "Checking..."
                                ModelStatus.NOT_FOUND -> "Model Not Installed"
                                ModelStatus.CORRUPTED -> "Corrupt Model"
                                else -> "Not Loaded"
                            },
                            valueColor = if (modelStatus == ModelStatus.READY) CyanNova else GlowAmber
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingInfoItem(
                            title = "Wake-Word Engine",
                            value = if (isWakeWordActive) "Active (Screen-off enabled)" else "Idle",
                            valueColor = if (isWakeWordActive) CyanNova else TextSecondary
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingActionItem(
                            title = "Test Voice Response",
                            subtitle = "Tap to verify local TextToSpeech playback",
                            onClick = {
                                ttsManager.speak("Nova offline speech is operating normally.")
                            }
                        )
                    }
                }

                // PERMISSIONS SECTION
                item {
                    SectionHeader(title = "PERMISSIONS")
                    SettingsCard {
                        PermissionStatusItem(
                            title = "Microphone",
                            granted = permissionManager.hasRecordAudioPermission()
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        PermissionStatusItem(
                            title = "Contacts",
                            granted = permissionManager.hasContactsPermission()
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        PermissionStatusItem(
                            title = "Phone & Calling",
                            granted = permissionManager.hasPhonePermission()
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        PermissionStatusItem(
                            title = "SMS",
                            granted = permissionManager.hasSmsPermission()
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        PermissionStatusItem(
                            title = "Notifications",
                            granted = permissionManager.hasNotificationPermission()
                        )
                    }
                }

                // SYSTEM & RESTRICTIONS SECTION
                item {
                    SectionHeader(title = "SYSTEM & INTEGRATION")
                    SettingsCard {
                        SettingActionItem(
                            title = "Default Digital Assistant",
                            subtitle = "Configure Nova as Android's default assistant",
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingActionItem(
                            title = "Battery Optimization",
                            subtitle = "Disable battery restrictions for screen-off listening",
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                        HorizontalDivider(color = SurfaceDark, thickness = 1.dp)
                        SettingInfoItem(
                            title = "App Version",
                            value = "1.0.0 (Release Debug)"
                        )
                    }
                }

                // PRIVACY NOTICE
                item {
                    SectionHeader(title = "PRIVACY & SECURITY")
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy",
                                tint = CyanNova,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Voice processing happens locally on this device. Nova does not send your voice recordings to cloud AI services.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariantDark)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Black,
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}

@Composable
private fun SettingInfoItem(
    title: String,
    value: String,
    valueColor: Color = TextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = valueColor, fontSize = 14.sp)
    }
}

@Composable
private fun SettingActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun PermissionStatusItem(
    title: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (granted) CyanNova else GlowRed,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (granted) "Granted" else "Denied",
                color = if (granted) CyanNova else GlowRed,
                fontSize = 13.sp
            )
        }
    }
}
