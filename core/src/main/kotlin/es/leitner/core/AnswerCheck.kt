package es.leitner.core

import java.text.Normalizer

/** Compara lo que se ha escrito con la respuesta correcta, sin ser demasiado estricto. */
object AnswerCheck {

    /**
     * true si [typed] coincide con [expected] sin contar mayúsculas, tildes, signos de puntuación
     * ni espacios de más. Si la respuesta tiene varias opciones separadas por "/" o ";",
     * vale cualquiera de ellas.
     */
    fun matches(typed: String, expected: String): Boolean {
        val answer = normalize(typed)
        if (answer.isEmpty()) return false
        val options = expected.split('/', ';').map(::normalize).filter { it.isNotEmpty() }
        return answer == normalize(expected) || answer in options
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[\\p{P}\\p{S}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
