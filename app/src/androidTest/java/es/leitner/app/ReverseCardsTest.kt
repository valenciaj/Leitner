package es.leitner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.leitner.app.data.CardContent
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.data.MediaStorage
import es.leitner.core.Mode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tarjetas al revés: se crean una sola vez y con copias propias de los ficheros. */
@RunWith(AndroidJUnit4::class)
class ReverseCardsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun crea_las_inversas_una_sola_vez() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        val media = MediaStorage(context)
        val repo = LeitnerRepository(db, media) { 100L }
        val image = File(media.directory, "reverse-test.jpg").apply { writeBytes(byteArrayOf(9)) }
        try {
            val deck = repo.createDeck("Inglés", Mode.NORMAL)
            repo.addCard(deck, CardContent("perro", "dog", imagePath = image.absolutePath))
            repo.addCard(deck, CardContent("gato", "cat"))

            assertEquals(2, repo.addReverseForDeck(deck))
            assertEquals(0, repo.addReverseForDeck(deck))

            val all = db.cardDao().cardsOfDeck(deck)
            assertEquals(4, all.size)
            val reversed = all.single { it.front == "dog" }
            assertEquals("perro", reversed.back)
            // La imagen pasa a la respuesta, como copia: borrar una tarjeta no rompe la otra.
            val copy = reversed.backImageUri!!
            assertNotEquals(image.absolutePath, copy)
            repo.deleteCard(all.single { it.front == "perro" }.id)
            assertTrue(File(copy).exists())
            repo.deleteCard(reversed.id)
        } finally {
            db.close()
            image.delete()
        }
    }
}
