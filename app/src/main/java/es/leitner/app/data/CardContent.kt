package es.leitner.app.data

/** Contenido editable de una tarjeta: texto, imagen y audio de la pregunta y de la respuesta. */
data class CardContent(
    val front: String = "",
    val back: String = "",
    val imagePath: String? = null,
    val audioPath: String? = null,
    val backImagePath: String? = null,
    val backAudioPath: String? = null,
) {
    val hasQuestion: Boolean get() = front.isNotBlank() || imagePath != null || audioPath != null
    val hasAnswer: Boolean get() = back.isNotBlank() || backImagePath != null || backAudioPath != null

    /** Pregunta y respuesta pueden ser texto, imagen o audio, pero ninguna puede quedar vacía. */
    val isValid: Boolean get() = hasQuestion && hasAnswer

    /** Ficheros que usa. */
    val files: Set<String> get() = setOfNotNull(imagePath, audioPath, backImagePath, backAudioPath)

    /** La misma tarjeta con pregunta y respuesta intercambiadas. */
    fun reversed() = CardContent(
        front = back,
        back = front,
        imagePath = backImagePath,
        audioPath = backAudioPath,
        backImagePath = imagePath,
        backAudioPath = audioPath,
    )
}

fun CardEntity.content() = CardContent(front, back, imageUri, audioPath, backImageUri, backAudioPath)
