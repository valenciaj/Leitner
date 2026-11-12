package es.leitner.core

/** Pegatinas que se ganan estudiando (pensadas para el modo niños). */
enum class Achievement(val emoji: String, val title: String, val hint: String) {
    FIRST_CORRECT("🌱", "Primer acierto", "Acierta una tarjeta"),
    CORRECT_10("🐣", "10 aciertos", "Acierta 10 tarjetas"),
    CORRECT_50("🦊", "50 aciertos", "Acierta 50 tarjetas"),
    CORRECT_100("🦁", "100 aciertos", "Acierta 100 tarjetas"),
    CORRECT_500("🐉", "500 aciertos", "Acierta 500 tarjetas"),
    CORRECT_1000("🦄", "1000 aciertos", "Acierta 1000 tarjetas"),
    STREAK_3("🔥", "Racha de 3 días", "Repasa 3 días seguidos"),
    STREAK_7("🌈", "Racha de 7 días", "Repasa 7 días seguidos"),
    STREAK_30("🚀", "Racha de 30 días", "Repasa 30 días seguidos"),
    DAYS_10("📚", "10 días estudiando", "Repasa 10 días distintos"),
    DAYS_50("🏰", "50 días estudiando", "Repasa 50 días distintos"),
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
