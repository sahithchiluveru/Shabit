package com.sahith.shabit.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sahith.shabit.R

/** Test handle for the button that actually deletes. */
const val DELETE_CONFIRM_TAG = "delete-confirm"

/** Test handle for the button that backs out. */
const val DELETE_CANCEL_TAG = "delete-cancel"

/**
 * The one irreversible action in Shabit, and the only place it is confirmed.
 *
 * There is no export and no undo (decision 6 in #1), so the dialog says what will actually
 * be lost — the real number of filled tiles, not "its whole grid" — and says out loud that
 * it cannot be undone. [offerArchive] adds the way out for a habit that is still active;
 * one already archived has taken it.
 */
@Composable
fun DeleteHabitDialog(
    habitName: String,
    completionCount: Int,
    offerArchive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val message = resources.getQuantityString(
        R.plurals.delete_habit_message,
        completionCount,
        completionCount,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_habit_title, habitName)) },
        text = {
            Text(
                if (offerArchive) {
                    message + " " + stringResource(R.string.delete_habit_archive_hint)
                } else {
                    message
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(DELETE_CONFIRM_TAG)) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(DELETE_CANCEL_TAG)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
