package com.sahith.shabit.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.ui.DEFAULT_COLOR_HEX
import com.sahith.shabit.ui.HabitIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The add/edit form.
 *
 * @param editing false when this is a new habit, true when an existing one is being changed.
 *   The two differ in more than the title: creating writes a whole row and can be refused
 *   by the cap, editing writes four fields and never can be.
 * @param loading true while an existing habit is being read; the form is not editable yet.
 * @param finished set once the save has landed, which is the screen's cue to close.
 * @param rejectedAtCap set when a create was refused because four habits are already
 *   active. Only reachable if the dashboard's disabled + button was somehow bypassed.
 */
data class HabitEditorUiState(
    val editing: Boolean,
    val name: String = "",
    val description: String = "",
    val iconKey: String = HabitIcons.DEFAULT_ICON_KEY,
    val colorHex: String = DEFAULT_COLOR_HEX,
    val loading: Boolean = false,
    val finished: Boolean = false,
    val rejectedAtCap: Boolean = false,
) {
    /** A habit needs a name. Whitespace is not one — see [HabitEditorViewModel.save]. */
    val canSave: Boolean get() = name.isNotBlank() && !loading && !finished
}

/**
 * @param habitId the habit being edited, or null to create one.
 */
class HabitEditorViewModel(
    private val repository: HabitRepository,
    private val habitId: Long?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HabitEditorUiState(editing = habitId != null, loading = habitId != null),
    )
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch {
                // A habit deleted from the dashboard while this screen was opening leaves
                // nothing to edit. Closing is the honest answer; there is no row to fix.
                val habit = repository.habit(habitId)
                _uiState.update {
                    if (habit == null) {
                        it.copy(loading = false, finished = true)
                    } else {
                        it.copy(
                            name = habit.name,
                            description = habit.description,
                            iconKey = habit.iconKey,
                            colorHex = habit.colorHex,
                            loading = false,
                        )
                    }
                }
            }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun setDescription(description: String) = _uiState.update { it.copy(description = description) }

    fun setIconKey(iconKey: String) = _uiState.update { it.copy(iconKey = iconKey) }

    fun setColorHex(colorHex: String) = _uiState.update { it.copy(colorHex = colorHex) }

    /**
     * Writes the form and sets [HabitEditorUiState.finished], or [HabitEditorUiState.rejectedAtCap]
     * if creating was refused.
     *
     * Both text fields are trimmed on the way in: a name that is only spaces is not a name,
     * and a description with a trailing space is the same description.
     */
    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val name = state.name.trim()
        val description = state.description.trim()

        viewModelScope.launch {
            if (habitId == null) {
                val created = repository.create(
                    Habit(
                        name = name,
                        description = description,
                        iconKey = state.iconKey,
                        colorHex = state.colorHex,
                        createdDate = repository.today(),
                    ),
                )
                _uiState.update {
                    if (created == null) it.copy(rejectedAtCap = true) else it.copy(finished = true)
                }
            } else {
                // Only these four fields move. The dashboard and — from #7 — the widget both
                // read the row through a Room flow, so both redraw as soon as this returns.
                repository.updateDetails(habitId, name, description, state.iconKey, state.colorHex)
                _uiState.update { it.copy(finished = true) }
            }
        }
    }

    companion object {
        fun factory(habitId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                HabitEditorViewModel(
                    HabitRepository.getInstance(checkNotNull(application)),
                    habitId,
                )
            }
        }
    }
}
