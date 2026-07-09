package gf.gab.upm.spv.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WaveCircles(
    modifier: Modifier = Modifier,
    circleColor: Color = Color(0xFF3F51B5),
    maxRadius: Dp = 120.dp,
    minRadius: Dp = 64.dp,
    waveCount: Int = 10,
    animationDuration: Int = 3000,

    alphaStart: Float = 0.2f,
    alphaEnd: Float = 0.8f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier.size(maxRadius * 2),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = modifier.size(maxRadius * 2)) {
            val center = size / 2f

            val minRadiusPx = minRadius.toPx()
            val baseMaxRadiusPx = maxRadius.toPx()

            for (i in 0 until waveCount) {
                val waveDelay = i.toFloat() / waveCount
                var progress = animationProgress - waveDelay
                if (progress < 0f) progress += 1f

                val currentRadius = minRadiusPx + (baseMaxRadiusPx - minRadiusPx) * progress
                val alpha = when {
                    progress < alphaStart -> progress / alphaStart
                    else -> (1f - progress) / alphaEnd
                }

                drawCircle(
                    color = circleColor,
                    radius = currentRadius,
                    center = Offset(center.width, center.height),
                    alpha = alpha.coerceIn(0f, 1f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
