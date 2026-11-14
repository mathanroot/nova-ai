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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nova.assistant.OrbVisualMode
import com.example.nova.ui.theme.CyanNova
import com.example.nova.ui.theme.ElectricBlue
import com.example.nova.ui.theme.GlowAmber
import com.example.nova.ui.theme.GlowRed
import com.example.nova.ui.theme.MagentaNova
import com.example.nova.ui.theme.PurpleNova
import com.example.nova.ui.theme.VioletNova
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SiriLiquidOrb(
    mode: OrbVisualMode,
    audioLevel: Float, // 0.0f .. 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SiriOrbTransition")

    // Dynamic rotation of colors
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Gentle breathing scale
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    val targetScale = when (mode) {
        OrbVisualMode.IDLE -> breathScale
        OrbVisualMode.WAKE_DETECTED -> 1.25f
        OrbVisualMode.LISTENING -> 1.08f + (audioLevel * 0.40f)
        OrbVisualMode.PROCESSING -> 1.12f + (breathScale * 0.05f)
        OrbVisualMode.SPEAKING -> 1.06f + (audioLevel * 0.30f)
        OrbVisualMode.ERROR -> 0.95f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 320f),
        label = "SiriOrbScale"
    )

    val primaryColors = when (mode) {
        OrbVisualMode.IDLE -> listOf(ElectricBlue, VioletNova, CyanNova, PurpleNova)
        OrbVisualMode.WAKE_DETECTED -> listOf(ElectricBlue, MagentaNova, CyanNova, Color.White)
        OrbVisualMode.LISTENING -> listOf(CyanNova, ElectricBlue, MagentaNova, VioletNova)
        OrbVisualMode.PROCESSING -> listOf(MagentaNova, VioletNova, ElectricBlue, CyanNova)
        OrbVisualMode.SPEAKING -> listOf(ElectricBlue, MagentaNova, CyanNova, PurpleNova)
        OrbVisualMode.ERROR -> listOf(GlowRed, GlowAmber, MagentaNova)
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2.3f) * animatedScale

            // Outer atmospheric bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColors[0].copy(alpha = 0.50f),
                        primaryColors[1].copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.7f
                ),
                radius = baseRadius * 1.7f,
                center = center
            )

            // Fluid harmonic halo
            drawSiriHalo(
                center = center,
                baseRadius = baseRadius * 1.15f,
                angleOffset = rotationAngle,
                audioFactor = if (mode == OrbVisualMode.LISTENING || mode == OrbVisualMode.SPEAKING) audioLevel else 0.12f,
                colors = primaryColors
            )

            // Dynamic luminous core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColors[0].copy(alpha = 0.85f),
                        primaryColors[1].copy(alpha = 0.70f),
                        primaryColors[2].copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.18f),
                    radius = baseRadius * 1.05f
                ),
                radius = baseRadius,
                center = center
            )

            // Specular glass top reflection (Siri's signature shine)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.30f, center.y - baseRadius * 0.35f),
                    radius = baseRadius * 0.40f
                ),
                radius = baseRadius * 0.40f,
                center = Offset(center.x - baseRadius * 0.30f, center.y - baseRadius * 0.35f)
            )
        }
    }
}

private fun DrawScope.drawSiriHalo(
    center: Offset,
    baseRadius: Float,
    angleOffset: Float,
    audioFactor: Float,
    colors: List<Color>
) {
    val path = Path()
    val steps = 40
    val radOffset = (angleOffset * PI / 180f).toFloat()

    for (i in 0..steps) {
        val theta = (i * 2 * PI / steps).toFloat()
        val ripple = sin(theta * 3f + radOffset) * 0.08f + cos(theta * 2f - radOffset) * 0.06f
        val audioDeformation = (audioFactor * 0.30f * sin(theta * 5f + radOffset))
        val r = baseRadius * (1.0f + ripple + audioDeformation)

        val x = center.x + r * cos(theta)
        val y = center.y + r * sin(theta)

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(
        path = path,
        brush = Brush.sweepGradient(
            colors = colors + colors.first(),
            center = center
        ),
        alpha = 0.65f
    )
}
