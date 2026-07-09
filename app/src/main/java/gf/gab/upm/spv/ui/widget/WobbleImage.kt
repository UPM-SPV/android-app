package gf.gab.upm.spv.ui.widget

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import gf.gab.upm.spv.ui.shader.rememberAnimationTimer
import kotlin.math.sin

@Composable
fun WobbleIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,

    rotXSpeed: Double = 2.0,
    rotYSpeed: Double = 1.5,
    rotZSpeed: Double = 1.25,

    rotXMagnitude: Double = 15.0,
    rotYMagnitude: Double = 15.0,
    rotZMagnitude: Double = 1.5,

    graphicsModifier: GraphicsLayerScope.() -> Unit = {},
    tint: Color = LocalContentColor.current
) {
    val modifier = modifier.rememberWobble(
        modifier,
        rotXSpeed, rotYSpeed, rotZSpeed,
        rotXMagnitude, rotYMagnitude, rotZMagnitude,
        graphicsModifier
    )

    Icon(
        imageVector,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun WobbleIcon(
    modifier: Modifier = Modifier,
    painter: Painter,

    rotXSpeed: Double = 2.0,
    rotYSpeed: Double = 1.5,
    rotZSpeed: Double = 1.25,

    rotXMagnitude: Double = 15.0,
    rotYMagnitude: Double = 15.0,
    rotZMagnitude: Double = 1.5,

    graphicsModifier: GraphicsLayerScope.() -> Unit = {},
    tint: Color = LocalContentColor.current
) {
    val modifier = modifier.rememberWobble(
        modifier,
        rotXSpeed, rotYSpeed, rotZSpeed,
        rotXMagnitude, rotYMagnitude, rotZMagnitude,
        graphicsModifier
    )

    Icon(
        painter,
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
private fun Modifier.rememberWobble(
    modifier: Modifier = Modifier,

    rotXSpeed: Double,
    rotYSpeed: Double,
    rotZSpeed: Double,

    rotXMagnitude: Double,
    rotYMagnitude: Double,
    rotZMagnitude: Double,

    graphicsModifier: GraphicsLayerScope.() -> Unit,
): Modifier = composed {
    val animationCounter by rememberAnimationTimer()
    this.graphicsLayer {
            // Balatro-like wobble
            rotationX = (sin(animationCounter * rotXSpeed) * rotXMagnitude).toFloat()
            rotationY = (sin(animationCounter * rotYSpeed) * rotYMagnitude).toFloat()
            rotationZ = (sin(animationCounter * rotZSpeed) * rotZMagnitude).toFloat()
            graphicsModifier()
        }
        .then(modifier)
}
