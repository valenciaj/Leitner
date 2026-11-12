package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    private val today = 100L

    @Test
    fun `sin repasos la racha es cero`() {
        assertEquals(0, Streak.current(emptySet(), today))
    }

    @Test
    fun `cuenta dias seguidos hasta hoy`() {
        assertEquals(3, Streak.current(setOf(98, 99, 100), today))
    }

    @Test
    fun `si hoy no has repasado cuenta hasta ayer`() {
        assertEquals(2, Streak.current(setOf(98, 99), today))
    }

    @Test
    fun `un hueco corta la racha`() {
        assertEquals(1, Streak.current(setOf(96, 97, 100), today))
    }

    @Test
    fun `si el ultimo repaso fue anteayer no hay racha`() {
        assertEquals(0, Streak.current(setOf(97, 98), today))
    }
}
