package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerSchedulerTest {

    private val config = LeitnerConfig.DEFAULT_NORMAL
    private val today = 20_000L

    @Test
    fun `una tarjeta nueva empieza en la caja 1 y toca hoy`() {
        val card = LeitnerScheduler.newCard(today)
        assertEquals(1, card.box)
        assertTrue(LeitnerScheduler.isDue(card, today))
    }

    @Test
    fun `acertar sube una caja y programa segun su intervalo`() {
        val result = LeitnerScheduler.answer(CardProgress(1, today), correct = true, today, config)
        assertEquals(2, result.box)
        assertEquals(today + 2, result.nextReviewDay)
    }

    @Test
    fun `fallar vuelve a la caja 1`() {
        val result = LeitnerScheduler.answer(CardProgress(4, today), correct = false, today, config)
        assertEquals(1, result.box)
        assertEquals(today + 1, result.nextReviewDay)
    }

    @Test
    fun `acertar en la ultima caja se queda en la ultima`() {
        val result = LeitnerScheduler.answer(CardProgress(5, today), correct = true, today, config)
        assertEquals(5, result.box)
        assertEquals(today + 16, result.nextReviewDay)
    }

    @Test
    fun `no toca repasar antes de su fecha`() {
        assertFalse(LeitnerScheduler.isDue(CardProgress(2, today + 1), today))
    }

    @Test
    fun `al reducir cajas la tarjeta pasa a la ultima`() {
        val fitted = LeitnerScheduler.fitToConfig(CardProgress(5, today), LeitnerConfig.DEFAULT_KIDS)
        assertEquals(3, fitted.box)
    }

    @Test
    fun `respuesta con caja fuera de rango se ajusta`() {
        val result = LeitnerScheduler.answer(CardProgress(5, today), correct = true, today, LeitnerConfig.DEFAULT_KIDS)
        assertEquals(3, result.box)
    }
}
