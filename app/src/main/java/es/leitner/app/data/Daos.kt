package es.leitner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import es.leitner.core.Mode
import kotlinx.coroutines.flow.Flow

/** Mazo con contadores para la pantalla principal. */
data class DeckSummary(
    val id: Long,
    val name: String,
    val totalCards: Int,
    val dueCards: Int,
)

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: DeckEntity): Long

    @Update
    suspend fun update(deck: DeckEntity)

    @Delete
    suspend fun delete(deck: DeckEntity)

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun get(id: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observe(id: Long): Flow<DeckEntity?>

    @Query("UPDATE decks SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT d.id, d.name,
               COUNT(c.id) AS totalCards,
               COALESCE(SUM(CASE WHEN c.nextReviewDay <= :today THEN 1 ELSE 0 END), 0) AS dueCards
        FROM decks d LEFT JOIN cards c ON c.deckId = d.id
        WHERE d.mode = :mode
        GROUP BY d.id
        ORDER BY d.name COLLATE NOCASE
        """,
    )
    fun observeSummaries(mode: Mode, today: Long): Flow<List<DeckSummary>>
}

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: Long): CardEntity?

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE cards SET front = :front, back = :back, imageUri = :imagePath, audioPath = :audioPath WHERE id = :id")
    suspend fun updateContent(id: Long, front: String, back: String, imagePath: String?, audioPath: String?)

    @Query("SELECT imageUri FROM cards WHERE deckId = :deckId AND imageUri IS NOT NULL")
    suspend fun imagePathsOfDeck(deckId: Long): List<String>

    @Query("SELECT audioPath FROM cards WHERE deckId = :deckId AND audioPath IS NOT NULL")
    suspend fun audioPathsOfDeck(deckId: Long): List<String>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt")
    fun observeByDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND nextReviewDay <= :today ORDER BY box, nextReviewDay")
    suspend fun dueCards(deckId: Long, today: Long): List<CardEntity>

    /** Al reducir el número de cajas, mueve a la última caja las tarjetas que se han quedado fuera. */
    @Query(
        """
        UPDATE cards SET box = :lastBox
        WHERE box > :lastBox AND deckId IN (SELECT id FROM decks WHERE mode = :mode)
        """,
    )
    suspend fun clampBoxes(mode: Mode, lastBox: Int)
}

@Dao
interface BoxSettingsDao {
    @Query("SELECT * FROM box_settings WHERE mode = :mode")
    suspend fun get(mode: Mode): BoxSettingsEntity?

    @Query("SELECT * FROM box_settings WHERE mode = :mode")
    fun observe(mode: Mode): Flow<BoxSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BoxSettingsEntity)
}

data class BoxCount(val box: Int, val count: Int)

data class DayActivity(val day: Long, val correct: Int, val total: Int)

@Dao
interface StatsDao {
    @Insert
    suspend fun log(entry: ReviewLogEntity)

    @Query(
        """
        SELECT c.box AS box, COUNT(*) AS count
        FROM cards c JOIN decks d ON c.deckId = d.id
        WHERE d.mode = :mode
        GROUP BY c.box ORDER BY c.box
        """,
    )
    fun observeBoxCounts(mode: Mode): Flow<List<BoxCount>>

    @Query(
        """
        SELECT COUNT(*) FROM cards c JOIN decks d ON c.deckId = d.id
        WHERE d.mode = :mode AND c.nextReviewDay <= :today
        """,
    )
    fun observeDueCount(mode: Mode, today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE nextReviewDay <= :today")
    suspend fun totalDue(today: Long): Int

    @Query(
        """
        SELECT r.day AS day, SUM(r.correct) AS correct, COUNT(*) AS total
        FROM review_log r JOIN decks d ON r.deckId = d.id
        WHERE d.mode = :mode AND r.day >= :fromDay
        GROUP BY r.day ORDER BY r.day
        """,
    )
    fun observeActivity(mode: Mode, fromDay: Long): Flow<List<DayActivity>>

    @Query(
        """
        SELECT DISTINCT r.day FROM review_log r JOIN decks d ON r.deckId = d.id
        WHERE d.mode = :mode ORDER BY r.day DESC
        """,
    )
    fun observeReviewDays(mode: Mode): Flow<List<Long>>
}
