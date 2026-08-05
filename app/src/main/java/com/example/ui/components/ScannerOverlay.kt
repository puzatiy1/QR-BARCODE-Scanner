package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    cornerColor: Color = Color(0xFF0EA5E9), // Sky Blue Accent
    laserColor: Color = Color(0xFF10B981)   // Emerald Green Laser
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserYFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y_position"
    )

    val cornerLen = 24.dp
    val strokeWidth = 4.dp
    val density = LocalDensity.current

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Scanner guide box is a square centered on screen (about 70% of narrower dimension)
        val boxWidth = width.coerceAtMost(height) * 0.72f
        val boxHeight = boxWidth

        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2
        val right = left + boxWidth
        val bottom = top + boxHeight

        val cornerLenPx = with(density) { cornerLen.toPx() }
        val strokeWidthPx = with(density) { strokeWidth.toPx() }

        // 1. Draw Translucent Dark Mask outside scanning area
        // Top banner
        drawRect(Color.Black.copy(alpha = 0.55f), size = Size(width, top))
        // Bottom banner
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, bottom), size = Size(width, height - bottom))
        // Left column
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, top), size = Size(left, boxHeight))
        // Right column
        drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(right, top), size = Size(width - right, boxHeight))

        // 2. Draw Outer Box Guide Frame (thin border)
        drawRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 1.dp.toPx() })
        )

        // 3. Draw Heavy Neon Corner brackets
        val offsetHalf = strokeWidthPx / 2f

        // Top Left Corner
        drawLine(
            color = cornerColor,
            start = Offset(left - offsetHalf, top),
            end = Offset(left + cornerLenPx, top),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top - offsetHalf),
            end = Offset(left, top + cornerLenPx),
            strokeWidth = strokeWidthPx
        )

        // Top Right Corner
        drawLine(
            color = cornerColor,
            start = Offset(right + offsetHalf, top),
            end = Offset(right - cornerLenPx, top),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, top - offsetHalf),
            end = Offset(right, top + cornerLenPx),
            strokeWidth = strokeWidthPx
        )

        // Bottom Left Corner
        drawLine(
            color = cornerColor,
            start = Offset(left - offsetHalf, bottom),
            end = Offset(left + cornerLenPx, bottom),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, bottom + offsetHalf),
            end = Offset(left, bottom - cornerLenPx),
            strokeWidth = strokeWidthPx
        )

        // Bottom Right Corner
        drawLine(
            color = cornerColor,
            start = Offset(right + offsetHalf, bottom),
            end = Offset(right - cornerLenPx, bottom),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, bottom + offsetHalf),
            end = Offset(right, bottom - cornerLenPx),
            strokeWidth = strokeWidthPx
        )

        // 4. Draw Animated Pulsing Laser Scanner Line
        val laserY = top + (boxHeight * laserYFraction)
        drawLine(
            color = laserColor,
            start = Offset(left + with(density) { 8.dp.toPx() }, laserY),
            end = Offset(right - with(density) { 8.dp.toPx() }, laserY),
            strokeWidth = with(density) { 3.dp.toPx() }
        )
    }
}
