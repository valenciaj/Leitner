package es.leitner.app.ui.deck

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.R
import es.leitner.app.data.CardContent
import es.leitner.app.data.CardEntity
import es.leitner.app.data.DeckEntity
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.CsvExport
import es.leitner.core.CsvImport
import es.leitner.core.LeitnerConfig
import es.leitner.core.Mode
import es.leitner.core.Tags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckViewModel(
    private val repository: LeitnerRepository,
    val mode: Mode,
    val deckId: Long,
    /** Para los textos de los mensajes. */
    private val resources: Resources,
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

    /** Todas las etiquetas del mazo. */
    val tags: StateFlow<List<String>> = cards.map { list -> Tags.all(list.map { it.tags }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedTag = MutableStateFlow<String?>(null)
    /** Etiqueta elegida para filtrar, o null para ver todas. */
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    fun selectTag(tag: String?) {
        _selectedTag.value = if (tag == _selectedTag.value) null else tag
    }

    /**
     * Tarjetas que coinciden con la etiqueta elegida y con la búsqueda
     * (pregunta, respuesta o etiquetas, sin mayúsculas ni tildes).
     */
    val visibleCards: StateFlow<List<CardEntity>> = combine(cards, _query, _selectedTag) { list, q, tag ->
        val needle = normalize(q.trim())
        list.filter { card ->
            (tag == null || Tags.has(card.tags, tag)) &&
                (needle.isEmpty() || listOf(card.front, card.back, card.tags).any { normalize(it).contains(needle) })
        }
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
                0 -> resources.getString(R.string.msg_reversed_none)
                else -> resources.getQuantityString(R.plurals.msg_reversed, added, added)
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
            !ok -> resources.getString(R.string.msg_export_failed)
            exported < all -> resources.getQuantityString(R.plurals.msg_exported, exported, exported) + " · " +
                resources.getQuantityString(R.plurals.msg_export_skipped, all - exported, all - exported)
            else -> resources.getQuantityString(R.plurals.msg_exported, exported, exported)
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
            _message.value = resources.getString(R.string.msg_read_failed)
            return
        }
        val result = CsvImport.parse(text)
        viewModelScope.launch {
            if (result.cards.isNotEmpty()) repository.addCards(deckId, result.cards)
            _message.value = buildString {
                append(resources.getQuantityString(R.plurals.msg_imported, result.cards.size, result.cards.size))
                if (result.skipped > 0) {
                    append(" · ")
                    append(resources.getQuantityString(R.plurals.msg_incomplete_lines, result.skipped, result.skipped))
                }
            }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    fun setPaused(paused: Boolean) {
        viewModelScope.launch { repository.setDeckPaused(deckId, paused) }
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
        private val resources: Resources,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DeckViewModel(repository, mode, deckId, resources) as T
    }
}
