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
    private val media: MediaStorage,
    private val clock: () -> Long = { LocalDate.now().toEpochDay() },
) {
    private val decks = db.deckDao()
    private val cards = db.cardDao()
    private val settings = db.boxSettingsDao()
    private val stats = db.statsDao()

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

    /** Borra el mazo, todas sus tarjetas y sus imágenes y audios. */
    suspend fun deleteDeck(deckId: Long) {
        val files = cards.imagePathsOfDeck(deckId) + cards.audioPathsOfDeck(deckId)
        decks.deleteById(deckId)
        files.forEach(media::delete)
    }

    // ---- Tarjetas ----

    suspend fun addCard(
        deckId: Long,
        front: String,
        back: String,
        imagePath: String? = null,
        audioPath: String? = null,
    ): Long {
        val progress = LeitnerScheduler.newCard(today())
        return cards.insert(
            CardEntity(
                deckId = deckId,
                front = front.trim(),
                back = back.trim(),
                imageUri = imagePath,
                audioPath = audioPath,
                box = progress.box,
                nextReviewDay = progress.nextReviewDay,
            ),
        )
    }

    /** Añade muchas tarjetas de texto de golpe (importar CSV). */
    suspend fun addCards(deckId: Long, items: List<Pair<String, String>>) {
        val progress = LeitnerScheduler.newCard(today())
        cards.insertAll(
            items.map { (front, back) ->
                CardEntity(
                    deckId = deckId,
                    front = front,
                    back = back,
                    box = progress.box,
                    nextReviewDay = progress.nextReviewDay,
                )
            },
        )
    }

    fun observeCards(deckId: Long): Flow<List<CardEntity>> = cards.observeByDeck(deckId)

    suspend fun card(cardId: Long): CardEntity? = cards.get(cardId)

    /** Editar no cambia la caja ni la fecha de repaso. Borra los ficheros que se sustituyen. */
    suspend fun updateCard(cardId: Long, front: String, back: String, imagePath: String?, audioPath: String?) {
        val old = cards.get(cardId) ?: return
        cards.updateContent(cardId, front.trim(), back.trim(), imagePath, audioPath)
        if (old.imageUri != imagePath) old.imageUri?.let(media::delete)
        if (old.audioPath != audioPath) old.audioPath?.let(media::delete)
    }

    suspend fun deleteCard(cardId: Long) {
        val old = cards.get(cardId) ?: return
        cards.deleteById(cardId)
        old.imageUri?.let(media::delete)
        old.audioPath?.let(media::delete)
    }

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
        db.withTransaction {
            cards.update(card.copy(box = result.box, nextReviewDay = result.nextReviewDay))
            stats.log(ReviewLogEntity(cardId = card.id, deckId = card.deckId, day = today(), correct = correct))
        }
    }

    // ---- Estadísticas ----

    /** Hoy, como epochDay. */
    fun today(): Long = clock()

    fun observeBoxCounts(mode: Mode): Flow<List<BoxCount>> = stats.observeBoxCounts(mode)

    fun observeDueCount(mode: Mode): Flow<Int> = stats.observeDueCount(mode, today())

    /** Tarjetas pendientes hoy en todos los modos (para el recordatorio). */
    suspend fun totalDue(): Int = stats.totalDue(today())

    /** Actividad de los últimos [days] días (incluido hoy). */
    fun observeActivity(mode: Mode, days: Int): Flow<List<DayActivity>> =
        stats.observeActivity(mode, today() - (days - 1))

    fun observeReviewDays(mode: Mode): Flow<List<Long>> = stats.observeReviewDays(mode)
}
