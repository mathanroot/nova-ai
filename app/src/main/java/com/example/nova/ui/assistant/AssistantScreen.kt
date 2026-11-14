package com.example.nova.ui.assistant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.assistant.AssistantState
import com.example.nova.assistant.AssistantViewModel
import com.example.nova.assistant.toOrbMode
import com.example.nova.stt.ModelStatus
import com.example.nova.ui.components.LiveTranscriptionView
import com.example.nova.ui.components.MicActivityIndicator
import com.example.nova.ui.components.NovaOrb
import com.example.nova.ui.theme.Black
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.GlowAmber
import com.example.nova.ui.theme.GlowRed
import com.example.nova.ui.theme.SurfaceDark
import com.example.nova.ui.theme.TextSecondary
import com.example.nova.ui.theme.TextTertiary
import com.example.nova.ui.theme.TextWhite

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.assistantState.collectAsState()
    val liveTranscription by viewModel.liveTranscription.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()

    val orbMode = state.toOrbMode()
    val isListening = state is AssistantState.Listening ||
            state is AssistantState.SpeechPaused ||
            state is AssistantState.FollowUpListening

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nova Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (modelStatus) {
                                ModelStatus.READY -> CyanNova
                                ModelStatus.NOT_FOUND -> GlowAmber
                                ModelStatus.CORRUPTED -> GlowRed
                                else -> ElectricBlue
                            }
                        )
                )
                Text(
                    text = "NOVA",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
            }

            // Actions: Mic indicator & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.onOrbClicked() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone Status",
                        tint = if (isListening) ElectricBlue else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // CENTER: Animated Glowing Orb and Prompts
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NovaOrb(
                mode = orbMode,
                audioLevel = audioLevel,
                size = 280.dp,
                onClick = { viewModel.onOrbClicked() }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Assistant Status & Voice Prompts
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "StatusTextTransition"
            ) { targetState ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (targetState) {
                        is AssistantState.Idle -> {
                            Text(
                                text = "Hey, I'm Nova",
                                color = TextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Say \"Hey Nova\" or tap the orb",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                        is AssistantState.WakeDetected -> {
                            Text(
                                text = "Hey Nova",
                                color = ElectricBlue,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is AssistantState.Listening -> {
                            Text(
                                text = "Listening…",
                                color = CyanNova,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is AssistantState.SpeechPaused -> {
                            Text(
                                text = "I'm listening…",
                                color = CyanNova.copy(alpha = 0.8f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        is AssistantState.Processing -> {
                            Text(
                                text = "Thinking…",
                                color = ElectricBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is AssistantState.Speaking -> {
                            Text(
                                text = targetState.responseText,
                                color = TextWhite,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }
                        is AssistantState.FollowUpListening -> {
                            Text(
                                text = "Listening for follow-up…",
                                color = CyanNova,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        is AssistantState.Error -> {
                            Text(
                                text = targetState.errorMessage,
                                color = GlowRed,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real-Time Partial & Final Transcription View
            LiveTranscriptionView(text = liveTranscription)
        }

        // BOTTOM: Mic Activity Waveform Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            MicActivityIndicator(
                audioLevel = audioLevel,
                isListening = isListening
            )
        }
    }
}
