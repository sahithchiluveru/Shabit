package com.sahith.shabit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahith.shabit.ui.dashboard.DashboardScreen
import com.sahith.shabit.ui.dashboard.DashboardViewModel
import com.sahith.shabit.ui.editor.FinishEffect
import com.sahith.shabit.ui.editor.HabitEditorScreen
import com.sahith.shabit.ui.editor.HabitEditorViewModel
import com.sahith.shabit.ui.theme.ShabitTheme

/**
 * Stands in for "create a new habit" in [ShabitApp]'s editor target. Room ids autogenerate
 * from 1, so 0 can never collide with a real habit — which is what lets the whole
 * navigation state be one nullable Long that `rememberSaveable` can put in a Bundle.
 */
private const val NEW_HABIT = 0L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShabitTheme {
                ShabitApp()
            }
        }
    }
}

/**
 * Two screens and one way between them, so this is the whole of Shabit's navigation:
 * null shows the dashboard, anything else shows the editor. A navigation library would be
 * a dependency and a graph for a single edge.
 */
@Composable
private fun ShabitApp() {
    var editorTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = editorTarget

    if (target == null) {
        Dashboard(
            onAddHabit = { editorTarget = NEW_HABIT },
            onEditHabit = { habitId -> editorTarget = habitId },
        )
    } else {
        BackHandler { editorTarget = null }
        HabitEditor(
            habitId = target.takeIf { it != NEW_HABIT },
            onClose = { editorTarget = null },
        )
    }
}

@Composable
private fun Dashboard(
    onAddHabit: () -> Unit,
    onEditHabit: (habitId: Long) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Coming back to the app after 4am has to move every grid's anchor column along.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }

    DashboardScreen(
        state = state,
        onToggle = viewModel::toggle,
        onAddHabit = onAddHabit,
        onEditHabit = onEditHabit,
        onArchiveHabit = viewModel::archive,
        onDeleteHabit = viewModel::delete,
    )
}

@Composable
private fun HabitEditor(habitId: Long?, onClose: () -> Unit) {
    // Keyed by target so that closing one habit's form and opening another's starts from
    // that habit's row rather than the last one's leftovers.
    val viewModel: HabitEditorViewModel = viewModel(
        key = "editor-$habitId",
        factory = HabitEditorViewModel.factory(habitId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FinishEffect(finished = state.finished, onFinished = onClose)

    HabitEditorScreen(
        state = state,
        onNameChange = viewModel::setName,
        onDescriptionChange = viewModel::setDescription,
        onIconChange = viewModel::setIconKey,
        onColorChange = viewModel::setColorHex,
        onSave = viewModel::save,
        onClose = onClose,
    )
}
