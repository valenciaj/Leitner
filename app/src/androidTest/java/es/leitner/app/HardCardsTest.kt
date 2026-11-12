package es.leitner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.ReviewLogEntity
import es.leitner.core.Mode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Las tarjetas difíciles son las falladas 2 veces o más, de más a menos % de fallos, sin mazos en pausa. */
@RunWith(AndroidJUnit4::class)
class HardCardsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun ordena_por_porcentaje_de_fallos() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, LeitnerDatabase::class.java).build()
        try {
            val deck = db.deckDao().insert(DeckEntity(name = "Inglés", mode = Mode.NORMAL))
            val paused = db.deckDao().insert(DeckEntity(name = "Pausado", mode = Mode.NORMAL, paused = true))
            fun card(deckId: Long, front: String) = CardEntity(deckId = deckId, front = front, back = "x", nextReviewDay = 0)
            val a = db.cardDao().insert(card(deck, "a")) // 2 fallos de 4
            val b = db.cardDao().insert(card(deck, "b")) // 3 fallos de 3
            val c = db.cardDao().insert(card(deck, "c")) // 1 fallo: no es difícil
            val d = db.cardDao().insert(card(paused, "d")) // mazo en pausa
            suspend fun log(cardId: Long, deckId: Long, vararg results: Boolean) =
                results.forEachIndexed { i, ok -> db.statsDao().log(ReviewLogEntity(cardId = cardId, deckId = deckId, day = i.toLong(), correct = ok)) }
            log(a, deck, false, true, false, true)
            log(b, deck, false, false, false)
            log(c, deck, false, true)
            log(d, paused, false, false)

            val hard = db.statsDao().observeHardCards(Mode.NORMAL, minFails = 2, limit = 10).first()
            assertEquals(listOf("b", "a"), hard.map { it.card.front })
            assertEquals(listOf(3, 2), hard.map { it.fails })
            assertEquals(listOf(3, 4), hard.map { it.answers })
        } finally {
            db.close()
        }
    }
}
