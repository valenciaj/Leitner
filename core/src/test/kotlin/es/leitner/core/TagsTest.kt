package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {
    @Test
    fun `separa por comas y limpia`() {
        assertEquals(listOf("verbos", "tema 3"), Tags.parse(" verbos ,, tema   3 , "))
    }

    @Test
    fun `quita repetidas sin distinguir mayusculas`() {
        assertEquals(listOf("Verbos"), Tags.parse("Verbos, verbos, VERBOS"))
    }

    @Test
    fun `texto vacio no tiene etiquetas`() {
        assertEquals(emptyList<String>(), Tags.parse(""))
        assertEquals("", Tags.normalize("  , "))
    }

    @Test
    fun `normaliza para guardar`() {
        assertEquals("a, b", Tags.normalize("a,b , A"))
    }

    @Test
    fun `recorta las largas`() {
        assertEquals(Tags.MAX_LENGTH, Tags.parse("x".repeat(50)).single().length)
    }

    @Test
    fun `comprueba si tiene una etiqueta`() {
        assertTrue(Tags.has("verbos, tema 3", "Tema 3"))
        assertFalse(Tags.has("verbos, tema 3", "tema"))
    }

    @Test
    fun `junta las de varias tarjetas`() {
        assertEquals(listOf("animales", "colores", "verbos"), Tags.all(listOf("verbos, animales", "colores", "Colores", "")))
    }
}
