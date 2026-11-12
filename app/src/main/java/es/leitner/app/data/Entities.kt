package es.leitner.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import es.leitner.core.Mode

/** Un mazo pertenece a un modo (niños o normal). */
@Entity(tableName = "decks", indices = [Index("mode")])
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: Mode,
    val createdAt: Long = System.currentTimeMillis(),
    /** Mazo en pausa: sus tarjetas no salen en el repaso ni cuentan como pendientes. */
    @ColumnInfo(defaultValue = "0")
    val paused: Boolean = false,
)

/** Tarjeta con su caja actual y la fecha (epochDay) del próximo repaso. */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId"), Index("nextReviewDay")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    /** Ruta local (en filesDir) de una imagen opcional de la pregunta. */
    val imageUri: String? = null,
    /** Ruta local (en filesDir) de un audio opcional de la pregunta. */
    val audioPath: String? = null,
    /** Imagen opcional de la respuesta. */
    val backImageUri: String? = null,
    /** Audio opcional de la respuesta. */
    val backAudioPath: String? = null,
    val box: Int = 1,
    val nextReviewDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Configuración de cajas de cada modo. intervals = "1,2,4,8,16". */
@Entity(tableName = "box_settings")
data class BoxSettingsEntity(
    @PrimaryKey val mode: Mode,
    val intervals: String,
)

/** Un registro por cada respuesta en un repaso. Se usa para las estadísticas. */
@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId"), Index("day")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    /** Día del repaso (epochDay). */
    val day: Long,
    val correct: Boolean,
)
