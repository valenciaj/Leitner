package es.leitner.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    @Test
    fun `ignora mayusculas tildes y espacios`() {
        assertTrue(AnswerCheck.matches("  Canción ", "cancion"))
        assertTrue(AnswerCheck.matches("el  perro", "El perro."))
        assertTrue(AnswerCheck.matches("¿que tal?", "¿Qué tal?"))
    }

    @Test
    fun `acepta cualquiera de las opciones`() {
        assertTrue(AnswerCheck.matches("car", "coche / car / automobile"))
        assertTrue(AnswerCheck.matches("automobile", "coche; automobile"))
    }

    @Test
    fun `rechaza respuestas distintas o vacias`() {
        assertFalse(AnswerCheck.matches("gato", "perro"))
        assertFalse(AnswerCheck.matches("   ", "perro"))
    }
}
