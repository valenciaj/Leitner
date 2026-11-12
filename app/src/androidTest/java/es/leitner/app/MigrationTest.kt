package es.leitner.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.leitner.app.data.LeitnerDatabase
import es.leitner.app.data.ReviewLogEntity
import es.leitner.core.Mode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crea una base de datos con el esquema de la versión 1 (tal como la generaba Room),
 * la abre con la versión actual y comprueba que los datos siguen ahí.
 * Si una migración deja el esquema distinto del esperado, Room lanza una excepción al abrirla.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Test
    fun migra_de_v1_a_la_actual_sin_perder_datos() {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `decks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `mode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_decks_mode` ON `decks` (`mode`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `cards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`deckId` INTEGER NOT NULL, `front` TEXT NOT NULL, `back` TEXT NOT NULL, `imageUri` TEXT, " +
                    "`box` INTEGER NOT NULL, `nextReviewDay` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`deckId`) REFERENCES `decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_deckId` ON `cards` (`deckId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_nextReviewDay` ON `cards` (`nextReviewDay`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `box_settings` (`mode` TEXT NOT NULL, `intervals` TEXT NOT NULL, " +
                    "PRIMARY KEY(`mode`))",
            )

            db.execSQL("INSERT INTO decks (id, name, mode, createdAt) VALUES (1, 'Inglés', 'NORMAL', 0)")
            db.execSQL(
                "INSERT INTO cards (id, deckId, front, back, imageUri, box, nextReviewDay, createdAt) " +
                    "VALUES (1, 1, 'perro', 'dog', NULL, 3, 100, 0)",
            )
            db.execSQL("INSERT INTO box_settings (mode, intervals) VALUES ('NORMAL', '1,3,7')")
            db.version = 1
        }

        val room = Room.databaseBuilder(context, LeitnerDatabase::class.java, name)
            .addMigrations(LeitnerDatabase.MIGRATION_1_2, LeitnerDatabase.MIGRATION_2_3, LeitnerDatabase.MIGRATION_3_4)
            .build()
        try {
            runBlocking {
                val card = room.cardDao().get(1)!!
                assertEquals("perro", card.front)
                assertEquals(3, card.box)
                assertNull(card.audioPath)
                assertNull(card.backImageUri)
                assertNull(card.backAudioPath)
                assertEquals("1,3,7", room.boxSettingsDao().get(Mode.NORMAL)!!.intervals)

                // La tabla nueva funciona.
                room.statsDao().log(ReviewLogEntity(cardId = 1, deckId = 1, day = 100, correct = true))
                assertEquals(listOf(100L), room.statsDao().observeReviewDays(Mode.NORMAL).first())
            }
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }
}
