package es.leitner.core

/** Lee tarjetas de un CSV: una por línea, "pregunta;respuesta" (también con coma o tabulador). */
object CsvImport {

    data class Result(val cards: List<Pair<String, String>>, val skipped: Int)

    private val HEADERS = setOf("pregunta", "front", "question", "anverso")

    fun parse(text: String): Result {
        val rows = splitRows(text.removePrefix("﻿"))
        if (rows.isEmpty()) return Result(emptyList(), 0)
        val separator = detectSeparator(rows.first())

        val cards = mutableListOf<Pair<String, String>>()
        var skipped = 0
        rows.forEachIndexed { index, raw ->
            if (raw.isBlank()) return@forEachIndexed
            val fields = splitFields(raw, separator)
            if (index == 0 && fields.firstOrNull()?.trim()?.lowercase() in HEADERS) return@forEachIndexed
            val front = fields.getOrNull(0)?.trim().orEmpty()
            val back = fields.getOrNull(1)?.trim().orEmpty()
            if (front.isEmpty() || back.isEmpty()) skipped++ else cards += front to back
        }
        return Result(cards, skipped)
    }

    /** El separador más frecuente fuera de comillas en la primera línea. */
    private fun detectSeparator(line: String): Char =
        listOf(';', '\t', ',').maxBy { sep -> splitFields(line, sep).size }

    /** Divide en líneas respetando saltos de línea dentro de comillas. */
    private fun splitRows(text: String): List<String> {
        val rows = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> { inQuotes = !inQuotes; current.append(c) }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    rows += current.toString()
                    current.clear()
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) rows += current.toString()
        return rows
    }

    /** Divide una línea en campos; "" dentro de comillas es una comilla literal. */
    private fun splitFields(line: String, separator: Char): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == separator && !inQuotes -> { fields += current.toString(); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        fields += current.toString()
        return fields
    }
}
