package es.leitner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.DeckShare
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.MediaStorage
import es.leitner.core.Mode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Compartir un mazo y volver a importarlo crea un mazo nuevo con las mismas tarjetas, en la caja 1. */
@RunWith(AndroidJUnit4::class)
class DeckShareTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun compartir_e_importar_crea_un_mazo_nuevo() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        val media = MediaStorage(context)
        val share = DeckShare(db, media, today = { 100 })
        val image = File(media.directory, "share-test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        var importedImage: String? = null
        try {
            val deckId = db.deckDao().insert(DeckEntity(name = "Animales", mode = Mode.NORMAL))
            db.cardDao().insertAll(
                listOf(
                    CardEntity(deckId = deckId, front = "dog", back = "perro", box = 4, nextReviewDay = 50),
                    CardEntity(deckId = deckId, front = "", back = "gato", imageUri = image.absolutePath, box = 2, nextReviewDay = 60),
                ),
            )

            val out = ByteArrayOutputStream()
            assertEquals(2, share.export(deckId, out))

            val imported = share.import(ByteArrayInputStream(out.toByteArray()), Mode.KIDS)
            assertEquals("Animales", imported.name)
            assertEquals(2, imported.cards)
            assertEquals(Mode.KIDS, db.deckDao().get(imported.deckId)!!.mode)

            val cards = db.cardDao().cardsOfDeck(imported.deckId).sortedBy { it.back }
            assertEquals(listOf("gato", "perro"), cards.map { it.back })
            assertTrue(cards.all { it.box == 1 && it.nextReviewDay == 100L })
            importedImage = cards[0].imageUri
            assertNotEquals(image.absolutePath, importedImage)
            assertTrue(File(importedImage!!).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
            assertNull(cards[1].imageUri)
            // El mazo original sigue igual.
            assertEquals(2, db.cardDao().cardsOfDeck(deckId).size)
        } finally {
            db.close()
            image.delete()
            importedImage?.let { File(it).delete() }
        }
    }

    @Test(expected = DeckShare.InvalidDeckFileException::class)
    fun rechaza_un_fichero_que_no_es_un_mazo() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        try {
            DeckShare(db, MediaStorage(context)).import(ByteArrayInputStream("hola".toByteArray()), Mode.NORMAL)
        } finally {
            db.close()
        }
    }
}
