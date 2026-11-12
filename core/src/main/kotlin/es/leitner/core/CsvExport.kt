package es.leitner.core

/** Escribe tarjetas en CSV ("pregunta;respuesta"), compatible con [CsvImport] y con Excel. */
object CsvExport {

    const val SEPARATOR = ';'

    /** Con BOM para que Excel lo abra como UTF-8 y con cabecera. */
    fun write(cards: List<Pair<String, String>>): String = buildString {
        append('﻿')
        append("pregunta").append(SEPARATOR).append("respuesta").append("\r\n")
        cards.forEach { (front, back) ->
            append(quote(front)).append(SEPARATOR).append(quote(back)).append("\r\n")
        }
    }

    private fun quote(field: String): String =
        if (field.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' || it == ',' || it == '\t' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
