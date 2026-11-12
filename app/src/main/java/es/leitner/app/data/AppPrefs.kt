package es.leitner.app.data

import android.content.Context
import es.leitner.core.Mode
import es.leitner.core.Pin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencias sencillas: PIN del modo niños y último modo usado. */
class AppPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("leitner_prefs", Context.MODE_PRIVATE)

    private val _hasPin = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return true
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return true
        return Pin.matches(pin, salt, hash)
    }

    fun setPin(pin: String) {
        require(Pin.isValidFormat(pin))
        val salt = Pin.newSalt()
        prefs.edit().putString(KEY_PIN_SALT, salt).putString(KEY_PIN_HASH, Pin.hash(pin, salt)).apply()
        _hasPin.value = true
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).apply()
        _hasPin.value = false
    }

    // ---- Escribir la respuesta (por modo) ----

    fun typeAnswer(mode: Mode): Boolean = prefs.getBoolean(KEY_TYPE_ANSWER + mode.name, false)

    fun setTypeAnswer(mode: Mode, on: Boolean) = prefs.edit().putBoolean(KEY_TYPE_ANSWER + mode.name, on).apply()

    // ---- Recordatorio diario ----

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ON, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 18)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    // ---- Tema ----

    private val _theme = MutableStateFlow(
        prefs.getString(KEY_THEME, null)?.let { runCatching { ThemeChoice.valueOf(it) }.getOrNull() } ?: ThemeChoice.SYSTEM,
    )
    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()

    fun setTheme(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME, choice.name).apply()
        _theme.value = choice
    }

    var lastMode: Mode?
        get() = prefs.getString(KEY_LAST_MODE, null)?.let { runCatching { Mode.valueOf(it) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_LAST_MODE, value?.name).apply()

    private companion object {
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_LAST_MODE = "last_mode"
        const val KEY_THEME = "theme"
        const val KEY_TYPE_ANSWER = "type_answer_"
        const val KEY_REMINDER_ON = "reminder_on"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
