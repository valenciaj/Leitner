package es.leitner.core

/**
 * Sesión de repaso: recorre una lista de tarjetas pendientes, una a una.
 * No guarda nada; quien la usa persiste cada respuesta.
 *
 * - Las tarjetas falladas vuelven al final de la cola hasta acertarlas ([repeatFailed]).
 *   Solo el primer intento de cada tarjeta cuenta para las cajas y las estadísticas.
 * - [undo] deshace la última respuesta.
 *
 * @param cardIds tarjetas pendientes, en el orden en que se mostrarán.
 * @param limit máximo de tarjetas en la sesión (útil para sesiones cortas en modo niños).
 * @param repeatFailed si las falladas se repiten al final de la sesión.
 */
class ReviewSession(
    cardIds: List<Long>,
    limit: Int? = null,
    private val repeatFailed: Boolean = true,
) {

    /** Una respuesta dada en la sesión. */
    data class Step(
        val cardId: Long,
        val correct: Boolean,
        /** true si era el primer intento de esa tarjeta (el que hay que guardar). */
        val firstAttempt: Boolean,
        /** true si la respuesta añadió la tarjeta al final de la cola. */
        val requeued: Boolean,
    )

    private val queue: MutableList<Long> = (if (limit != null) cardIds.take(limit) else cardIds).distinct().toMutableList()
    private val history = mutableListOf<Step>()
    private val seen = mutableSetOf<Long>()
    private var index = 0

    /** Aciertos al primer intento. */
    var correct: Int = 0
        private set

    /** Fallos al primer intento. */
    var wrong: Int = 0
        private set

    /** Tarjetas distintas de la sesión. */
    val total: Int = queue.size

    /** Posición actual en la cola (incluye repeticiones). */
    val answered: Int get() = index

    /** Longitud de la cola (crece al repetir falladas). */
    val queueSize: Int get() = queue.size

    val isFinished: Boolean get() = index >= queue.size

    val canUndo: Boolean get() = history.isNotEmpty()

    /** true si la tarjeta actual ya se falló antes en esta sesión. */
    val isRetry: Boolean get() = current?.let { it in seen } ?: false

    /** Tarjeta actual, o null si la sesión ha terminado. */
    val current: Long? get() = queue.getOrNull(index)

    /** Registra la respuesta de la tarjeta actual y avanza. */
    fun answer(isCorrect: Boolean): Step {
        val id = checkNotNull(current) { "La sesión ya ha terminado" }
        val first = seen.add(id)
        if (first) {
            if (isCorrect) correct++ else wrong++
        }
        val requeue = !isCorrect && repeatFailed
        if (requeue) queue.add(id)
        index++
        return Step(id, isCorrect, first, requeue).also(history::add)
    }

    /** Deshace la última respuesta y vuelve a esa tarjeta. Devuelve lo deshecho, o null. */
    fun undo(): Step? {
        val step = history.removeLastOrNull() ?: return null
        // Las respuestas se deshacen en orden inverso, así que la tarjeta reencolada es la última.
        if (step.requeued) queue.removeAt(queue.lastIndex)
        index--
        if (step.firstAttempt) {
            seen.remove(step.cardId)
            if (step.correct) correct-- else wrong--
        }
        return step
    }

    /** Estrellas de 0 a 3 según el porcentaje de aciertos al primer intento (para el modo niños). */
    fun stars(): Int {
        val answeredCards = correct + wrong
        if (answeredCards == 0) return 0
        val ratio = correct.toDouble() / answeredCards
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

        /**
         * Orden de repaso: primero las cajas bajas (las que más cuestan) y, dentro de
         * cada caja, al azar para no memorizar el orden.
         */
        fun <T> order(cards: List<T>, box: (T) -> Int, random: kotlin.random.Random = kotlin.random.Random): List<T> =
            cards.groupBy(box).toSortedMap().values.flatMap { it.shuffled(random) }
    }
}
