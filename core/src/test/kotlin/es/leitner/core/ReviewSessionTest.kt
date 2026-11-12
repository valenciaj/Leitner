package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    @Test
    fun `recorre las tarjetas en orden y cuenta aciertos`() {
        val session = ReviewSession(listOf(1, 2, 3), repeatFailed = false)
        assertEquals(1L, session.answer(true).cardId)
        assertEquals(2L, session.answer(false).cardId)
        assertEquals(3L, session.answer(true).cardId)
        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(2, session.correct)
        assertEquals(1, session.wrong)
    }

    @Test
    fun `respeta el limite de tarjetas`() {
        val session = ReviewSession((1L..20L).toList(), limit = ReviewSession.KIDS_SESSION_LIMIT)
        assertEquals(10, session.total)
    }

    @Test
    fun `sesion vacia esta terminada`() {
        assertTrue(ReviewSession(emptyList()).isFinished)
    }

    @Test
    fun `estrellas segun aciertos`() {
        fun starsFor(correct: Int, wrong: Int): Int {
            val s = ReviewSession((1L..(correct + wrong).toLong()).toList())
            repeat(correct) { s.answer(true) }
            repeat(wrong) { s.answer(false) }
            return s.stars()
        }
        assertEquals(3, starsFor(10, 0))
        assertEquals(2, starsFor(7, 3))
        assertEquals(1, starsFor(1, 9))
        assertEquals(0, starsFor(0, 5))
    }

    @Test(expected = IllegalStateException::class)
    fun `no se puede responder con la sesion terminada`() {
        ReviewSession(emptyList()).answer(true)
    }

    @Test
    fun `las falladas se repiten al final hasta acertarlas`() {
        val session = ReviewSession(listOf(1, 2))
        session.answer(false) // 1
        session.answer(true) // 2
        assertEquals(1L, session.current)
        assertTrue(session.isRetry)
        val retry = session.answer(false) // 1 otra vez
        assertFalse(retry.firstAttempt)
        assertEquals(1L, session.current)
        session.answer(true)
        assertTrue(session.isFinished)
        // Solo cuenta el primer intento.
        assertEquals(1, session.correct)
        assertEquals(1, session.wrong)
        assertEquals(2, session.total)
    }

    @Test
    fun `deshacer vuelve a la tarjeta anterior`() {
        val session = ReviewSession(listOf(1, 2, 3))
        session.answer(true)
        session.answer(false)
        assertEquals(4, session.queueSize)
        val undone = session.undo()!!
        assertEquals(2L, undone.cardId)
        assertTrue(undone.firstAttempt)
        assertEquals(2L, session.current)
        assertEquals(3, session.queueSize)
        assertEquals(1, session.correct)
        assertEquals(0, session.wrong)
        assertFalse(session.isRetry)
        session.undo()
        assertEquals(1L, session.current)
        assertEquals(0, session.correct)
        assertFalse(session.canUndo)
        assertNull(session.undo())
    }

    @Test
    fun `deshacer un reintento no toca los contadores`() {
        val session = ReviewSession(listOf(1))
        session.answer(false)
        session.answer(true)
        assertTrue(session.isFinished)
        val undone = session.undo()!!
        assertFalse(undone.firstAttempt)
        assertEquals(0, session.correct)
        assertEquals(1, session.wrong)
        assertEquals(1L, session.current)
    }

    @Test
    fun `orden por caja y aleatorio dentro de cada caja`() {
        val cards = listOf(3 to 10L, 1 to 1L, 2 to 5L, 1 to 2L, 1 to 3L, 2 to 6L)
        val ordered = ReviewSession.order(cards, box = { it.first }, random = kotlin.random.Random(42))
        assertEquals(listOf(1, 1, 1, 2, 2, 3), ordered.map { it.first })
        assertEquals(setOf(1L, 2L, 3L), ordered.take(3).map { it.second }.toSet())
    }
}
