package es.leitner.app.data

import androidx.room.withTransaction
import es.leitner.core.Achievement
import es.leitner.core.Achievements
import es.leitner.core.CardProgress
import es.leitner.core.LeitnerConfig
import es.leitner.core.LeitnerScheduler
import es.leitner.core.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    suspend fun setDeckPaused(deckId: Long, paused: Boolean) = decks.setPaused(deckId, paused)

    suspend fun renameDeck(deckId: Long, name: String) = decks.rename(deckId, name.trim())

    /** Borra el mazo, todas sus tarjetas y sus imágenes y audios. */
    suspend fun deleteDeck(deckId: Long) {
        val files = cards.cardsOfDeck(deckId).flatMap { it.content().files }
        decks.deleteById(deckId)
        files.forEach(media::delete)
    }

    // ---- Tarjetas ----

    suspend fun addCard(deckId: Long, content: CardContent): Long {
        val progress = LeitnerScheduler.newCard(today())
        return cards.insert(
            CardEntity(
                deckId = deckId,
                front = content.front.trim(),
                back = content.back.trim(),
                imageUri = content.imagePath,
                audioPath = content.audioPath,
                backImageUri = content.backImagePath,
                backAudioPath = content.backAudioPath,
                box = progress.box,
                nextReviewDay = progress.nextReviewDay,
            ),
        )
    }

    /**
     * Añade la tarjeta inversa (respuesta → pregunta) de [content], con copias propias de sus ficheros.
     * Si el mazo ya tiene esa tarjeta inversa (mismo texto), no hace nada. Devuelve true si la añadió.
     */
    suspend fun addReverse(deckId: Long, content: CardContent): Boolean {
        val reversed = content.reversed()
        if (!reversed.isValid) return false
        val front = reversed.front.trim()
        val back = reversed.back.trim()
        // Solo se puede comprobar si hay texto; las tarjetas de solo imagen o audio se añaden siempre.
        val exists = (front.isNotEmpty() || back.isNotEmpty()) && cards.cardsOfDeck(deckId).any {
            it.front.equals(front, ignoreCase = true) && it.back.equals(back, ignoreCase = true)
        }
        if (exists) return false
        addCard(
            deckId,
            reversed.copy(
                imagePath = reversed.imagePath?.let(media::copy),
                audioPath = reversed.audioPath?.let(media::copy),
                backImagePath = reversed.backImagePath?.let(media::copy),
                backAudioPath = reversed.backAudioPath?.let(media::copy),
            ),
        )
        return true
    }

    /** Crea la tarjeta inversa de todas las del mazo que aún no la tengan. Devuelve cuántas creó. */
    suspend fun addReverseForDeck(deckId: Long): Int =
        cards.cardsOfDeck(deckId).sortedBy { it.id }.count { addReverse(deckId, it.content()) }

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
    suspend fun updateCard(cardId: Long, content: CardContent) {
        val old = cards.get(cardId) ?: return
        cards.updateContent(
            cardId,
            content.front.trim(),
            content.back.trim(),
            content.imagePath,
            content.audioPath,
            content.backImagePath,
            content.backAudioPath,
        )
        (old.content().files - content.files).forEach(media::delete)
    }

    suspend fun deleteCard(cardId: Long) {
        val old = cards.get(cardId) ?: return
        cards.deleteById(cardId)
        old.content().files.forEach(media::delete)
    }

    /** Pendientes de hoy de un mazo, o de todos los mazos del modo si [deckId] es [ALL_DECKS]. */
    suspend fun dueCards(mode: Mode, deckId: Long): List<CardEntity> =
        if (deckId == ALL_DECKS) cards.dueCardsOfMode(mode, today()) else cards.dueCards(deckId, today())

    /** Registra la respuesta a una tarjeta y la mueve de caja. Devuelve lo necesario para deshacerla. */
    suspend fun answer(cardId: Long, mode: Mode, correct: Boolean): AnswerRecord? {
        val card = cards.get(cardId) ?: return null
        val result = LeitnerScheduler.answer(
            progress = CardProgress(card.box, card.nextReviewDay),
            correct = correct,
            today = today(),
            config = config(mode),
        )
        return db.withTransaction {
            cards.update(card.copy(box = result.box, nextReviewDay = result.nextReviewDay))
            val logId = stats.log(ReviewLogEntity(cardId = card.id, deckId = card.deckId, day = today(), correct = correct))
            AnswerRecord(card.id, card.box, card.nextReviewDay, logId)
        }
    }

    /** Deshace una respuesta: devuelve la tarjeta a su caja y borra el registro. */
    suspend fun undoAnswer(record: AnswerRecord) = db.withTransaction {
        cards.setProgress(record.cardId, record.previousBox, record.previousNextReviewDay)
        stats.deleteLog(record.logId)
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

    // ---- Pegatinas ----

    fun observeAchievements(mode: Mode): Flow<Set<Achievement>> =
        combine(stats.observeTotalCorrect(mode), stats.observeReviewDays(mode)) { correct, days ->
            Achievements.earned(Achievements.Progress(correct, days.toSet()))
        }

    suspend fun achievements(mode: Mode): Set<Achievement> =
        Achievements.earned(Achievements.Progress(stats.totalCorrect(mode), stats.reviewDays(mode).toSet()))

    companion object {
        /** deckId especial para repasar todos los mazos de un modo. */
        const val ALL_DECKS = 0L
    }
}

/** Estado de una tarjeta antes de responderla, para poder deshacer. */
data class AnswerRecord(
    val cardId: Long,
    val previousBox: Int,
    val previousNextReviewDay: Long,
    val logId: Long,
)
