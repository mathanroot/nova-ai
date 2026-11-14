package com.example.nova.ui.assistant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
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
import com.example.nova.ui.components.LiveTranscriptionView
import com.example.nova.ui.components.MicActivityIndicator
import com.example.nova.ui.components.SiriEdgeGlow
import com.example.nova.ui.components.SiriLiquidOrb
import com.example.nova.ui.components.SiriResponseCard
import com.example.nova.ui.theme.Black
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.GlowRed
import com.example.nova.ui.theme.SurfaceDark
import com.example.nova.ui.theme.TextSecondary
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

    val orbMode = state.toOrbMode()
    val isListening = state is AssistantState.Listening ||
            state is AssistantState.SpeechPaused ||
            state is AssistantState.FollowUpListening

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (state !is AssistantState.Idle) {
                        viewModel.cancel()
                    }
                }
            )
    ) {
        // 1. Siri-Style Edge Glow Ribbon (Illuminates full screen border on wake/speak)
        SiriEdgeGlow(
            mode = orbMode,
            audioLevel = audioLevel
        )

        // 2. Minimal Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isListening) CyanNova else ElectricBlue)
                )
                Text(
                    text = "NOVA",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 3. Center Content Area (Cards, Transcription, Siri Prompts)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Siri Status Prompt
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SiriPromptAnim"
            ) { targetState ->
                when (targetState) {
                    is AssistantState.Idle -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "What can I help you with?",
                                color = TextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Say \"Hey Nova\" or tap the glowing orb",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                    is AssistantState.WakeDetected -> {
                        Text(
                            text = "Go ahead…",
                            color = ElectricBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is AssistantState.Listening, is AssistantState.SpeechPaused, is AssistantState.FollowUpListening -> {
                        Text(
                            text = if (targetState is AssistantState.SpeechPaused) "I'm listening…" else "Listening…",
                            color = CyanNova,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
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
                        // Display Siri Interactive Response Card
                        SiriResponseCard(spokenText = targetState.responseText)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Real-Time Floating Partial Transcription
            LiveTranscriptionView(text = liveTranscription)
        }

        // 4. Bottom Siri Liquid Orb & Mic Waveform
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MicActivityIndicator(
                audioLevel = audioLevel,
                isListening = isListening
            )

            // Siri Liquid Metal Bottom Orb
            SiriLiquidOrb(
                mode = orbMode,
                audioLevel = audioLevel,
                size = 140.dp,
                onClick = { viewModel.onOrbClicked() }
            )
        }
    }
}
