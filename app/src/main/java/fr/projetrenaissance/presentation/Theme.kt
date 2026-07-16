package fr.projetrenaissance.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF102A43)
val Sage = Color(0xFF819B83)
val Copper = Color(0xFFB7653E)
val WarmBackground = Color(0xFFF7F4EE)
val Ink = Color(0xFF17232D)
val DeepNavy = Color(0xFF0B2135)
val SoftSage = Color(0xFFE8EFE8)
val PaleCopper = Color(0xFFF3E3D9)
val Paper = Color(0xFFFFFBF6)
val SoftGray = Color(0xFF6B747B)
val WarmLine = Color(0xFFDED8CE)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Sage,
    onSecondary = Color.White,
    tertiary = Copper,
    onTertiary = Color.White,
    background = WarmBackground,
    onBackground = Ink,
    surface = Color(0xFFFFFBF6),
    onSurface = Ink,
    error = Color(0xFF9F2D2D),
)

private val RenaissanceTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.1.sp),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAC7E3),
    secondary = Color(0xFFAFC8B0),
    tertiary = Color(0xFFFFB494),
    background = Color(0xFF0C2033),
    surface = Color(0xFF132D42),
)

@Composable
fun RenaissanceTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RenaissanceTypography,
        content = content,
    )
}
