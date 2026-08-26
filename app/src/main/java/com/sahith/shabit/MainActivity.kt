package com.sahith.shabit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahith.shabit.ui.dashboard.DashboardScreen
import com.sahith.shabit.ui.dashboard.DashboardViewModel
import com.sahith.shabit.ui.editor.FinishEffect
import com.sahith.shabit.ui.editor.HabitEditorScreen
import com.sahith.shabit.ui.editor.HabitEditorViewModel
import com.sahith.shabit.ui.settings.SettingsScreen
import com.sahith.shabit.ui.settings.SettingsViewModel
import com.sahith.shabit.ui.theme.ShabitTheme

/**
 * Stands in for "create a new habit" in [ShabitApp]'s editor target. Room ids autogenerate
 * from 1, so 0 can never collide with a real habit.
 */
private const val NEW_HABIT = 0L

/** Where the About section's link goes. */
private const val REPO_URL = "https://github.com/sahithchiluveru/Shabit"

/**
 * Shabit's three screens. An enum is `Serializable`, which is all `rememberSaveable` needs
 * to put it in a Bundle — no custom Saver, and no navigation library for what is still a
 * dashboard with two things hanging off it.
 */
private enum class Screen { Dashboard, Editor, Settings }

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

@Composable
private fun ShabitApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }
    var editorTarget by rememberSaveable { mutableStateOf(NEW_HABIT) }
    // Which *visit* to the editor this is. See [HabitEditor] for why a target id alone is
    // not enough to identify one.
    var editorVisit by rememberSaveable { mutableStateOf(0) }
    val toDashboard = { screen = Screen.Dashboard }
    val openEditor = { habitId: Long ->
        editorTarget = habitId
        editorVisit++
        screen = Screen.Editor
    }

    when (screen) {
        Screen.Dashboard -> Dashboard(
            onAddHabit = { openEditor(NEW_HABIT) },
            onEditHabit = openEditor,
            onOpenSettings = { screen = Screen.Settings },
        )

        Screen.Editor -> {
            BackHandler(onBack = toDashboard)
            HabitEditor(
                habitId = editorTarget.takeIf { it != NEW_HABIT },
                visit = editorVisit,
                onClose = toDashboard,
            )
        }

        Screen.Settings -> {
            BackHandler(onBack = toDashboard)
            Settings(onBack = toDashboard)
        }
    }
}

@Composable
private fun Dashboard(
    onAddHabit: () -> Unit,
    onEditHabit: (habitId: Long) -> Unit,
    onOpenSettings: () -> Unit,
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
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun HabitEditor(habitId: Long?, visit: Int, onClose: () -> Unit) {
    // Keyed by target *and* visit. `viewModel()` here resolves against the activity's
    // store, which outlives this composition, so a key of the target alone hands the
    // second visit back the first visit's finished form — and `FinishEffect` would close
    // the screen again the instant it opened, which is what a dead + button looks like.
    // The visit counter is `rememberSaveable`, so a rotation is still the same visit and
    // keeps whatever has been typed. The cost is that last visit's view model sits in the
    // store until the activity goes: four strings and two booleans, once per visit.
    val viewModel: HabitEditorViewModel = viewModel(
        key = "editor-$visit-$habitId",
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

@Composable
private fun Settings(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) {
        // From the installed package rather than BuildConfig: generating BuildConfig means
        // a Java source set, and the whole app is otherwise Kotlin. This is the same
        // string, read from the APK that is actually running.
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    SettingsScreen(
        state = state,
        versionName = versionName,
        onRestore = viewModel::restore,
        onDelete = viewModel::delete,
        onOpenRepo = {
            // Shabit holds no INTERNET permission and wants none — handing the URL to
            // whatever browser is installed is the whole of its networking.
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
            } catch (_: ActivityNotFoundException) {
                // A device with no browser at all. Nothing to do here but not crash.
            }
        },
        onBack = onBack,
    )
}
