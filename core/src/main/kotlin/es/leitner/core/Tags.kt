package es.leitner.core

/**
 * Etiquetas de una tarjeta, guardadas como texto separado por comas ("verbos, tema 3").
 * Se ignoran los espacios sobrantes, las vacías y las repetidas (sin distinguir mayúsculas).
 */
object Tags {
    /** Máximo de caracteres de una etiqueta. */
    const val MAX_LENGTH = 30

    fun parse(text: String): List<String> =
        text.split(',')
            .map { it.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    /** Texto normalizado para guardar. */
    fun normalize(text: String): String = parse(text).joinToString(", ")

    fun has(text: String, tag: String): Boolean = parse(text).any { it.equals(tag, ignoreCase = true) }

    /** Todas las etiquetas de varias tarjetas, sin repetir y en orden alfabético. */
    fun all(texts: Iterable<String>): List<String> =
        texts.flatMap(::parse).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
}
