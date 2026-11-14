package com.example.nova.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.SurfaceDark
import com.example.nova.ui.theme.TextSecondary
import com.example.nova.ui.theme.TextWhite

@Composable
fun SiriResponseCard(
    spokenText: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = spokenText.isNotBlank(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
        modifier = modifier
    ) {
        val lower = spokenText.lowercase()

        // 1. Alarm Card Widget
        if (lower.contains("alarm is set") || lower.contains("alarm for")) {
            AlarmWidgetCard(spokenText = spokenText)
        }
        // 2. Timer Card Widget
        else if (lower.contains("timer set") || lower.contains("timer for")) {
            TimerWidgetCard(spokenText = spokenText)
        }
        // 3. Battery Card Widget
        else if (lower.contains("battery is at")) {
            BatteryWidgetCard(spokenText = spokenText)
        }
        // 4. Calling Card Widget
        else if (lower.startsWith("calling ")) {
            CallWidgetCard(spokenText = spokenText)
        }
        // 5. Standard Glassmorphic Siri Speech Bubble
        else {
            StandardSiriBubble(spokenText = spokenText)
        }
    }
}

@Composable
private fun AlarmWidgetCard(spokenText: String) {
    var isEnabled by remember { mutableStateOf(true) }
    val timeMatch = Regex("(\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?)").find(spokenText)
    val displayTime = timeMatch?.value ?: "07:00 AM"

    GlassmorphicContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Alarm",
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = displayTime,
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Alarm",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759), // iOS Green
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = SurfaceDark
                )
            )
        }
    }
}

@Composable
private fun TimerWidgetCard(spokenText: String) {
    GlassmorphicContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9F0A).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFFFF9F0A),
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = spokenText.removePrefix("Your ").removePrefix("The "),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Timer running",
                    color = Color(0xFFFF9F0A),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BatteryWidgetCard(spokenText: String) {
    val percentMatch = Regex("(\\d+)\\s*percent").find(spokenText)
    val percent = percentMatch?.groupValues?.get(1) ?: "85"

    GlassmorphicContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = "$percent%",
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = spokenText,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CallWidgetCard(spokenText: String) {
    val name = spokenText.removePrefix("Calling ").removeSuffix(".")

    GlassmorphicContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    text = name,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Connecting call…",
                    color = Color(0xFF34C759),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StandardSiriBubble(spokenText: String) {
    GlassmorphicContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = spokenText,
                color = TextWhite,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun GlassmorphicContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1C1C24).copy(alpha = 0.75f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}
