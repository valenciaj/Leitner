package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LeitnerConfigTest {

    @Test
    fun `valores por defecto`() {
        assertEquals(listOf(1, 2, 4, 8, 16), LeitnerConfig.defaultFor(Mode.NORMAL).intervalsDays)
        assertEquals(3, LeitnerConfig.defaultFor(Mode.KIDS).boxCount)
    }

    @Test
    fun `serializar y leer devuelve lo mismo`() {
        val config = LeitnerConfig(listOf(1, 3, 7, 30))
        assertEquals(config, LeitnerConfig.parse(config.serialize()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza una sola caja`() {
        LeitnerConfig(listOf(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza intervalos de cero dias`() {
        LeitnerConfig(listOf(1, 0, 4))
    }
}
