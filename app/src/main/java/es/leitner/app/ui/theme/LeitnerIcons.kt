package es.leitner.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconos de trazo que no vienen en material-icons-core.
 * Se dibujan aquí para no añadir la librería de iconos extendida (varios MB).
 */
object LeitnerIcons {
    val Stats: ImageVector by lazy { stroke("Stats", "M5 20V11M12 20V4M19 20v-6") }
    val SwapMode: ImageVector by lazy { stroke("SwapMode", "M7 4 3 8l4 4M3 8h14M17 20l4-4-4-4M21 16H7") }
    val Speaker: ImageVector by lazy {
        stroke("Speaker", "M11 5 6 9H3v6h3l5 4z", "M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13")
    }
    val Undo: ImageVector by lazy { stroke("Undo", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11") }
    val Keyboard: ImageVector by lazy {
        stroke("Keyboard", "M3 6h18v12H3z", "M7 10h.01M11 10h.01M15 10h.01M7 14h10")
    }
    val Trophy: ImageVector by lazy {
        stroke("Trophy", "M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0z", "M7 6H4a3 3 0 0 0 3 4M17 6h3a3 3 0 0 1-3 4")
    }

    private fun stroke(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }
}
