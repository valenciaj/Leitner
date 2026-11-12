package es.leitner.app.ui

import androidx.annotation.StringRes
import es.leitner.app.R
import es.leitner.core.Mode

/** Nombres visibles de los modos (en strings.xml: mode_kids y mode_normal). */
object ModeNames {
    @StringRes
    fun of(mode: Mode): Int = when (mode) {
        Mode.KIDS -> R.string.mode_kids
        Mode.NORMAL -> R.string.mode_normal
    }
}
