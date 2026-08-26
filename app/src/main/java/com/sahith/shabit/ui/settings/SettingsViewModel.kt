package com.sahith.shabit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sahith.shabit.data.ArchivedHabit
import com.sahith.shabit.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @param activeCount how many of the four slots are taken right now. Restoring needs one
 *   free, so this is what decides whether the Restore buttons are live.
 * @param loading true only before the first read comes back, so "nothing archived yet"
 *   does not flash over a list that does have habits in it.
 */
data class SettingsUiState(
    val archived: List<ArchivedHabit> = emptyList(),
    val activeCount: Int = 0,
    val loading: Boolean = false,
) {
    val canRestore: Boolean get() = activeCount < HabitRepository.MAX_ACTIVE_HABITS
}

/**
 * The settings screen: the archived habits and what can be done with them.
 *
 * Both the list and the active count come from the database as flows, so archiving a habit
 * on the dashboard and then opening this screen — or restoring one here and going back —
 * needs no refresh anywhere.
 */
class SettingsViewModel(private val repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(repository.archivedHabits(), repository.activeHabits()) { archived, active ->
            SettingsUiState(archived = archived, activeCount = active.size)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(loading = true),
        )

    /**
     * Silent when the cap refuses it: the button that got here is only enabled while a
     * slot is free, and the reason is already on screen beside it.
     */
    fun restore(habitId: Long) {
        viewModelScope.launch { repository.restore(habitId) }
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
                SettingsViewModel(HabitRepository.getInstance(checkNotNull(application)))
            }
        }
    }
}
