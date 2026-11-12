package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {

    @Test
    fun `formato valido solo con 4 digitos`() {
        assertTrue(Pin.isValidFormat("0123"))
        assertFalse(Pin.isValidFormat("123"))
        assertFalse(Pin.isValidFormat("12345"))
        assertFalse(Pin.isValidFormat("12a4"))
    }

    @Test
    fun `el pin correcto coincide y el incorrecto no`() {
        val salt = Pin.newSalt()
        val hash = Pin.hash("4321", salt)
        assertTrue(Pin.matches("4321", salt, hash))
        assertFalse(Pin.matches("1234", salt, hash))
    }

    @Test
    fun `distinta sal da distinto hash`() {
        assertNotEquals(Pin.hash("1111", "a"), Pin.hash("1111", "b"))
        assertEquals(64, Pin.hash("1111", "a").length)
    }
}
