package es.leitner.app.ui

import es.leitner.core.Mode

/** Nombres visibles de los modos. Cambiar aquí para renombrarlos en toda la app. */
object ModeNames {
    const val KIDS = "Peques"
    const val NORMAL = "Clásico"

    fun of(mode: Mode): String = when (mode) {
        Mode.KIDS -> KIDS
        Mode.NORMAL -> NORMAL
    }
}
