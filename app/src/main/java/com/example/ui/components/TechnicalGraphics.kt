package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DotMatrixFont
import com.example.ui.theme.NothingRed

/**
 * Nothing OS 4-dot signature brand logo (2x2 grid).
 * Top-left, top-right, and bottom-left dots use contrasting color,
 * while the bottom-right dot is the signature Nothing Red (#E51C23).
 */
@Composable
fun NothingLogo4Dot(
    modifier: Modifier = Modifier,
    boxSize: Dp = 38.dp,
    dotSize: Dp = 4.5.dp,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color.Transparent,
    contrastDotColor: Color = Color.White
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(boxSize)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(dotSize).background(contrastDotColor, CircleShape))
                Box(modifier = Modifier.size(dotSize).background(contrastDotColor, CircleShape))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(dotSize).background(contrastDotColor, CircleShape))
                Box(modifier = Modifier.size(dotSize).background(NothingRed, CircleShape))
            }
        }
    }
}

/**
 * Subtle technical blueprint graphic drawn via Canvas:
 * Features concentric circles, dashed technical arcs, crosshair ticks,
 * and aesthetic geometry matching Nothing OS industrial design.
 */
@Composable
fun TechnicalCircleGraphic(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.12f),
    accentColor: Color = NothingRed.copy(alpha = 0.35f)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f

        // Outer dashed circular ring
        drawCircle(
            color = color,
            radius = radius * 0.95f,
            center = Offset(centerX, centerY),
            style = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
            )
        )

        // Middle solid fine circle
        drawCircle(
            color = color.copy(alpha = color.alpha * 0.8f),
            radius = radius * 0.65f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1f)
        )

        // Inner dotted fine circle
        drawCircle(
            color = color,
            radius = radius * 0.35f,
            center = Offset(centerX, centerY),
            style = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f)
            )
        )

        // Technical crosshair ticks
        val tickLen = radius * 0.15f
        // Top tick
        drawLine(color = color, start = Offset(centerX, 0f), end = Offset(centerX, tickLen), strokeWidth = 1f)
        // Bottom tick
        drawLine(color = color, start = Offset(centerX, size.height - tickLen), end = Offset(centerX, size.height), strokeWidth = 1f)
        // Left tick
        drawLine(color = color, start = Offset(0f, centerY), end = Offset(tickLen, centerY), strokeWidth = 1f)
        // Right tick (with subtle red accent)
        drawLine(color = accentColor, start = Offset(size.width - tickLen, centerY), end = Offset(size.width, centerY), strokeWidth = 1.5f)
    }
}

/**
 * Reusable Section Header formatted in Nothing OS Dot-Matrix typography with a signature red dot.
 */
@Composable
fun NothingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    textColor: Color = Color.White,
    secondaryColor: Color = Color(0xFF8E8E93),
    fontSize: Int = 18
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title.uppercase(),
                fontFamily = DotMatrixFont,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                letterSpacing = 2.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "•",
                color = NothingRed,
                fontSize = (fontSize + 2).sp,
                fontWeight = FontWeight.Black
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = secondaryColor,
                letterSpacing = 0.3.sp
            )
        }
    }
}
