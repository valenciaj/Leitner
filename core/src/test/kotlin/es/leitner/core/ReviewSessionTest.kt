package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    @Test
    fun `recorre las tarjetas en orden y cuenta aciertos`() {
        val session = ReviewSession(listOf(1, 2, 3))
        assertEquals(1L, session.answer(true))
        assertEquals(2L, session.answer(false))
        assertEquals(3L, session.answer(true))
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
}
