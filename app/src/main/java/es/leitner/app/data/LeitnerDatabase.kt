package es.leitner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DeckEntity::class, CardEntity::class, BoxSettingsEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LeitnerDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun boxSettingsDao(): BoxSettingsDao

    companion object {
        fun create(context: Context): LeitnerDatabase =
            Room.databaseBuilder(context, LeitnerDatabase::class.java, "leitner.db").build()
    }
}
