package es.leitner.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.CardEntity
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.Mode
import es.leitner.core.ReviewSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val card: CardEntity? = null,
    val revealed: Boolean = false,
    val position: Int = 0,
    val total: Int = 0,
    val finished: Boolean = false,
    val correct: Int = 0,
    val wrong: Int = 0,
    val stars: Int = 0,
)

class ReviewViewModel(
    private val repository: LeitnerRepository,
    val mode: Mode,
    private val deckId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private lateinit var session: ReviewSession
    private var cards: Map<Long, CardEntity> = emptyMap()
    private var busy = false

    init {
        viewModelScope.launch {
            val due = repository.dueCards(deckId)
            cards = due.associateBy { it.id }
            val limit = if (mode == Mode.KIDS) ReviewSession.KIDS_SESSION_LIMIT else null
            session = ReviewSession(due.map { it.id }, limit)
            publish(revealed = false)
        }
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    fun answer(correct: Boolean) {
        if (busy || !::session.isInitialized || session.isFinished) return
        busy = true
        viewModelScope.launch {
            val id = session.answer(correct)
            repository.answer(id, mode, correct)
            publish(revealed = false)
            busy = false
        }
    }

    private fun publish(revealed: Boolean) {
        _state.value = ReviewUiState(
            loading = false,
            card = session.current?.let { cards[it] },
            revealed = revealed,
            position = session.answered + 1,
            total = session.total,
            finished = session.isFinished,
            correct = session.correct,
            wrong = session.wrong,
            stars = session.stars(),
        )
    }

    class Factory(
        private val repository: LeitnerRepository,
        private val mode: Mode,
        private val deckId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel(repository, mode, deckId) as T
    }
}
