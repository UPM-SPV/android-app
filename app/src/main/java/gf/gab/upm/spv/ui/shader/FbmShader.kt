package gf.gab.upm.spv.ui.shader

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

@Language("AGSL")
private val FBM_SHADER = """
    uniform vec2 u_resolution;
    uniform float u_time;
    uniform shader u_colors;
    uniform float u_colors_count;
    // NOTE:
    //
    // This code uses string-replaced constants to allow for constant expansion and loop unrolling.
    
    // Sine fractional hash function.
    // (Mostly) depends on hardware precision.
    float rand(vec2 n) {
        // Calculate a big number and take the unpredictable fractional part.
        return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
    }
    
    // 2D fractal value noise
    float noise(vec2 p){
        vec2 ip = floor(p);
        vec2 u = fract(p);
        u = u*u*(3.0-2.0*u);

        float res = mix(
            mix(rand(ip),rand(ip+vec2(1.0,0.0)),u.x),
            mix(rand(ip+vec2(0.0,1.0)),rand(ip+vec2(1.0,1.0)),u.x),u.y);
        return res*res;
    }
    
    // The fractal brownian motion code itself.
    // Adds together multiple layers of noise. 
    float fbm(vec2 x) {
        float v = 0.0;
        float a = {{NOISE_AMPLITUDE}};
        vec2 shift = vec2({{NOISE_SHIFT}});
        // Rotate to reduce axial bias
        mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.50));
        for (int i = 0; i < {{NUM_OCTAVES}}; ++i) {
            v += a * noise(x);
            x = rot * x * {{NOISE_FREQUENCY}} + shift;
            a *= 0.5;
        }
        return v;
    }

    vec4 main(float2 fragCoord) {
        vec2 st = fragCoord.xy/u_resolution.xy;
        st.x *= u_resolution.x/u_resolution.y;
        vec2 scaled_pos = st * {{POSITION_SCALE}};
        float scaled_time = u_time * 0.5;
        

        vec2 q;
        q.x = fbm(scaled_pos + scaled_time * -0.8);
        q.y = fbm(scaled_pos + scaled_time * 0.8);
        
        vec2 p;
        p.x = fbm(scaled_pos + scaled_time * 1.6);
        p.y = fbm(scaled_pos + scaled_time * -1.6);
        
        float mix_point = clamp(length(q + p), 0.0, u_colors_count - 1.0);
        float color_a_index = floor(mix_point);
        float color_b_index = color_a_index + 1;
        half4 color_a = u_colors.eval(vec2(color_a_index, 0.5));
        half4 color_b = u_colors.eval(vec2(color_b_index, 0.5));
        
        float interp = mix_point - color_a_index;
        half4 color = mix(color_a, color_b, interp);

        return color;
    }
""".trimIndent()

fun Modifier.drawWithFbmBackground(
    colors: List<Color>,
    timeStart: Double? = null,

    amplitude: Float = 1.0f,
    noiseShift: Float = 1.0f,
    fbmOctaves: Int = 1,
    noiseFrequency: Float = 1.0f,
    positionScale: Float = 1.0f,
): Modifier = composed {
    val (shader, colorBrush, animationCounter) = rememberShader(
        colors, timeStart,
        amplitude, noiseShift, fbmOctaves, noiseFrequency, positionScale
    )

    this.drawWithCache {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setInputBuffer("u_colors", colorBrush)
        shader.setFloatUniform("u_colors_count", colors.size.toFloat())

        val brush = ShaderBrush(shader)

        onDrawBehind {
            shader.setFloatUniform("u_time", animationCounter.toFloat())
            drawRect(brush = brush)
        }
    }
}

fun Modifier.drawWithFbmForeground(
    colors: List<Color>,
    timeStart: Double? = null,

    amplitude: Float = 1.0f,
    noiseShift: Float = 1.0f,
    fbmOctaves: Int = 1,
    noiseFrequency: Float = 1.0f,
    positionScale: Float = 1.0f,
): Modifier = composed {
    val (shader, colorBrush, animationCounter) = rememberShader(
        colors, timeStart,
        amplitude, noiseShift, fbmOctaves, noiseFrequency, positionScale
    )

    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithCache {
            shader.setFloatUniform("u_resolution", size.width, size.height)
            shader.setInputBuffer("u_colors", colorBrush)
            shader.setFloatUniform("u_colors_count", colors.size.toFloat())

            val brush = ShaderBrush(shader)

            onDrawWithContent {
                drawContent()

                shader.setFloatUniform("u_time", animationCounter.toFloat())
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.SrcIn
                )
            }
    }
}

@Composable
fun rememberFbmContext(
    colors: List<Color>,
    timeStart: Double? = null,

    amplitude: Float = 1.0f,
    noiseShift: Float = 1.0f,
    fbmOctaves: Int = 1,
    noiseFrequency: Float = 1.0f,
    positionScale: Float = 1.0f,
): FbmShaderContext {
    val (shader, colorBrush, _) = rememberShader(
        colors, timeStart,
        amplitude, noiseShift, fbmOctaves, noiseFrequency, positionScale
    )

    return FbmShaderContext(shader, colors.size, colorBrush)
}

class FbmShaderContext(
    val shader: RuntimeShader,
    val colorCount: Int,
    val colorBrush: BitmapShader,
) {

    private val brush = ShaderBrush(shader)

    fun DrawScope.drawFbmPath(
        path: Path,
        time: Float,
        blendMode: BlendMode = BlendMode.SrcOver
    ) {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setInputBuffer("u_colors", colorBrush)
        shader.setFloatUniform("u_colors_count", colorCount.toFloat())
        shader.setFloatUniform("u_time", time)

        drawPath(
            path = path,
            brush = brush,
            blendMode = blendMode
        )
    }

    fun DrawScope.drawFbmArc(
        time: Float,

        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean = false,
        style: Stroke,
        topLeft: Offset,
        size: Size,
    ) {
        shader.setFloatUniform("u_resolution", size.width, size.height)
        shader.setInputBuffer("u_colors", colorBrush)
        shader.setFloatUniform("u_colors_count", colorCount.toFloat())
        shader.setFloatUniform("u_time", time)

        drawArc(
            startAngle = startAngle,
            brush = brush,
            sweepAngle = sweepAngle,
            useCenter = useCenter,
            style = style,
            topLeft = topLeft,
            size = size
        )
    }
}

@Composable
private fun rememberShader(
    colors: List<Color>,
    timeStart: Double? = null,

    amplitude: Float = 1.0f,
    noiseShift: Float = 1.0f,
    fbmOctaves: Int = 1,
    noiseFrequency: Float = 1.0f,
    positionScale: Float = 1.0f,
): Triple<RuntimeShader, BitmapShader, Double> {
    val shader = remember(amplitude, noiseShift, fbmOctaves, noiseFrequency, positionScale) {
        val replacements = mapOf(
            "NOISE_AMPLITUDE" to amplitude,
            "NOISE_SHIFT" to noiseShift,
            "NUM_OCTAVES" to fbmOctaves,
            "NOISE_FREQUENCY" to noiseFrequency,
            "POSITION_SCALE" to positionScale
        )
        val parameterizedShader = replacements.entries.fold(FBM_SHADER) { currentString, entry ->
            currentString.replace("{{" + entry.key + "}}", entry.value.toString())
        }
        RuntimeShader(parameterizedShader)
    }
    val colorBrush = rememberColorTexture(colors)
    val animationCounter by rememberAnimationTimer(startRandom = timeStart == null, startValue = timeStart ?: 0.0)

    return Triple(shader, colorBrush, animationCounter)
}

@Composable
private fun rememberColorTexture(
    colors: List<Color>
): BitmapShader {
    return remember(colors) {
        val width = colors.size
        val height = 1

        val pixels = IntArray(width) { index -> colors[index].toArgb() }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
}
