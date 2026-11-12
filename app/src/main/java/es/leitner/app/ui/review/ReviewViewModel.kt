package es.leitner.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.AnswerRecord
import es.leitner.app.data.AppPrefs
import es.leitner.app.data.CardEntity
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.Achievement
import es.leitner.core.AnswerCheck
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
    /** Posición en la cola (empieza en 1). */
    val position: Int = 0,
    /** Longitud de la cola, incluidas las falladas que se repiten. */
    val queueSize: Int = 0,
    /** Tarjetas distintas de la sesión. */
    val total: Int = 0,
    val isRetry: Boolean = false,
    val canUndo: Boolean = false,
    val finished: Boolean = false,
    val correct: Int = 0,
    val wrong: Int = 0,
    val stars: Int = 0,
    /** Modo "escribir la respuesta" activado. */
    val typeAnswer: Boolean = false,
    /** Resultado de comprobar lo escrito: null si no se ha comprobado. */
    val typedCorrect: Boolean? = null,
    val typedText: String = "",
    /** Pegatinas ganadas en esta sesión. */
    val newAchievements: List<Achievement> = emptyList(),
)

class ReviewViewModel(
    private val repository: LeitnerRepository,
    val mode: Mode,
    private val deckId: Long,
    private val prefs: AppPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private lateinit var session: ReviewSession
    private var cards: Map<Long, CardEntity> = emptyMap()
    /** Respuestas guardadas en la base de datos, para poder deshacerlas (solo primeros intentos). */
    private val saved = ArrayDeque<AnswerRecord>()
    private var busy = false
    /** Pegatinas que ya había al empezar. */
    private var achievementsBefore: Set<Achievement> = emptySet()

    init {
        viewModelScope.launch {
            achievementsBefore = repository.achievements(mode)
            val due = ReviewSession.order(repository.dueCards(mode, deckId), box = { it.box })
            cards = due.associateBy { it.id }
            val limit = if (mode == Mode.KIDS) ReviewSession.KIDS_SESSION_LIMIT else null
            session = ReviewSession(due.map { it.id }, limit)
            publish(revealed = false)
        }
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    fun toggleTypeAnswer() {
        val on = !_state.value.typeAnswer
        prefs.setTypeAnswer(mode, on)
        _state.value = _state.value.copy(typeAnswer = on)
    }

    /** Comprueba lo escrito y muestra la respuesta. */
    fun check(typed: String) {
        val card = _state.value.card ?: return
        _state.value = _state.value.copy(
            revealed = true,
            typedText = typed.trim(),
            typedCorrect = AnswerCheck.matches(typed, card.back),
        )
    }

    fun answer(correct: Boolean) {
        if (busy || !::session.isInitialized || session.isFinished) return
        busy = true
        viewModelScope.launch {
            val step = session.answer(correct)
            // Los reintentos de una fallada no cambian la caja: ya volvió a la caja 1.
            if (step.firstAttempt) {
                repository.answer(step.cardId, mode, correct)?.let(saved::addLast)
            }
            publish(revealed = false)
            if (session.isFinished) {
                val earned = repository.achievements(mode) - achievementsBefore
                _state.value = _state.value.copy(newAchievements = Achievement.entries.filter { it in earned })
            }
            busy = false
        }
    }

    /** Deshace la última respuesta y vuelve a mostrar esa tarjeta con la respuesta a la vista. */
    fun undo() {
        if (busy || !::session.isInitialized || !session.canUndo) return
        busy = true
        viewModelScope.launch {
            val step = session.undo()
            if (step != null && step.firstAttempt) {
                saved.removeLastOrNull()?.let { repository.undoAnswer(it) }
            }
            publish(revealed = true)
            busy = false
        }
    }

    private fun publish(revealed: Boolean) {
        _state.value = ReviewUiState(
            loading = false,
            card = session.current?.let { cards[it] },
            revealed = revealed,
            position = session.answered + 1,
            queueSize = session.queueSize,
            total = session.total,
            isRetry = session.isRetry,
            canUndo = session.canUndo,
            finished = session.isFinished,
            correct = session.correct,
            wrong = session.wrong,
            stars = session.stars(),
            typeAnswer = prefs.typeAnswer(mode),
        )
    }

    class Factory(
        private val repository: LeitnerRepository,
        private val mode: Mode,
        private val deckId: Long,
        private val prefs: AppPrefs,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel(repository, mode, deckId, prefs) as T
    }
}
