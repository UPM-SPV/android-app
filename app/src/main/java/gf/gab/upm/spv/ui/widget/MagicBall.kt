package gf.gab.upm.spv.ui.widget

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gf.gab.upm.spv.ui.shader.animateColorListAsState
import gf.gab.upm.spv.ui.shader.rememberAnimationTimer
import gf.gab.upm.spv.ui.shader.rememberFbmContext
import kotlin.math.sin

@Composable
fun MagicBall(
    modifier: Modifier = Modifier,

    ballProgress: Float = 0.0f,
    ringProgress: Float = 0.0f,

    progressAnimation: AnimationSpec<Float> = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    parameterAnimation: AnimationSpec<Float> = tween(durationMillis = 1000, easing = EaseInOut),
    colorAnimation: AnimationSpec<Color> = tween(durationMillis = 1000, easing = EaseInOut),

    foregroundWave: WaveParameters = WaveParameters(),
    backgroundWave: WaveParameters = WaveParameters(),
    ringParameters: RingParameters = RingParameters(),

    contents: @Composable () -> Unit = {}
) {
    val animatedFluidProgress by animateFloatAsState(
        targetValue = when(ballProgress) { // Completely fill/empty the ball.
            1.0f -> 1000f
            0.0f -> -1000f
            else -> ballProgress
        },
        animationSpec = progressAnimation,
    )
    val animatedRingProgress by animateFloatAsState(
        targetValue = ringProgress,
        animationSpec = progressAnimation
    )

    val foregroundWave = animateWaveParameters(foregroundWave, parameterAnimation, colorAnimation)
    val backgroundWave = animateWaveParameters(backgroundWave, parameterAnimation, colorAnimation)
    val ringParameters = animateRingParameters(ringParameters, parameterAnimation, colorAnimation)

    val timer by rememberAnimationTimer()
    val foregroundFbm = rememberFbmContext(foregroundWave.palette)
    val backgroundFbm = rememberFbmContext(backgroundWave.palette)
    val ringFbm = rememberFbmContext(ringParameters.palette)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                onDrawBehind {
                    val center = Offset(size.width / 2f,  size.height / 2f)

                    // Magic ball fluid.
                    val fluidLevel = size.height * (1f - animatedFluidProgress)

                    val foregroundPath = Path()
                    val backgroundPath = Path()

                    foregroundPath.moveTo(0f, size.height)
                    backgroundPath.moveTo(0f, size.height)

                    for(x in 0..size.width.toInt()) {
                        val foregroundY = fluidLevel + sin(
                            x * foregroundWave.frequency * 2 * Math.PI / size.width + timer * foregroundWave.speed
                        ).toFloat() * foregroundWave.amplitude
                        val backgroundY = fluidLevel + sin(
                            -x * backgroundWave.frequency * 2 * Math.PI / size.width - timer * backgroundWave.speed
                        ).toFloat() * backgroundWave.amplitude

                        if(x == 0) {
                            foregroundPath.moveTo(0f, foregroundY)
                            backgroundPath.moveTo(0f, backgroundY)
                        } else {
                            foregroundPath.lineTo(x.toFloat(), foregroundY)
                            backgroundPath.lineTo(x.toFloat(), backgroundY)
                        }
                    }

                    foregroundPath.lineTo(size.width, size.height)
                    backgroundPath.lineTo(size.width, size.height)
                    foregroundPath.lineTo(0f, size.height)
                    backgroundPath.lineTo(0f, size.height)

                    foregroundPath.close()
                    backgroundPath.close()

                    with(backgroundFbm) {
                        drawFbmPath(backgroundPath, timer.toFloat())
                    }
                    with(foregroundFbm) {
                        drawFbmPath(foregroundPath, timer.toFloat())
                    }

                    // Outer secondary ring.
                    val ringRadius = size.width / 2f - ringParameters.thickness.toPx() / 2f
                    val ringBoundingBox = Rect(center = center, radius = ringRadius)

                    with(ringFbm) {
                        drawFbmArc(
                            time = timer.toFloat(),
                            startAngle = -90f,
                            sweepAngle = animatedRingProgress * 360f,
                            useCenter = false,
                            style = Stroke(
                                width = ringParameters.thickness.toPx(),
                                cap = StrokeCap.Round
                            ),
                            topLeft = ringBoundingBox.topLeft,
                            size = ringBoundingBox.size
                        )
                    }
                }
            }
    ) {
        contents()
    }
}

data class WaveParameters(
    val frequency: Float = 1.0f,
    val amplitude: Float = 1.0f,
    val speed: Float = 1.0f,
    val palette: List<Color> = listOf(Color.White)
)

data class RingParameters(
    val thickness: Dp = 1.dp,
    val palette: List<Color> = listOf(Color.White)
)

@Composable
private fun animateWaveParameters(
    target: WaveParameters,
    animationSpec: AnimationSpec<Float>,
    colorAnimationSpec: AnimationSpec<Color>,
): WaveParameters {
    return WaveParameters(
        frequency = animateFloatAsState(target.frequency, animationSpec).value,
        amplitude = animateFloatAsState(target.amplitude, animationSpec).value,
        speed = animateFloatAsState(target.speed, animationSpec).value,
        palette = animateColorListAsState(target.palette, colorAnimationSpec)
    )
}

@Composable
private fun animateRingParameters(
    target: RingParameters,
    animationSpec: AnimationSpec<Float>,
    colorAnimationSpec: AnimationSpec<Color>,
): RingParameters {
    return RingParameters(
        thickness = animateFloatAsState(target.thickness.value, animationSpec).value.dp,
        palette = animateColorListAsState(target.palette, colorAnimationSpec)
    )
}
