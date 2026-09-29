package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.model.AssistantState
import com.example.ui.theme.CyberAccentNeon
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberElectricBlue
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberWarning
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated circular voice indicator inspired by futuristic cybernetic reactors.
 * Responds distinctly to Idle, Listening, Thinking, Speaking, and Error states.
 */
@Composable
fun JarvisArcReactor(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_reactor")

    // Slow rotation for idle/speaking
    val slowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_rotation"
    )

    // Fast rotation for thinking state
    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_rotation"
    )

    // Idle breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // Speaking sonic wave pulse
    val speakingPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_pulse"
    )

    // Smooth transition for audio level reactivity
    val audioAnim = remember { Animatable(0f) }
    LaunchedEffect(state) {
        val targetAudio = when (state) {
            is AssistantState.Listening -> state.audioLevel
            is AssistantState.Speaking -> state.audioLevel
            else -> 0f
        }
        audioAnim.animateTo(
            targetValue = targetAudio,
            animationSpec = tween(durationMillis = 80)
        )
    }

    val primaryColor = when (state) {
        is AssistantState.Idle -> CyberCyan
        is AssistantState.Listening -> CyberAccentNeon
        is AssistantState.Thinking -> CyberElectricBlue
        is AssistantState.Speaking -> CyberCyanGlow
        is AssistantState.Error -> CyberError
    }

    val secondaryColor = when (state) {
        is AssistantState.Idle -> CyberElectricBlue
        is AssistantState.Listening -> CyberCyan
        is AssistantState.Thinking -> CyberCyan
        is AssistantState.Speaking -> CyberAccentNeon
        is AssistantState.Error -> CyberWarning
    }

    val currentRotation = when (state) {
        is AssistantState.Thinking -> fastRotation
        else -> slowRotation
    }

    val dynamicScale = when (state) {
        is AssistantState.Idle -> breathingPulse
        is AssistantState.Listening -> 1.0f + (audioAnim.value * 0.28f)
        is AssistantState.Thinking -> 1.02f
        is AssistantState.Speaking -> speakingPulse + (audioAnim.value * 0.15f)
        is AssistantState.Error -> 0.98f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.82f

            // 1. Outermost Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f * dynamicScale),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f * dynamicScale
                ),
                radius = baseRadius * 1.35f * dynamicScale,
                center = center
            )

            // 2. Outer Orbiting Ring with Tech Ticks
            rotate(currentRotation, pivot = center) {
                drawOuterTicks(
                    center = center,
                    radius = baseRadius * 1.05f * dynamicScale,
                    color = primaryColor.copy(alpha = 0.65f),
                    state = state
                )
            }

            // 3. Counter-rotating segmented arcs
            rotate(-currentRotation * 1.4f, pivot = center) {
                drawSegmentedArcs(
                    center = center,
                    radius = baseRadius * 0.88f * dynamicScale,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    state = state
                )
            }

            // 4. Middle Concentric Halo Ring
            drawCircle(
                color = secondaryColor.copy(alpha = 0.45f),
                radius = baseRadius * 0.72f * dynamicScale,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 5. Reactive Audio Visualizer Wave (Active when listening or speaking)
            if (state is AssistantState.Listening || state is AssistantState.Speaking) {
                val waveAlpha = (0.35f + audioAnim.value * 0.5f).coerceIn(0f, 1f)
                drawCircle(
                    color = primaryColor.copy(alpha = waveAlpha),
                    radius = baseRadius * (0.55f + audioAnim.value * 0.35f),
                    center = center,
                    style = Stroke(width = 3.5f)
                )
            }

            // 6. Glowing Inner Reactor Core
            val coreRadius = baseRadius * 0.38f * dynamicScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 7. Core Perimeter Ring with Crisp Accent
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = coreRadius * 0.65f,
                center = center,
                style = Stroke(width = 2.0f)
            )

            // 8. Core Center Cyber Dot
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.25f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawOuterTicks(
    center: Offset,
    radius: Float,
    color: Color,
    state: AssistantState
) {
    val tickCount = 36
    val tickLength = if (state is AssistantState.Listening) 12f else 8f
    val strokeWidth = if (state is AssistantState.Thinking) 3f else 2f

    for (i in 0 until tickCount) {
        val angle = Math.toRadians((i * (360f / tickCount)).toDouble())
        val isMajor = i % 4 == 0
        val length = if (isMajor) tickLength * 1.5f else tickLength

        val startX = (center.x + (radius - length) * cos(angle)).toFloat()
        val startY = (center.y + (radius - length) * sin(angle)).toFloat()
        val endX = (center.x + radius * cos(angle)).toFloat()
        val endY = (center.y + radius * sin(angle)).toFloat()

        drawLine(
            color = if (isMajor) color else color.copy(alpha = 0.3f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) strokeWidth * 1.5f else strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSegmentedArcs(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    state: AssistantState
) {
    val strokeWidth = if (state is AssistantState.Thinking) 4.5f else 3.0f
    val arcSize = Size(radius * 2f, radius * 2f)
    val arcTopLeft = Offset(center.x - radius, center.y - radius)

    // Segment 1
    drawArc(
        color = primaryColor,
        startAngle = 10f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Segment 2
    drawArc(
        color = secondaryColor,
        startAngle = 100f,
        sweepAngle = 50f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Segment 3
    drawArc(
        color = primaryColor,
        startAngle = 170f,
        sweepAngle = 80f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Segment 4
    drawArc(
        color = secondaryColor,
        startAngle = 270f,
        sweepAngle = 65f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
