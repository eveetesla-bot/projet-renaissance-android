package fr.projetrenaissance.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Renaissance 2.0 — système visuel refondu.
//
// Palette « athletic clean » : indigo profond (marque), vert-sarcelle
// (secondaire), ambre brûlé (accent) posés sur des surfaces claires et
// froides. Chaque couleur de texte est choisie pour un contraste AA sur son
// fond d'usage ; les cartes associent toujours un fond à sa couleur de texte
// (voir DesignSystem.PremiumSurfaceCard), ce qui rend tout « texte sombre sur
// fond sombre » structurellement impossible.
//
// Les noms de tokens sont conservés pour rester compatibles avec l'ensemble
// des écrans ; seules leurs valeurs et leur mise en forme changent.
// ---------------------------------------------------------------------------

// Marque et textes — identité « salle de sport » : encre quasi-noire + orange
// énergique + sarcelle santé. Fort contraste, lecture immédiate.
val Navy = Color(0xFF17191F)        // anthracite : gros titres, valeurs, marque
val DeepNavy = Color(0xFF101216)    // quasi-noir : cartes « hero » / titres sur clair
val Ink = Color(0xFF1C1F26)         // texte courant
val OnNavy = Color(0xFFF2F4F8)      // texte clair sur fond sombre

// Neutres
val SoftGray = Color(0xFF5B6472)    // légendes, texte atténué
val WarmLine = Color(0xFFE2E5EC)    // filets, séparateurs, pistes de jauge

// Accents
val Copper = Color(0xFFC9450F)      // orange énergie : action, intitulés, CTA
val Sage = Color(0xFF127A63)        // sarcelle : santé, intitulés secondaires

// Surfaces
val WarmBackground = Color(0xFFF4F5F8)  // fond d'application
val Paper = Color(0xFFFFFFFF)           // cartes neutres
val SoftSage = Color(0xFFE2F0EC)        // carte teintée sarcelle
val PaleCopper = Color(0xFFFCE7DC)      // carte teintée orange

private val LightColors = lightColorScheme(
    primary = Copper,
    onPrimary = Color.White,
    primaryContainer = PaleCopper,
    onPrimaryContainer = Color(0xFF4A1806),
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = SoftSage,
    onSecondaryContainer = Color(0xFF073B30),
    tertiary = Navy,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE0E8),
    onTertiaryContainer = Color(0xFF14161C),
    background = WarmBackground,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9ECF2),
    onSurfaceVariant = SoftGray,
    outline = Color(0xFFB7BEC9),
    outlineVariant = WarmLine,
    error = Color(0xFFC0392B),
    onError = Color.White,
    errorContainer = Color(0xFFFBE0DC),
    onErrorContainer = Color(0xFF4A120C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC0FF),
    onPrimary = Color(0xFF10204F),
    primaryContainer = Color(0xFF2C3D8F),
    onPrimaryContainer = Color(0xFFDFE4FB),
    secondary = Color(0xFF74D6BF),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF11413A),
    onSecondaryContainer = Color(0xFFC7EFE6),
    tertiary = Color(0xFFFFB182),
    onTertiary = Color(0xFF4A1E06),
    tertiaryContainer = Color(0xFF6A3413),
    onTertiaryContainer = Color(0xFFFBE6D9),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF161A21),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF2A303B),
    onSurfaceVariant = Color(0xFFC2C9D6),
    outline = Color(0xFF48505E),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF561A12),
)

private val Sans = FontFamily.SansSerif

private val RenaissanceTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

private val RenaissanceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun RenaissanceTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RenaissanceTypography,
        shapes = RenaissanceShapes,
        content = content,
    )
}
