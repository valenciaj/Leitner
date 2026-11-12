package es.leitner.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.leitner.app.data.DayActivity
import es.leitner.app.data.LeitnerRepository
import es.leitner.core.Achievement
import es.leitner.core.Mode
import es.leitner.core.Streak
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val streak: Int = 0,
    val totalCards: Int = 0,
    val dueToday: Int = 0,
    val reviewedToday: Int = 0,
    val correctToday: Int = 0,
    /** Tarjetas por caja; el índice 0 es la caja 1. */
    val perBox: List<Int> = emptyList(),
    /** Últimos 7 días, del más antiguo a hoy. */
    val lastWeek: List<DayActivity> = emptyList(),
    val achievements: Set<Achievement> = emptySet(),
)

class StatsViewModel(repository: LeitnerRepository, val mode: Mode) : ViewModel() {

    private val today = repository.today()

    val state: StateFlow<StatsUiState> = combine(
        repository.observeBoxCounts(mode),
        repository.observeConfig(mode),
        repository.observeDueCount(mode),
        repository.observeActivity(mode, days = 7),
        repository.observeReviewDays(mode),
    ) { boxCounts, config, due, activity, reviewDays ->
        val byBox = boxCounts.associate { it.box to it.count }
        val byDay = activity.associateBy { it.day }
        val todayActivity = byDay[today]
        StatsUiState(
            streak = Streak.current(reviewDays.toSet(), today),
            totalCards = boxCounts.sumOf { it.count },
            dueToday = due,
            reviewedToday = todayActivity?.total ?: 0,
            correctToday = todayActivity?.correct ?: 0,
            perBox = (1..config.boxCount).map { byBox[it] ?: 0 },
            lastWeek = (today - 6..today).map { day -> byDay[day] ?: DayActivity(day, 0, 0) },
        )
    }.combine(repository.observeAchievements(mode)) { state, achievements ->
        state.copy(achievements = achievements)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    class Factory(private val repository: LeitnerRepository, private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatsViewModel(repository, mode) as T
    }
}
