package gf.gab.upm.spv.ui.shader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DoubleState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlin.math.abs
import kotlin.random.Random

private val RANDOM_RANGE = -32768..32768

@Composable
fun rememberAnimationTimer(
    startValue: Double = 0.0,
    startRandom: Boolean = true,
): DoubleState {
    val animationTimer = remember {
        if(startRandom) {
            val range = abs(RANDOM_RANGE.first) + abs(RANDOM_RANGE.last)
            mutableDoubleStateOf(Random.nextDouble() * range - abs(RANDOM_RANGE.first))
        } else {
            mutableDoubleStateOf(startValue)
        }
    }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it } - animationTimer.doubleValue * 1000.0
        while (true) {
            withFrameMillis { frameTime ->
                animationTimer.doubleValue = (frameTime - startTime) / 1000.0
            }
        }
    }

    return animationTimer
}
