package es.leitner.app.data

import androidx.room.withTransaction
import es.leitner.core.CardProgress
import es.leitner.core.LeitnerConfig
import es.leitner.core.LeitnerScheduler
import es.leitner.core.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Punto único de acceso a los datos. La UI no habla con Room directamente. */
class LeitnerRepository(
    private val db: LeitnerDatabase,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) {
    private val decks = db.deckDao()
    private val cards = db.cardDao()
    private val settings = db.boxSettingsDao()

    // ---- Configuración de cajas ----

    suspend fun config(mode: Mode): LeitnerConfig =
        settings.get(mode)?.let { LeitnerConfig.parse(it.intervals) } ?: LeitnerConfig.defaultFor(mode)

    fun observeConfig(mode: Mode): Flow<LeitnerConfig> =
        settings.observe(mode).map { it?.let { s -> LeitnerConfig.parse(s.intervals) } ?: LeitnerConfig.defaultFor(mode) }

    suspend fun saveConfig(mode: Mode, config: LeitnerConfig) = db.withTransaction {
        settings.upsert(BoxSettingsEntity(mode, config.serialize()))
        cards.clampBoxes(mode, config.boxCount)
    }

    // ---- Mazos ----

    fun observeDecks(mode: Mode): Flow<List<DeckSummary>> = decks.observeSummaries(mode, today())

    suspend fun createDeck(name: String, mode: Mode): Long = decks.insert(DeckEntity(name = name.trim(), mode = mode))

    fun observeDeck(deckId: Long): Flow<DeckEntity?> = decks.observe(deckId)

    suspend fun renameDeck(deckId: Long, name: String) = decks.rename(deckId, name.trim())

    /** Borra el mazo y todas sus tarjetas. */
    suspend fun deleteDeck(deckId: Long) = decks.deleteById(deckId)

    // ---- Tarjetas ----

    suspend fun addCard(deckId: Long, front: String, back: String, imageUri: String? = null): Long {
        val progress = LeitnerScheduler.newCard(today())
        return cards.insert(
            CardEntity(
                deckId = deckId,
                front = front.trim(),
                back = back.trim(),
                imageUri = imageUri,
                box = progress.box,
                nextReviewDay = progress.nextReviewDay,
            ),
        )
    }

    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cards.observeByDeck(deckId)

    suspend fun card(cardId: Long): CardEntity? = cards.get(cardId)

    /** Editar el texto no cambia la caja ni la fecha de repaso. */
    suspend fun updateCard(cardId: Long, front: String, back: String) =
        cards.updateText(cardId, front.trim(), back.trim())

    suspend fun deleteCard(cardId: Long) = cards.deleteById(cardId)

    suspend fun dueCards(deckId: Long): List<CardEntity> = cards.dueCards(deckId, today())

    /** Registra la respuesta a una tarjeta y la mueve de caja. */
    suspend fun answer(cardId: Long, mode: Mode, correct: Boolean) {
        val card = cards.get(cardId) ?: return
        val result = LeitnerScheduler.answer(
            progress = CardProgress(card.box, card.nextReviewDay),
            correct = correct,
            today = today(),
            config = config(mode),
        )
        cards.update(card.copy(box = result.box, nextReviewDay = result.nextReviewDay))
    }
}
