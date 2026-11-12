package es.leitner.core

/**
 * Configuración de las cajas de Leitner.
 *
 * [intervalsDays] tiene un elemento por caja: el número de días que pasan
 * hasta volver a repasar una tarjeta que está en esa caja.
 * La caja 1 corresponde al índice 0.
 */
data class LeitnerConfig(val intervalsDays: List<Int>) {

    init {
        require(intervalsDays.size in MIN_BOXES..MAX_BOXES) {
            "El número de cajas debe estar entre $MIN_BOXES y $MAX_BOXES"
        }
        require(intervalsDays.all { it in 1..MAX_INTERVAL_DAYS }) {
            "Cada intervalo debe estar entre 1 y $MAX_INTERVAL_DAYS días"
        }
    }

    val boxCount: Int get() = intervalsDays.size

    /** Intervalo en días de la caja [box] (1..boxCount). */
    fun intervalFor(box: Int): Int = intervalsDays[box.coerceIn(1, boxCount) - 1]

    /** Formato para guardar en base de datos: "1,2,4,8,16". */
    fun serialize(): String = intervalsDays.joinToString(",")

    /** Por qué una lista de intervalos no vale. */
    enum class Problem { BOX_COUNT, INTERVAL_RANGE }

    companion object {
        const val MIN_BOXES = 2
        const val MAX_BOXES = 10
        const val MAX_INTERVAL_DAYS = 365

        val DEFAULT_NORMAL = LeitnerConfig(listOf(1, 2, 4, 8, 16))
        val DEFAULT_KIDS = LeitnerConfig(listOf(1, 2, 4))

        fun defaultFor(mode: Mode): LeitnerConfig = when (mode) {
            Mode.KIDS -> DEFAULT_KIDS
            Mode.NORMAL -> DEFAULT_NORMAL
        }

        /** Comprueba unos intervalos sin lanzar excepción. Devuelve null si son válidos. */
        fun validate(intervalsDays: List<Int>): Problem? = when {
            intervalsDays.size !in MIN_BOXES..MAX_BOXES -> Problem.BOX_COUNT
            !intervalsDays.all { it in 1..MAX_INTERVAL_DAYS } -> Problem.INTERVAL_RANGE
            else -> null
        }

        fun parse(value: String): LeitnerConfig =
            LeitnerConfig(value.split(",").map { it.trim().toInt() })
    }
}
