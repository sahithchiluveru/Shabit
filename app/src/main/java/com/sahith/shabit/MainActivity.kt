package com.sahith.shabit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahith.shabit.ui.dashboard.DashboardScreen
import com.sahith.shabit.ui.dashboard.DashboardViewModel
import com.sahith.shabit.ui.theme.ShabitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShabitTheme {
                Dashboard()
            }
        }
    }
}

@Composable
private fun Dashboard(
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
        // Creating and editing a habit is #5; until that screen exists there is nowhere
        // for these to go. The cap logic behind the + button is live either way.
        onAddHabit = {},
        onEditHabit = {},
        onArchiveHabit = viewModel::archive,
        onDeleteHabit = viewModel::delete,
    )
}
