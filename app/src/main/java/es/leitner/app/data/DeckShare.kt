package es.leitner.app.data

import androidx.room.withTransaction
import es.leitner.core.LeitnerScheduler
import es.leitner.core.Mode
import es.leitner.core.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Compartir un mazo en un fichero .leitner (un .zip):
 * - deck.json: nombre del mazo y tarjetas (sin cajas ni estadísticas).
 * - media/: imágenes y audios de las tarjetas.
 * Importar crea un mazo nuevo; no toca los demás.
 */
class DeckShare(
    private val db: LeitnerDatabase,
    private val media: MediaStorage,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) {
    data class Imported(val deckId: Long, val name: String, val cards: Int)

    /** El fichero no es un mazo compartido o está dañado. */
    class InvalidDeckFileException : Exception()

    /** Escribe el mazo en [output]. Devuelve cuántas tarjetas se exportaron. */
    suspend fun export(deckId: Long, output: OutputStream): Int = withContext(Dispatchers.IO) {
        val deck = checkNotNull(db.deckDao().get(deckId)) { "El mazo no existe" }
        val cards = db.cardDao().cardsOfDeck(deckId)
        val json = JSONObject()
            .put("format", FORMAT)
            .put("name", deck.name)
            .put("cards", JSONArray(cards.map { it.toShareJson() }))
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(json.toString().toByteArray())
            zip.closeEntry()
            cards.flatMap { it.content().files }
                .map(::File)
                .filter { it.isFile }
                .distinctBy { it.name }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry("$MEDIA_PREFIX${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        cards.size
    }

    /** Lee un mazo compartido y lo crea como mazo nuevo en [mode], con todas las tarjetas en la caja 1. */
    suspend fun import(input: InputStream, mode: Mode): Imported = withContext(Dispatchers.IO) {
        val staging = File(media.directory.parentFile, "share-staging").apply { deleteRecursively(); mkdirs() }
        val created = mutableListOf<String>()
        try {
            var jsonText: String? = null
            try {
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        when {
                            entry.isDirectory -> Unit
                            entry.name == JSON_NAME -> jsonText = zip.readBytes().toString(Charsets.UTF_8)
                            entry.name.startsWith(MEDIA_PREFIX) -> {
                                // Solo el nombre del fichero: evita rutas maliciosas ("../").
                                val name = File(entry.name).name
                                if (name.isNotBlank()) File(staging, name).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
            } catch (e: ZipException) {
                throw InvalidDeckFileException()
            }
            val json = try {
                JSONObject(jsonText ?: throw InvalidDeckFileException())
            } catch (e: JSONException) {
                throw InvalidDeckFileException()
            }
            if (json.optInt("format") != FORMAT) throw InvalidDeckFileException()
            val name = json.optString("name").trim().ifEmpty { throw InvalidDeckFileException() }

            // Cada fichero se copia con un nombre nuevo para no pisar los de otros mazos.
            fun file(obj: JSONObject, key: String): String? {
                if (!obj.has(key) || obj.isNull(key)) return null
                val source = File(staging, File(obj.getString(key)).name)
                if (!source.isFile) return null
                return media.copy(source.absolutePath)?.also(created::add)
            }

            val contents = try {
                val array = json.getJSONArray("cards")
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    CardContent(
                        front = obj.optString("front").trim(),
                        back = obj.optString("back").trim(),
                        imagePath = file(obj, "image"),
                        audioPath = file(obj, "audio"),
                        backImagePath = file(obj, "backImage"),
                        backAudioPath = file(obj, "backAudio"),
                        tags = Tags.normalize(obj.optString("tags", "")),
                    )
                }
            } catch (e: JSONException) {
                throw InvalidDeckFileException()
            }
            val valid = contents.filter { it.isValid }
            // Ficheros de tarjetas que no se van a crear.
            (contents - valid.toSet()).flatMap { it.files }.forEach { media.delete(it); created.remove(it) }

            val progress = LeitnerScheduler.newCard(today())
            val deckId = db.withTransaction {
                val id = db.deckDao().insert(DeckEntity(name = name, mode = mode))
                db.cardDao().insertAll(
                    valid.map {
                        CardEntity(
                            deckId = id,
                            front = it.front,
                            back = it.back,
                            imageUri = it.imagePath,
                            audioPath = it.audioPath,
                            backImageUri = it.backImagePath,
                            backAudioPath = it.backAudioPath,
                            tags = it.tags,
                            box = progress.box,
                            nextReviewDay = progress.nextReviewDay,
                        )
                    },
                )
                id
            }
            created.clear()
            Imported(deckId, name, valid.size)
        } finally {
            // Si algo falló, no dejar ficheros sueltos.
            created.forEach(media::delete)
            staging.deleteRecursively()
        }
    }

    companion object {
        /** Extensión de los ficheros de mazo compartido. */
        const val EXTENSION = "leitner"
        private const val FORMAT = 1
        private const val JSON_NAME = "deck.json"
        private const val MEDIA_PREFIX = "media/"

        private fun CardEntity.toShareJson() = JSONObject()
            .put("front", front).put("back", back)
            .put("image", imageUri?.let { File(it).name } ?: JSONObject.NULL)
            .put("audio", audioPath?.let { File(it).name } ?: JSONObject.NULL)
            .put("backImage", backImageUri?.let { File(it).name } ?: JSONObject.NULL)
            .put("backAudio", backAudioPath?.let { File(it).name } ?: JSONObject.NULL)
            .put("tags", tags)
    }
}
