package gf.gab.upm.spv.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun AnimatedSpinnerCheckmark(
    modifier: Modifier = Modifier,
    state: AnimatedCheckmarkState = AnimatedCheckmarkState.Complete,
    spinnerColor: Color = MaterialTheme.colorScheme.secondary,
    successColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {

    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
    ) { state ->
        when(state) {
            AnimatedCheckmarkState.Inactive -> {}
            AnimatedCheckmarkState.Spinning -> CircularProgressIndicator(
                                                    modifier = modifier,
                                                    color = spinnerColor,
                                                    strokeWidth = strokeWidth,
                                                )
            AnimatedCheckmarkState.Complete ->  Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = successColor
                                                )
        }
    }
}

enum class AnimatedCheckmarkState {
    Inactive, Spinning, Complete
}
