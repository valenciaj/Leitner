package es.leitner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.leitner.app.R
import es.leitner.app.data.ThemeChoice
import es.leitner.core.Mode

// ---- Colores: modo Clásico (índigo) ----

private val NormalLight = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF1A2378),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF2D1228),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF1B1B24),
    surface = Color(0xFFF6F6FB),
    onSurface = Color(0xFF1B1B24),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F0F7),
    surfaceContainer = Color(0xFFEBEBF2),
    surfaceContainerHigh = Color(0xFFE5E5EC),
    surfaceContainerHighest = Color(0xFFE0E0E7),
)
private val NormalDark = darkColorScheme(
    primary = Color(0xFFBCC2FF),
    onPrimary = Color(0xFF0F1A6B),
    primaryContainer = Color(0xFF2B3A97),
    onPrimaryContainer = Color(0xFFDFE0FF),
    secondary = Color(0xFFC4C4DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E0F9),
    tertiary = Color(0xFFE6BAD7),
    onTertiary = Color(0xFF44263D),
    tertiaryContainer = Color(0xFF5D3C55),
    onTertiaryContainer = Color(0xFFFFD7F1),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)

// ---- Colores: modo Peques (naranja, cian y amarillo) ----

private val KidsLight = lightColorScheme(
    primary = Color(0xFFD9480F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF00707F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F5FA),
    onSecondaryContainer = Color(0xFF00363E),
    tertiary = Color(0xFF7A5300),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFFFF6E5),
    onBackground = Color(0xFF2B1A0E),
    surface = Color(0xFFFFF6E5),
    onSurface = Color(0xFF2B1A0E),
    surfaceVariant = Color(0xFFF5DECF),
    onSurfaceVariant = Color(0xFF53433A),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFE0C4B3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1DD),
    surfaceContainer = Color(0xFFFBEBD5),
    surfaceContainerHigh = Color(0xFFF5E5CF),
    surfaceContainerHighest = Color(0xFFEFDFC9),
)
private val KidsDark = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF581E00),
    primaryContainer = Color(0xFF7C2E00),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFF82D3E0),
    onSecondary = Color(0xFF00363E),
    secondaryContainer = Color(0xFF004F59),
    onSecondaryContainer = Color(0xFFA2EFFD),
    tertiary = Color(0xFFF5BF48),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4200),
    onTertiaryContainer = Color(0xFFFFDEA6),
    background = Color(0xFF1D140E),
    onBackground = Color(0xFFF1DFD6),
    surface = Color(0xFF1D140E),
    onSurface = Color(0xFFF1DFD6),
    surfaceVariant = Color(0xFF53433A),
    onSurfaceVariant = Color(0xFFD8C2B6),
    outline = Color(0xFFA08D83),
    outlineVariant = Color(0xFF53433A),
    surfaceContainerLowest = Color(0xFF170F09),
    surfaceContainerLow = Color(0xFF261C15),
    surfaceContainer = Color(0xFF2A2019),
    surfaceContainerHigh = Color(0xFF352A23),
    surfaceContainerHighest = Color(0xFF40352D),
)

// ---- Tipografía: Inter (Clásico) y Nunito (Peques), fuentes variables ----

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(res: Int) = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private val Inter = variableFamily(R.font.inter)
private val Nunito = variableFamily(R.font.nunito)

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

private fun normalTypography(): Typography {
    val base = Typography().withFamily(Inter)
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** En modo niños, todos los textos son más grandes y redondeados. */
private fun kidsTypography(): Typography {
    val base = Typography().withFamily(Nunito)
    return base.copy(
        headlineMedium = base.headlineMedium.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
    )
}

// ---- Formas: esquinas más redondeadas que las de serie ----

private val LeitnerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tema elegido en Ajustes; lo proporciona MainActivity. */
val LocalThemeChoice = staticCompositionLocalOf { ThemeChoice.SYSTEM }

@Composable
fun LeitnerTheme(mode: Mode? = null, content: @Composable () -> Unit) {
    val dark = when (LocalThemeChoice.current) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val colors = when (mode) {
        Mode.KIDS -> if (dark) KidsDark else KidsLight
        else -> if (dark) NormalDark else NormalLight
    }
    val typography = remember(mode) { if (mode == Mode.KIDS) kidsTypography() else normalTypography() }
    MaterialTheme(colorScheme = colors, typography = typography, shapes = LeitnerShapes, content = content)
}
