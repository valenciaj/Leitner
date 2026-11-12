package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    @Test
    fun `sin repasos no hay pegatinas`() {
        assertTrue(Achievements.earned(Achievements.Progress(0, emptySet())).isEmpty())
    }

    @Test
    fun `aciertos y racha mas larga`() {
        // Racha de 3 días hace tiempo y luego días sueltos: la pegatina de racha se mantiene.
        val days = setOf(10L, 11L, 12L, 20L, 25L)
        val earned = Achievements.earned(Achievements.Progress(12, days))
        assertEquals(
            setOf(Achievement.FIRST_CORRECT, Achievement.CORRECT_10, Achievement.STREAK_3),
            earned,
        )
    }

    @Test
    fun `racha mas larga`() {
        assertEquals(0, Streak.longest(emptySet()))
        assertEquals(1, Streak.longest(setOf(5L)))
        assertEquals(4, Streak.longest(setOf(1L, 2L, 5L, 6L, 7L, 8L, 10L)))
    }
}
