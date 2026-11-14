package com.example.nova.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.nova.ui.theme.ElectricBlue

@Composable
fun MicActivityIndicator(
    audioLevel: Float, // 0.0f .. 1.0f
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val factor = when (index) {
                0, 4 -> 0.45f
                1, 3 -> 0.75f
                else -> 1.0f
            }

            val targetHeight = if (isListening) {
                6.dp + (24.dp * (audioLevel * factor))
            } else {
                4.dp
            }

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight.value,
                label = "BarHeight$index"
            )

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ElectricBlue.copy(alpha = if (isListening) 0.85f else 0.25f))
            )
        }
    }
}
