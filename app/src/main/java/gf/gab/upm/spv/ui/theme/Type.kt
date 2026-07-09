package gf.gab.upm.spv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gf.gab.upm.spv.R

private val fontFamily = FontFamily(
    Font(R.font.plusjakartasans_regular, FontWeight.Light),
     Font(R.font.plusjakartasans_regular, FontWeight.Normal),
    Font(R.font.plusjakartasans_regular, FontWeight.Medium),
    Font(R.font.plusjakartasans_regular, FontWeight.Bold),

    Font(R.font.plusjakartasans_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.plusjakartasans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plusjakartasans_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.plusjakartasans_italic, FontWeight.Bold, FontStyle.Italic),

)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
