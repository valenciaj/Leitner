package es.leitner.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.DeckSummary
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.Mode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: LeitnerRepository,
    val mode: Mode,
) : ViewModel() {

    val decks: StateFlow<List<DeckSummary>> = repository.observeDecks(mode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tarjetas repasadas hoy (primeros intentos; la práctica no cuenta). */
    val reviewedToday: StateFlow<Int> = repository.observeActivity(mode, days = 1)
        .map { days -> days.sumOf { it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun createDeck(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createDeck(name, mode) }
    }

    class Factory(private val repository: LeitnerRepository, private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository, mode) as T
    }
}
