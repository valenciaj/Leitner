package es.leitner.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ancho a partir del cual se usa el diseño de tablet (dos paneles). */
const val WIDE_SCREEN_DP = 600

/** true en tablets y en pantallas anchas (también en horizontal si cabe). */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP

/** Ancho máximo cómodo para leer en pantallas grandes. */
val READABLE_WIDTH: Dp = 720.dp

/** Limita el ancho del contenido y lo centra; en el móvil no cambia nada. */
fun Modifier.centeredMaxWidth(max: Dp = READABLE_WIDTH): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)
