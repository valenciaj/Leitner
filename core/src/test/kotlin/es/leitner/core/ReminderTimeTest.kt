package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ReminderTimeTest {
    private val now = LocalDateTime.of(2026, 11, 12, 9, 0)

    @Test
    fun `hora futura de hoy`() {
        assertEquals(Duration.ofHours(9), ReminderTime.delayUntil(now, 18, 0))
    }

    @Test
    fun `hora ya pasada va a manana`() {
        assertEquals(Duration.ofHours(23), ReminderTime.delayUntil(now, 8, 0))
    }

    @Test
    fun `misma hora exacta va a manana`() {
        assertEquals(Duration.ofDays(1), ReminderTime.delayUntil(now, 9, 0))
    }

    @Test
    fun `formato con ceros`() {
        assertEquals("07:05", ReminderTime.format(7, 5))
    }
}
