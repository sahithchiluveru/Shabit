package com.sahith.shabit.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sahith.shabit.R
import com.sahith.shabit.ui.HabitIcons
import com.sahith.shabit.ui.HabitPalette
import com.sahith.shabit.ui.habitColor
import com.sahith.shabit.ui.theme.ShabitTheme

/** Test handle for the tick that commits the form. */
const val SAVE_HABIT_TAG = "save-habit"

/** Test handle for the name field. */
const val NAME_FIELD_TAG = "habit-name"

/** Test handle for the description field. */
const val DESCRIPTION_FIELD_TAG = "habit-description"

/** Test handle for one icon in the picker, e.g. `icon-ic_dumbbell`. */
fun iconTag(iconKey: String): String = "icon-$iconKey"

/** Test handle for one swatch in the palette, e.g. `color-#FBBF24`. */
fun colorTag(colorHex: String): String = "color-$colorHex"

private const val ICONS_PER_ROW = 5
private const val SWATCHES_PER_ROW = 7

/**
 * Create a habit, or change one. Four fields — name, description, icon, colour — and no
 * more: streak goals and reminders are out of scope for v1 (decision 13 in #1), which is
 * why this screen is shorter than the reference shot it is drawn from.
 *
 * Stateless, like the dashboard: it renders a [HabitEditorUiState] and reports edits up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    state: HabitEditorUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.editing) R.string.edit_habit_title else R.string.new_habit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = state.canSave,
                        modifier = Modifier.testTag(SAVE_HABIT_TAG),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(R.string.save),
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

        Column(
            modifier = Modifier
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.rejectedAtCap) {
                Text(
                    text = stringResource(R.string.habit_cap_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FieldLabel(stringResource(R.string.field_name))
            FormField(
                value = state.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.field_name_hint),
                imeAction = ImeAction.Next,
                tag = NAME_FIELD_TAG,
            )

            FieldLabel(stringResource(R.string.field_description))
            FormField(
                value = state.description,
                onValueChange = onDescriptionChange,
                placeholder = stringResource(R.string.field_description_hint),
                imeAction = ImeAction.Done,
                tag = DESCRIPTION_FIELD_TAG,
            )

            FieldLabel(stringResource(R.string.field_icon))
            IconPicker(selected = state.iconKey, onSelect = onIconChange)

            FieldLabel(stringResource(R.string.field_color))
            ColorPicker(selected = state.colorHex, onSelect = onColorChange)
        }
    }
}

/**
 * Closes the screen once the save has landed. Kept beside the screen rather than inside it
 * so the composable above stays a pure function of its state.
 */
@Composable
fun FinishEffect(finished: Boolean, onFinished: () -> Unit) {
    LaunchedEffect(finished) {
        if (finished) onFinished()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Both fields are single-line: a description that wraps would push the pickers off screen,
 * and `singleLine` is also what keeps a newline out of the stored value.
 */
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    tag: String,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
    )
}

@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    PickerGrid(items = HabitIcons.keys, perRow = ICONS_PER_ROW) { iconKey ->
        SelectableTile(
            selected = iconKey == selected,
            tag = iconTag(iconKey),
            onClick = { onSelect(iconKey) },
        ) {
            Icon(
                painter = painterResource(HabitIcons.resolve(iconKey)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ColorPicker(selected: String, onSelect: (String) -> Unit) {
    PickerGrid(items = HabitPalette, perRow = SWATCHES_PER_ROW) { colorHex ->
        SelectableTile(
            selected = colorHex == selected,
            tag = colorTag(colorHex),
            onClick = { onSelect(colorHex) },
            size = SWATCH_TILE_SIZE,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(habitColor(colorHex), RoundedCornerShape(7.dp)),
            )
        }
    }
}

/**
 * Fixed rows rather than a lazy grid: both pickers hold a known handful of cells, and a
 * lazy grid nested in a scrolling column has no height the two can agree on.
 */
@Composable
private fun <T> PickerGrid(
    items: List<T>,
    perRow: Int,
    content: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { content(it) }
            }
        }
    }
}

private val ICON_TILE_SIZE = 52.dp
private val SWATCH_TILE_SIZE = 36.dp

/** A picker cell. Selection is a ring, exactly as in the reference shot. */
@Composable
private fun SelectableTile(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    size: Dp = ICON_TILE_SIZE,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(size)
            .background(MaterialTheme.colorScheme.surface, shape)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
                } else {
                    Modifier
                },
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun HabitEditorPreview() {
    ShabitTheme {
        HabitEditorScreen(
            state = HabitEditorUiState(
                editing = true,
                name = "Reading",
                description = "Read for at least 15 minutes",
                iconKey = "ic_book_open",
                colorHex = "#A78BFA",
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onIconChange = {},
            onColorChange = {},
            onSave = {},
            onClose = {},
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp")
@Composable
private fun NewHabitPreview() {
    ShabitTheme {
        HabitEditorScreen(
            state = HabitEditorUiState(editing = false),
            onNameChange = {},
            onDescriptionChange = {},
            onIconChange = {},
            onColorChange = {},
            onSave = {},
            onClose = {},
        )
    }
}
