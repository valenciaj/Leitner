package es.leitner.core

/** Estado de repaso de una tarjeta. Las fechas son días desde epoch (LocalDate.toEpochDay()). */
data class CardProgress(
    val box: Int,
    val nextReviewDay: Long,
)

/** Reglas de la caja de Leitner. */
object LeitnerScheduler {

    /** Estado de una tarjeta recién creada: caja 1 y lista para repasar hoy. */
    fun newCard(today: Long): CardProgress = CardProgress(box = 1, nextReviewDay = today)

    /** Una tarjeta toca repasarla si su fecha ya ha llegado. */
    fun isDue(progress: CardProgress, today: Long): Boolean = progress.nextReviewDay <= today

    /**
     * Aplica una respuesta:
     * - Acierto: sube una caja (sin pasar de la última).
     * - Fallo: vuelve a la caja 1.
     * La próxima fecha es hoy + intervalo de la nueva caja.
     */
    fun answer(
        progress: CardProgress,
        correct: Boolean,
        today: Long,
        config: LeitnerConfig,
    ): CardProgress {
        val current = progress.box.coerceIn(1, config.boxCount)
        val newBox = if (correct) (current + 1).coerceAtMost(config.boxCount) else 1
        return CardProgress(box = newBox, nextReviewDay = today + config.intervalFor(newBox))
    }

    /** Si se reduce el número de cajas, las tarjetas de cajas que ya no existen pasan a la última. */
    fun fitToConfig(progress: CardProgress, config: LeitnerConfig): CardProgress =
        if (progress.box > config.boxCount) progress.copy(box = config.boxCount) else progress
}
