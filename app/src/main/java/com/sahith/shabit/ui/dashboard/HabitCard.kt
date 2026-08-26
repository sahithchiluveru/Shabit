package com.sahith.shabit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sahith.shabit.R
import com.sahith.shabit.data.Habit
import com.sahith.shabit.ui.HabitIcons
import com.sahith.shabit.ui.habitColor
import com.sahith.shabit.ui.theme.ShabitTheme
import java.time.LocalDate

/** Test handle for a card's check button, e.g. `check-7`. */
fun checkButtonTag(habitId: Long): String = "check-$habitId"

/**
 * One habit: its identity along the top, its history underneath, today's tap on the right.
 *
 * The check button and today's tile are deliberately the same action — both call
 * `toggle(habitId, today)` — so there is no state in which one says done and the other
 * does not.
 */
@Composable
fun HabitCard(
    habit: Habit,
    completions: Set<LocalDate>,
    today: LocalDate,
    onToggle: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    val color = habitColor(habit.colorHex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            CardHeader(
                habit = habit,
                onEdit = onEdit,
                onArchive = onArchive,
                onDeleteRequested = { confirmingDelete = true },
            )
            Spacer(modifier = Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HabitGrid(
                    createdDate = habit.createdDate,
                    today = today,
                    completions = completions,
                    color = color,
                    onToggle = onToggle,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                CheckButton(
                    habitId = habit.id,
                    done = completions.contains(today),
                    color = color,
                    onClick = { onToggle(today) },
                )
            }
        }
    }

    if (confirmingDelete) {
        DeleteConfirmation(
            habitName = habit.name,
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun CardHeader(
    habit: Habit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(HabitIcons.resolve(habit.iconKey)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
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
            if (habit.description.isNotBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OverflowMenu(
            onEdit = onEdit,
            onArchive = onArchive,
            onDeleteRequested = onDeleteRequested,
        )
    }
}

@Composable
private fun OverflowMenu(
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = stringResource(R.string.habit_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.archive)) },
                onClick = {
                    expanded = false
                    onArchive()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    expanded = false
                    onDeleteRequested()
                },
            )
        }
    }
}

@Composable
private fun CheckButton(
    habitId: Long,
    done: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val base = Modifier
        .testTag(checkButtonTag(habitId))
        .size(48.dp)
        .clip(shape)
    Box(
        modifier = (if (done) base.background(color) else base.border(1.5.dp, color, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = stringResource(
                if (done) R.string.uncheck_today else R.string.check_today,
            ),
            // On a filled button the checkmark sits on the habit's own colour, so it has to
            // be the card's ground rather than that colour again.
            tint = if (done) MaterialTheme.colorScheme.background else color,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DeleteConfirmation(
    habitName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_habit_title, habitName)) },
        text = { Text(stringResource(R.string.delete_habit_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(widthDp = 380, backgroundColor = 0xFF0F172A, showBackground = true)
@Composable
private fun HabitCardPreview() {
    val today = LocalDate.of(2026, 3, 15)
    val habit = Habit(
        id = 1,
        name = "Sport",
        description = "Weightlifting, running or similar",
        iconKey = "ic_dumbbell",
        colorHex = "#FBBF24",
        createdDate = today.minusWeeks(20),
    )
    ShabitTheme {
        HabitCard(
            habit = habit,
            completions = sampleCompletions(habit.createdDate, today),
            today = today,
            onToggle = {},
            onEdit = {},
            onArchive = {},
            onDelete = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** Deterministic scatter, so previews look like a real grid rather than a solid block. */
internal fun sampleCompletions(from: LocalDate, to: LocalDate): Set<LocalDate> =
    generateSequence(from) { it.plusDays(1) }
        .takeWhile { !it.isAfter(to) }
        .filter { (it.toEpochDay() * 2654435761L).countOneBits() % 3 != 0 }
        .toSet()
