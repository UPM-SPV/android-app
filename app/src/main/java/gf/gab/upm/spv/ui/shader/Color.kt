package gf.gab.upm.spv.ui.shader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun animateColorListAsState(
    targetValue: List<Color>,
    animationSpec: AnimationSpec<Color>
): List<Color> {
    return targetValue.map { animateColorAsState(it, animationSpec).value }
}
