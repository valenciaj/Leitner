package es.leitner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.leitner.app.data.BackupManager
import es.leitner.app.data.BoxSettingsEntity
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.MediaStorage
import es.leitner.app.data.ReviewLogEntity
import es.leitner.core.Mode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Exportar y volver a importar deja exactamente los mismos datos y ficheros. */
@RunWith(AndroidJUnit4::class)
class BackupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun exportar_e_importar_conserva_todo() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        val media = MediaStorage(context)
        val backup = BackupManager(db, media)
        try {
            val image = File(media.directory, "test-image.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val audio = File(media.directory, "test-audio.m4a").apply { writeBytes(byteArrayOf(4, 5)) }
            db.backupDao().insertDecks(listOf(DeckEntity(id = 7, name = "Animales", mode = Mode.KIDS, createdAt = 1)))
            db.backupDao().insertCards(
                listOf(CardEntity(id = 3, deckId = 7, front = "", back = "perro", imageUri = image.absolutePath, backAudioPath = audio.absolutePath, box = 2, nextReviewDay = 50, tags = "animales")),
            )
            db.backupDao().insertSettings(listOf(BoxSettingsEntity(Mode.KIDS, "1,2")))
            db.backupDao().insertLogs(listOf(ReviewLogEntity(cardId = 3, deckId = 7, day = 49, correct = true)))

            val out = ByteArrayOutputStream()
            assertEquals(BackupManager.Summary(1, 1), backup.export(out))

            // Borrar todo y restaurar.
            db.backupDao().clearLogs(); db.backupDao().clearCards(); db.backupDao().clearDecks()
            image.delete()
            audio.delete()

            assertEquals(BackupManager.Summary(1, 1), backup.import(ByteArrayInputStream(out.toByteArray())))

            val card = db.backupDao().cards().single()
            assertEquals("perro", card.back)
            assertEquals(2, card.box)
            assertEquals("animales", card.tags)
            assertTrue(File(card.imageUri!!).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
            assertTrue(File(card.backAudioPath!!).readBytes().contentEquals(byteArrayOf(4, 5)))
            assertEquals("Animales", db.backupDao().decks().single().name)
            assertEquals("1,2", db.backupDao().settings().single().intervals)
            assertEquals(1, db.backupDao().logs().size)
        } finally {
            db.close()
            File(media.directory, "test-image.jpg").delete()
            File(media.directory, "test-audio.m4a").delete()
        }
    }

    @Test(expected = BackupManager.InvalidBackupException::class)
    fun rechaza_un_fichero_que_no_es_copia() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        try {
            BackupManager(db, MediaStorage(context)).import(ByteArrayInputStream("hola".toByteArray()))
        } finally {
            db.close()
        }
    }
}
