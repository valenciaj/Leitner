package es.leitner.core

/**
 * Pegatinas que se ganan estudiando (pensadas para el modo niños).
 * Los textos (nombre y pista) están en la app, en strings.xml.
 */
enum class Achievement(val emoji: String) {
    FIRST_CORRECT("🌱"),
    CORRECT_10("🐣"),
    CORRECT_50("🦊"),
    CORRECT_100("🦁"),
    CORRECT_500("🐉"),
    CORRECT_1000("🦄"),
    STREAK_3("🔥"),
    STREAK_7("🌈"),
    STREAK_30("🚀"),
    DAYS_10("📚"),
    DAYS_50("🏰"),
}

object Achievements {

    /** Lo que se ha conseguido hasta ahora en un modo. */
    data class Progress(
        val totalCorrect: Int,
        val reviewDays: Set<Long>,
    )

    /** Pegatinas ganadas. Solo dependen del historial, así que no se pierden. */
    fun earned(progress: Progress): Set<Achievement> {
        val correct = progress.totalCorrect
        val best = Streak.longest(progress.reviewDays)
        val days = progress.reviewDays.size
        return buildSet {
            if (correct >= 1) add(Achievement.FIRST_CORRECT)
            if (correct >= 10) add(Achievement.CORRECT_10)
            if (correct >= 50) add(Achievement.CORRECT_50)
            if (correct >= 100) add(Achievement.CORRECT_100)
            if (correct >= 500) add(Achievement.CORRECT_500)
            if (correct >= 1000) add(Achievement.CORRECT_1000)
            if (best >= 3) add(Achievement.STREAK_3)
            if (best >= 7) add(Achievement.STREAK_7)
            if (best >= 30) add(Achievement.STREAK_30)
            if (days >= 10) add(Achievement.DAYS_10)
            if (days >= 50) add(Achievement.DAYS_50)
        }
    }
}
