package com.sahith.shabit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sahith.shabit.R
import com.sahith.shabit.data.ArchivedHabit
import com.sahith.shabit.data.Habit
import com.sahith.shabit.ui.DeleteHabitDialog
import com.sahith.shabit.ui.EMPTY_TILE_ALPHA
import com.sahith.shabit.ui.HabitIcons
import com.sahith.shabit.ui.habitColor
import com.sahith.shabit.ui.theme.ShabitTheme
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test handle for the "nothing archived yet" copy. */
const val ARCHIVED_EMPTY_TAG = "archived-empty"

/** Test handle for the note explaining why Restore is unavailable. */
const val RESTORE_BLOCKED_TAG = "restore-blocked"

/** Test handle for one habit's Restore button, e.g. `restore-7`. */
fun restoreTag(habitId: Long): String = "restore-$habitId"

/** Test handle for one archived habit's Delete button, e.g. `delete-archived-7`. */
fun deleteArchivedTag(habitId: Long): String = "delete-archived-$habitId"

/**
 * Settings: the archived habits, and the two things that can happen to them.
 *
 * Archiving is what makes the four-habit cap survivable, so this screen is really the
 * other half of that cap — the place a grid waits while something else uses its slot.
 *
 * Stateless, like every other Shabit screen.
 *
 * @param versionName shown under About; read from the installed package so it cannot
 *   drift from what is actually running.
 * @param onOpenRepo opens the repository in a browser. Held as a callback rather than an
 *   Intent so this file stays testable and Android-free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    onRestore: (habitId: Long) -> Unit,
    onDelete: (habitId: Long) -> Unit,
    onOpenRepo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { insets ->
        if (state.loading) return@Scaffold

        LazyColumn(
            modifier = Modifier.padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(stringResource(R.string.archived_habits)) }

            if (state.archived.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.archived_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(ARCHIVED_EMPTY_TAG),
                    )
                }
            } else {
                if (!state.canRestore) {
                    // Four disabled buttons with no explanation is the same dead end the
                    // dashboard's + button would be. Say why, and say the way out.
                    item {
                        Text(
                            text = stringResource(R.string.restore_blocked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(RESTORE_BLOCKED_TAG),
                        )
                    }
                }
                items(items = state.archived, key = { it.habit.id }) { archived ->
                    ArchivedHabitRow(
                        archived = archived,
                        canRestore = state.canRestore,
                        onRestore = { onRestore(archived.habit.id) },
                        onDelete = { onDelete(archived.habit.id) },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.about)) }
            item { AboutSection(versionName = versionName, onOpenRepo = onOpenRepo) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ArchivedHabitRow(
    archived: ArchivedHabit,
    canRestore: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    val habit = archived.habit
    val color = habitColor(habit.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = EMPTY_TILE_ALPHA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(HabitIcons.resolve(habit.iconKey)),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.archived_span,
                            formatDate(habit.createdDate),
                            formatDate(archived.archivedDate),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.completed_days,
                            archived.completionCount,
                            archived.completionCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.testTag(deleteArchivedTag(habit.id)),
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(
                    onClick = onRestore,
                    enabled = canRestore,
                    modifier = Modifier.testTag(restoreTag(habit.id)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rotate_ccw),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    if (confirmingDelete) {
        DeleteHabitDialog(
            habitName = habit.name,
            completionCount = archived.completionCount,
            // This habit is already archived — there is no gentler option left to offer.
            offerArchive = false,
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun AboutSection(versionName: String, onOpenRepo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Text(
                text = stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenRepo)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.source_code),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_external_link),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatDate(date: LocalDate): String = date.format(DATE_FORMAT)

private fun previewArchived(
    id: Long,
    name: String,
    iconKey: String,
    colorHex: String,
    completionCount: Int,
) = ArchivedHabit(
    habit = Habit(
        id = id,
        name = name,
        description = "",
        iconKey = iconKey,
        colorHex = colorHex,
        createdDate = LocalDate.of(2025, 6, 14),
        archivedAt = Instant.parse("2026-08-20T09:00:00Z"),
    ),
    completionCount = completionCount,
    archivedDate = LocalDate.of(2026, 8, 20),
)

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsPreview() {
    ShabitTheme {
        SettingsScreen(
            state = SettingsUiState(
                archived = listOf(
                    previewArchived(1, "Smoking", "ic_cigarette_off", "#F87171", 412),
                    previewArchived(2, "Guitar", "ic_music", "#38BDF8", 27),
                ),
                activeCount = 2,
            ),
            versionName = "0.1.0",
            onRestore = {},
            onDelete = {},
            onOpenRepo = {},
            onBack = {},
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsAtCapPreview() {
    ShabitTheme {
        SettingsScreen(
            state = SettingsUiState(
                archived = listOf(previewArchived(1, "Smoking", "ic_cigarette_off", "#F87171", 412)),
                activeCount = 4,
            ),
            versionName = "0.1.0",
            onRestore = {},
            onDelete = {},
            onOpenRepo = {},
            onBack = {},
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun EmptySettingsPreview() {
    ShabitTheme {
        SettingsScreen(
            state = SettingsUiState(),
            versionName = "0.1.0",
            onRestore = {},
            onDelete = {},
            onOpenRepo = {},
            onBack = {},
        )
    }
}
