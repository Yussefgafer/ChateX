package com.kai.ghostmesh.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.spectralGlow(
    color: Color,
    radius: Dp = 12.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp)
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    this.drawBehind {
        val frameworkColor = color.copy(alpha = alpha).toArgb()
        val radiusPx = radius.toPx()
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    radiusPx,
                    0f, 0f,
                    frameworkColor
                )
            }
            
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                shape.topStart.toPx(size, this),
                shape.topStart.toPx(size, this),
                paint
            )
        }
    }
}
