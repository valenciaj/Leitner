package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvImportTest {

    @Test
    fun `punto y coma`() {
        val r = CsvImport.parse("perro;dog\ngato;cat")
        assertEquals(listOf("perro" to "dog", "gato" to "cat"), r.cards)
    }

    @Test
    fun `coma y cabecera`() {
        val r = CsvImport.parse("pregunta,respuesta\n2+2,4\r\n3+3,6\r\n")
        assertEquals(listOf("2+2" to "4", "3+3" to "6"), r.cards)
    }

    @Test
    fun `tabulador`() {
        assertEquals(listOf("uno" to "one"), CsvImport.parse("uno\tone").cards)
    }

    @Test
    fun `comillas con separador, comillas y salto de linea dentro`() {
        val r = CsvImport.parse("\"Hola, mundo\",\"Dijo \"\"hola\"\"\"\n\"dos\nlíneas\",ok")
        assertEquals(listOf("Hola, mundo" to "Dijo \"hola\"", "dos\nlíneas" to "ok"), r.cards)
    }

    @Test
    fun `lineas incompletas se saltan y vacias se ignoran`() {
        val r = CsvImport.parse("a;b\nsolo\n\n;x\nc;d")
        assertEquals(listOf("a" to "b", "c" to "d"), r.cards)
        assertEquals(2, r.skipped)
    }

    @Test
    fun `quita BOM de Excel`() {
        assertEquals(listOf("a" to "b"), CsvImport.parse("﻿a;b").cards)
    }

    @Test
    fun `texto vacio`() {
        assertEquals(0, CsvImport.parse("").cards.size)
    }
}
