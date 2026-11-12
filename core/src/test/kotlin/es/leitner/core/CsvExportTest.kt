package es.leitner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    @Test
    fun `lo exportado se vuelve a importar igual`() {
        val cards = listOf(
            "perro" to "dog",
            "uno; dos" to "one, two",
            "con \"comillas\"" to "línea 1\nlínea 2",
        )
        val csv = CsvExport.write(cards)
        assertTrue(csv.startsWith("﻿pregunta;respuesta"))
        val result = CsvImport.parse(csv)
        assertEquals(cards, result.cards)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `sin tarjetas solo hay cabecera`() {
        assertEquals(0, CsvImport.parse(CsvExport.write(emptyList())).cards.size)
    }
}
