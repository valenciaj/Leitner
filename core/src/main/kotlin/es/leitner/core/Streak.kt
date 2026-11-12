package es.leitner.core

/** Racha: días seguidos con al menos un repaso. */
object Streak {

    /**
     * Cuenta los días consecutivos con repaso que terminan hoy o ayer
     * (si hoy aún no has repasado, la racha de ayer sigue viva).
     */
    fun current(reviewDays: Set<Long>, today: Long): Int {
        var day = when {
            today in reviewDays -> today
            (today - 1) in reviewDays -> today - 1
            else -> return 0
        }
        var count = 0
        while (day in reviewDays) {
            count++
            day--
        }
        return count
    }
}
