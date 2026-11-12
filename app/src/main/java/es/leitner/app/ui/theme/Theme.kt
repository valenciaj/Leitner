package es.leitner.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import es.leitner.core.Mode

private val NormalLight = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF5C6BC0),
)
private val NormalDark = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF7986CB),
)

/** Colores vivos para el modo niños. */
private val KidsLight = lightColorScheme(
    primary = Color(0xFFFF7043),
    secondary = Color(0xFF26C6DA),
    tertiary = Color(0xFFFFCA28),
    background = Color(0xFFFFF8E1),
    surface = Color(0xFFFFF8E1),
)
private val KidsDark = darkColorScheme(
    primary = Color(0xFFFFAB91),
    secondary = Color(0xFF80DEEA),
    tertiary = Color(0xFFFFE082),
)

/** En modo niños, todos los textos son más grandes. */
private fun kidsTypography(base: Typography) = base.copy(
    titleLarge = base.titleLarge.copy(fontSize = 30.sp),
    titleMedium = base.titleMedium.copy(fontSize = 24.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 22.sp),
    labelLarge = base.labelLarge.copy(fontSize = 22.sp),
)

@Composable
fun LeitnerTheme(mode: Mode? = null, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when (mode) {
        Mode.KIDS -> if (dark) KidsDark else KidsLight
        else -> if (dark) NormalDark else NormalLight
    }
    val typography = if (mode == Mode.KIDS) kidsTypography(Typography()) else Typography()
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
