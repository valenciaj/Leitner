package es.leitner.app.data

import androidx.room.withTransaction
import es.leitner.core.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copia de seguridad completa en un .zip:
 * - backup.json: mazos, tarjetas, ajustes de cajas y registro de repasos.
 * - media/: imágenes y audios de las tarjetas.
 * Importar REEMPLAZA todos los datos actuales.
 */
class BackupManager(
    private val db: LeitnerDatabase,
    private val media: MediaStorage,
) {
    data class Summary(val decks: Int, val cards: Int)

    class InvalidBackupException(message: String) : Exception(message)

    private val dao = db.backupDao()

    suspend fun export(output: OutputStream): Summary = withContext(Dispatchers.IO) {
        val decks = dao.decks()
        val cards = dao.cards()
        val json = JSONObject()
            .put("format", FORMAT)
            .put("decks", JSONArray(decks.map { it.toJson() }))
            .put("cards", JSONArray(cards.map { it.toJson() }))
            .put("settings", JSONArray(dao.settings().map { JSONObject().put("mode", it.mode.name).put("intervals", it.intervals) }))
            .put("logs", JSONArray(dao.logs().map { it.toJson() }))

        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(json.toString().toByteArray())
            zip.closeEntry()
            cards.flatMap { listOfNotNull(it.imageUri, it.audioPath) }
                .map(::File)
                .filter { it.isFile }
                .distinctBy { it.name }
                .forEach { file ->
                    zip.putNextEntry(ZipEntry("$MEDIA_PREFIX${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        Summary(decks.size, cards.size)
    }

    suspend fun import(input: InputStream): Summary = withContext(Dispatchers.IO) {
        // 1. Leer todo a una carpeta temporal antes de tocar nada.
        val staging = File(media.directory.parentFile, "backup-staging").apply { deleteRecursively(); mkdirs() }
        try {
            var jsonText: String? = null
            try {
                readZip(input, staging) { jsonText = it }
            } catch (e: java.util.zip.ZipException) {
                throw InvalidBackupException("El fichero no es una copia de Leitner")
            }
            val json = try {
                JSONObject(jsonText ?: throw InvalidBackupException("El fichero no es una copia de Leitner"))
            } catch (e: org.json.JSONException) {
                throw InvalidBackupException("La copia está dañada")
            }
            if (json.optInt("format") != FORMAT) throw InvalidBackupException("Versión de copia no compatible")

            val decks = json.getJSONArray("decks").objects().map { it.toDeck() }
            val cards = json.getJSONArray("cards").objects().map { it.toCard(media.directory) }
            val settings = json.getJSONArray("settings").objects()
                .map { BoxSettingsEntity(Mode.valueOf(it.getString("mode")), it.getString("intervals")) }
            val logs = json.optJSONArray("logs")?.objects()?.map { it.toLog() }.orEmpty()

            // 2. Reemplazar los datos en una sola transacción.
            db.withTransaction {
                dao.clearLogs()
                dao.clearCards()
                dao.clearDecks()
                dao.clearSettings()
                dao.insertDecks(decks)
                dao.insertCards(cards)
                dao.insertSettings(settings)
                dao.insertLogs(logs)
            }

            // 3. Sustituir los ficheros multimedia.
            media.directory.listFiles()?.forEach { it.delete() }
            staging.listFiles()?.forEach { it.renameTo(File(media.directory, it.name)) }

            Summary(decks.size, cards.size)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun readZip(input: InputStream, staging: File, onJson: (String) -> Unit) {
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.isDirectory -> Unit
                    entry.name == JSON_NAME -> onJson(zip.readBytes().toString(Charsets.UTF_8))
                    entry.name.startsWith(MEDIA_PREFIX) -> {
                        // Solo el nombre del fichero: evita rutas maliciosas ("../").
                        val name = File(entry.name).name
                        if (name.isNotBlank()) File(staging, name).outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }
    }

    private companion object {
        const val FORMAT = 1
        const val JSON_NAME = "backup.json"
        const val MEDIA_PREFIX = "media/"

        fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)

        fun DeckEntity.toJson() = JSONObject()
            .put("id", id).put("name", name).put("mode", mode.name).put("createdAt", createdAt)

        fun JSONObject.toDeck() = DeckEntity(
            id = getLong("id"),
            name = getString("name"),
            mode = Mode.valueOf(getString("mode")),
            createdAt = getLong("createdAt"),
        )

        fun CardEntity.toJson() = JSONObject()
            .put("id", id).put("deckId", deckId).put("front", front).put("back", back)
            .put("image", imageUri?.let { File(it).name } ?: JSONObject.NULL)
            .put("audio", audioPath?.let { File(it).name } ?: JSONObject.NULL)
            .put("box", box).put("nextReviewDay", nextReviewDay).put("createdAt", createdAt)

        fun JSONObject.toCard(mediaDir: File) = CardEntity(
            id = getLong("id"),
            deckId = getLong("deckId"),
            front = getString("front"),
            back = getString("back"),
            imageUri = optStringOrNull("image")?.let { File(mediaDir, File(it).name).absolutePath },
            audioPath = optStringOrNull("audio")?.let { File(mediaDir, File(it).name).absolutePath },
            box = getInt("box"),
            nextReviewDay = getLong("nextReviewDay"),
            createdAt = getLong("createdAt"),
        )

        fun ReviewLogEntity.toJson() = JSONObject()
            .put("id", id).put("cardId", cardId).put("deckId", deckId).put("day", day).put("correct", correct)

        fun JSONObject.toLog() = ReviewLogEntity(
            id = getLong("id"),
            cardId = getLong("cardId"),
            deckId = getLong("deckId"),
            day = getLong("day"),
            correct = getBoolean("correct"),
        )
    }
}
