package es.leitner.app.data

import androidx.room.TypeConverter
import es.leitner.core.Mode

class Converters {
    @TypeConverter
    fun modeToString(mode: Mode): String = mode.name

    @TypeConverter
    fun stringToMode(value: String): Mode = Mode.valueOf(value)
}
