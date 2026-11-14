package com.example.nova.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.nova.assistant.OrbVisualMode
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.MagentaNova
import com.example.nova.ui.theme.PurpleNova
import com.example.nova.ui.theme.VioletNova

@Composable
fun SiriEdgeGlow(
    mode: OrbVisualMode,
    audioLevel: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier
) {
    if (mode == OrbVisualMode.IDLE) return

    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlowTransition")

    // Continuous rotation of colors around screen edge
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EdgeGlowRotation"
    )

    // Animated intensity driven by speech audio level
    val targetGlowWidth = when (mode) {
        OrbVisualMode.LISTENING, OrbVisualMode.SPEAKING -> 18.dp.value + (audioLevel * 30.dp.value)
        OrbVisualMode.WAKE_DETECTED -> 28.dp.value
        OrbVisualMode.PROCESSING -> 14.dp.value
        else -> 8.dp.value
    }

    val animatedGlowWidth by animateFloatAsState(
        targetValue = targetGlowWidth,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "GlowWidthAnimation"
    )

    val gradientColors = listOf(
        ElectricBlue,
        CyanNova,
        MagentaNova,
        VioletNova,
        ElectricBlue
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Outer glow path around the entire display edge
        val rectPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Draw dynamic blurred edge glow ribbon
        drawPath(
            path = rectPath,
            brush = Brush.sweepGradient(
                colors = gradientColors,
                center = Offset(w / 2f, h / 2f)
            ),
            style = Stroke(width = animatedGlowWidth * 1.5f),
            alpha = 0.45f
        )

        // Inner sharp ribbon
        drawPath(
            path = rectPath,
            brush = Brush.sweepGradient(
                colors = gradientColors.reversed(),
                center = Offset(w / 2f, h / 2f)
            ),
            style = Stroke(width = animatedGlowWidth * 0.45f),
            alpha = 0.85f
        )
    }
}
