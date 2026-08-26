package com.sahith.shabit.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One card's worth of state: the habit and every tile it has filled. */
data class HabitCardState(
    val habit: Habit,
    val completions: Set<LocalDate>,
)

/**
 * @param loading true only before the first read comes back, so the empty state does not
 *   flash on top of a database that does have habits in it.
 */
data class DashboardUiState(
    val today: LocalDate,
    val habits: List<HabitCardState> = emptyList(),
    val loading: Boolean = false,
) {
    val atCap: Boolean get() = habits.size >= HabitRepository.MAX_ACTIVE_HABITS

    val canAddHabit: Boolean get() = !loading && !atCap
}

class DashboardViewModel(private val repository: HabitRepository) : ViewModel() {
    /**
     * Today is state, not a constant: leave the app open past 4am and every card's anchor
     * column moves. Rather than run a timer, [refreshToday] is called when the screen
     * resumes, which covers the way this actually happens — phone locked overnight,
     * unlocked the next morning.
     */
    private val today = MutableStateFlow(repository.today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> =
        combine(repository.activeHabits(), today, ::Pair)
            .flatMapLatest { (habits, day) ->
                if (habits.isEmpty()) {
                    flowOf(DashboardUiState(today = day))
                } else {
                    // One completions flow per habit, recombined into a single state, so a
                    // tap on any card re-renders that card and nothing else.
                    combine(
                        habits.map { habit ->
                            repository.completions(habit.id).map { HabitCardState(habit, it) }
                        },
                    ) { cards ->
                        DashboardUiState(today = day, habits = cards.toList())
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DashboardUiState(today = today.value, loading = true),
            )

    fun refreshToday() {
        today.value = repository.today()
    }

    fun toggle(habitId: Long, date: LocalDate) {
        viewModelScope.launch { repository.toggle(habitId, date) }
    }

    fun archive(habitId: Long) {
        viewModelScope.launch { repository.archive(habitId) }
    }

    fun delete(habitId: Long) {
        viewModelScope.launch { repository.delete(habitId) }
    }

    companion object {
        /** Long enough to survive a rotation without tearing down the database flows. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                DashboardViewModel(HabitRepository.getInstance(checkNotNull(application)))
            }
        }
    }
}
