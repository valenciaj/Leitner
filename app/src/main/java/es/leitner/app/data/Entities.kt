package es.leitner.app.data

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
    /** Ruta local de una imagen opcional (pensado para el modo niños). */
    val imageUri: String? = null,
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
