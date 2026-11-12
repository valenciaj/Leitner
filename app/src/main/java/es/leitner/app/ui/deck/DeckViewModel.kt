package es.leitner.app.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.LeitnerRepository
import es.leitner.app.data.CardContent
import es.leitner.core.LeitnerConfig
import es.leitner.core.Mode
import es.leitner.core.CsvExport
import es.leitner.core.CsvImport
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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

    private val _query = MutableStateFlow("")
    /** Texto de búsqueda. */
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(text: String) {
        _query.value = text
    }

    /** Tarjetas que coinciden con la búsqueda (pregunta o respuesta, sin mayúsculas ni tildes). */
    val visibleCards: StateFlow<List<CardEntity>> = combine(cards, _query) { list, q ->
        val needle = normalize(q.trim())
        if (needle.isEmpty()) list else list.filter { normalize(it.front).contains(needle) || normalize(it.back).contains(needle) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val config: StateFlow<LeitnerConfig> = repository.observeConfig(mode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeitnerConfig.defaultFor(mode))

    fun addCard(content: CardContent, alsoReversed: Boolean = false) {
        if (!content.isValid) return
        viewModelScope.launch {
            repository.addCard(deckId, content)
            if (alsoReversed) repository.addReverse(deckId, content)
        }
    }

    fun updateCard(cardId: Long, content: CardContent) {
        if (!content.isValid) return
        viewModelScope.launch { repository.updateCard(cardId, content) }
    }

    /** Crea la tarjeta al revés de todas las del mazo que no la tengan. */
    fun addReversedCards() {
        viewModelScope.launch {
            val added = repository.addReverseForDeck(deckId)
            _message.value = when (added) {
                0 -> "Todas las tarjetas ya tienen su versión al revés"
                1 -> "1 tarjeta al revés creada"
                else -> "$added tarjetas al revés creadas"
            }
        }
    }

    /** CSV con las tarjetas que tienen texto en la pregunta y en la respuesta. */
    fun csv(): String = CsvExport.write(
        cards.value.filter { it.front.isNotBlank() && it.back.isNotBlank() }.map { it.front to it.back },
    )

    fun onExported(ok: Boolean) {
        val all = cards.value.size
        val exported = cards.value.count { it.front.isNotBlank() && it.back.isNotBlank() }
        _message.value = when {
            !ok -> "No se pudo exportar"
            exported < all -> "$exported tarjetas exportadas · ${all - exported} sin texto se han omitido"
            else -> "$exported tarjetas exportadas"
        }
    }

    private val _message = MutableStateFlow<String?>(null)
    /** Mensaje corto para mostrar (p. ej. resultado de importar). */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun importCsv(text: String?) {
        if (text == null) {
            _message.value = "No se pudo leer el fichero"
            return
        }
        val result = CsvImport.parse(text)
        viewModelScope.launch {
            if (result.cards.isNotEmpty()) repository.addCards(deckId, result.cards)
            _message.value = buildString {
                append(if (result.cards.size == 1) "1 tarjeta importada" else "${result.cards.size} tarjetas importadas")
                if (result.skipped > 0) append(" · ${result.skipped} líneas incompletas")
            }
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

    private companion object {
        fun normalize(text: String): String =
            java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
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
