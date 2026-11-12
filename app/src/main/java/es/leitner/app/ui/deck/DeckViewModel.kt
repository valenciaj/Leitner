package es.leitner.app.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.ui.common.CardContent
import es.leitner.core.LeitnerConfig
import es.leitner.core.Mode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckViewModel(
    private val repository: LeitnerRepository,
    val mode: Mode,
    val deckId: Long,
) : ViewModel() {

    val deck: StateFlow<DeckEntity?> = repository.observeDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val cards: StateFlow<List<CardEntity>> = repository.observeCards(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val config: StateFlow<LeitnerConfig> = repository.observeConfig(mode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeitnerConfig.defaultFor(mode))

    fun addCard(content: CardContent) {
        if (!content.isValid) return
        viewModelScope.launch {
            repository.addCard(deckId, content.front, content.back, content.imagePath, content.audioPath)
        }
    }

    fun updateCard(cardId: Long, content: CardContent) {
        if (!content.isValid) return
        viewModelScope.launch {
            repository.updateCard(cardId, content.front, content.back, content.imagePath, content.audioPath)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameDeck(deckId, name) }
    }

    fun deleteDeck(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
            onDone()
        }
    }

    class Factory(
        private val repository: LeitnerRepository,
        private val mode: Mode,
        private val deckId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DeckViewModel(repository, mode, deckId) as T
    }
}
