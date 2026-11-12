package es.leitner.core

/**
 * Sesión de repaso: recorre una lista de tarjetas pendientes, una a una.
 * No guarda nada; quien la usa persiste cada respuesta.
 *
 * @param cardIds tarjetas pendientes, en el orden en que se mostrarán.
 * @param limit máximo de tarjetas en la sesión (útil para sesiones cortas en modo niños).
 */
class ReviewSession(cardIds: List<Long>, limit: Int? = null) {

    private val queue: List<Long> = if (limit != null) cardIds.take(limit) else cardIds
    private var index = 0

    var correct: Int = 0
        private set
    var wrong: Int = 0
        private set

    val total: Int get() = queue.size
    val answered: Int get() = index
    val isFinished: Boolean get() = index >= queue.size

    /** Tarjeta actual, o null si la sesión ha terminado. */
    val current: Long? get() = queue.getOrNull(index)

    /** Registra la respuesta de la tarjeta actual y avanza. Devuelve el id respondido. */
    fun answer(isCorrect: Boolean): Long {
        val id = checkNotNull(current) { "La sesión ya ha terminado" }
        if (isCorrect) correct++ else wrong++
        index++
        return id
    }

    /** Estrellas de 0 a 3 según el porcentaje de aciertos (para el modo niños). */
    fun stars(): Int {
        if (answered == 0) return 0
        val ratio = correct.toDouble() / answered
        return when {
            ratio >= 0.9 -> 3
            ratio >= 0.6 -> 2
            ratio > 0.0 -> 1
            else -> 0
        }
    }

    companion object {
        /** Tamaño de sesión por defecto en modo niños. */
        const val KIDS_SESSION_LIMIT = 10
    }
}
