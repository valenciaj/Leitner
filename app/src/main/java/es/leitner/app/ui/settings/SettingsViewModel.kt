package es.leitner.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.LeitnerConfig
import es.leitner.core.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Borrador editable de los intervalos (texto, para poder escribir libremente). */
data class SettingsUiState(
    val mode: Mode = Mode.NORMAL,
    val intervals: List<String> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
)

class SettingsViewModel(private val repository: LeitnerRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        selectMode(Mode.NORMAL)
    }

    fun selectMode(mode: Mode) {
        viewModelScope.launch {
            val config = repository.config(mode)
            _state.value = SettingsUiState(mode = mode, intervals = config.intervalsDays.map(Int::toString))
        }
    }

    fun setInterval(index: Int, value: String) = edit {
        it.toMutableList().also { list -> list[index] = value.filter(Char::isDigit).take(3) }
    }

    fun addBox() = edit {
        if (it.size >= LeitnerConfig.MAX_BOXES) it
        else it + ((it.lastOrNull()?.toIntOrNull() ?: 1) * 2).coerceAtMost(LeitnerConfig.MAX_INTERVAL_DAYS).toString()
    }

    fun removeBox() = edit { if (it.size <= LeitnerConfig.MIN_BOXES) it else it.dropLast(1) }

    fun restoreDefaults() = edit { LeitnerConfig.defaultFor(_state.value.mode).intervalsDays.map(Int::toString) }

    fun save() {
        val current = _state.value
        val config = try {
            LeitnerConfig(current.intervals.map { it.toIntOrNull() ?: 0 })
        } catch (e: IllegalArgumentException) {
            _state.value = current.copy(error = e.message, saved = false)
            return
        }
        viewModelScope.launch {
            repository.saveConfig(current.mode, config)
            _state.value = current.copy(error = null, saved = true)
        }
    }

    private fun edit(change: (List<String>) -> List<String>) {
        val s = _state.value
        _state.value = s.copy(intervals = change(s.intervals), error = null, saved = false)
    }

    class Factory(private val repository: LeitnerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
    }
}
