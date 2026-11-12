package es.leitner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DeckEntity::class, CardEntity::class, BoxSettingsEntity::class, ReviewLogEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LeitnerDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun boxSettingsDao(): BoxSettingsDao
    abstract fun statsDao(): StatsDao

    companion object {
        fun create(context: Context): LeitnerDatabase =
            Room.databaseBuilder(context, LeitnerDatabase::class.java, "leitner.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2: tabla review_log para las estadísticas. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `review_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`cardId` INTEGER NOT NULL, " +
                        "`deckId` INTEGER NOT NULL, " +
                        "`day` INTEGER NOT NULL, " +
                        "`correct` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_log_cardId` ON `review_log` (`cardId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_log_day` ON `review_log` (`day`)")
            }
        }
    }
}
