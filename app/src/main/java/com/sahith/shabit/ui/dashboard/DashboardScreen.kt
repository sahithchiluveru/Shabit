package com.sahith.shabit.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sahith.shabit.R
import com.sahith.shabit.data.Habit
import com.sahith.shabit.ui.theme.ShabitTheme
import java.time.LocalDate

/** Test handle for the top-bar add button. */
const val ADD_HABIT_TAG = "add-habit"

/**
 * The main screen: every active habit as a card, newest week on the right.
 *
 * Stateless on purpose — it takes a [DashboardUiState] and reports taps upwards, so the
 * previews below exercise exactly the composition the app runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggle: (habitId: Long, date: LocalDate) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (habitId: Long) -> Unit,
    onArchiveHabit: (habitId: Long) -> Unit,
    onDeleteHabit: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(
                        onClick = onAddHabit,
                        enabled = state.canAddHabit,
                        modifier = Modifier.testTag(ADD_HABIT_TAG),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = stringResource(R.string.add_habit),
                        )
                    }
                },
            )
        },
    ) { insets ->
        when {
            state.loading -> Unit
            state.habits.isEmpty() -> EmptyState(
                onAddHabit = onAddHabit,
                modifier = Modifier.padding(insets),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(insets),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = state.habits, key = { it.habit.id }) { card ->
                    HabitCard(
                        habit = card.habit,
                        completions = card.completions,
                        today = state.today,
                        onToggle = { date -> onToggle(card.habit.id, date) },
                        onEdit = { onEditHabit(card.habit.id) },
                        onArchive = { onArchiveHabit(card.habit.id) },
                        onDelete = { onDeleteHabit(card.habit.id) },
                    )
                }
                if (state.atCap) {
                    // The disabled + button says "no" without saying why. The cap is a
                    // deliberate design choice, so explain the way out of it rather than
                    // leaving a dead control on screen.
                    item {
                        Text(
                            text = stringResource(R.string.habit_cap_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddHabit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddHabit) { Text(stringResource(R.string.add_habit)) }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 3, 15)

private fun previewCard(
    id: Long,
    name: String,
    description: String,
    iconKey: String,
    colorHex: String,
    weeks: Long,
): HabitCardState {
    val createdDate = PreviewToday.minusWeeks(weeks)
    return HabitCardState(
        habit = Habit(
            id = id,
            name = name,
            description = description,
            iconKey = iconKey,
            colorHex = colorHex,
            createdDate = createdDate,
        ),
        completions = sampleCompletions(createdDate, PreviewToday),
    )
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun DashboardPreview() {
    ShabitTheme {
        DashboardScreen(
            state = DashboardUiState(
                today = PreviewToday,
                habits = listOf(
                    previewCard(1, "Sport", "Weightlifting, running or similar", "ic_dumbbell", "#FBBF24", 30),
                    previewCard(2, "Coding", "Learn to code every day", "ic_code", "#EC4899", 22),
                    previewCard(3, "Reading", "Read for at least 15 minutes", "ic_book_open", "#A78BFA", 12),
                    previewCard(4, "Daily Walk", "Go for a walk outside", "ic_leaf", "#22C55E", 5),
                ),
            ),
            onToggle = { _, _ -> },
            onAddHabit = {},
            onEditHabit = {},
            onArchiveHabit = {},
            onDeleteHabit = {},
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun EmptyDashboardPreview() {
    ShabitTheme {
        DashboardScreen(
            state = DashboardUiState(today = PreviewToday),
            onToggle = { _, _ -> },
            onAddHabit = {},
            onEditHabit = {},
            onArchiveHabit = {},
            onDeleteHabit = {},
        )
    }
}
