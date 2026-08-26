package com.sahith.shabit.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sahith.shabit.R
import com.sahith.shabit.ui.EMPTY_TILE_ALPHA
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TileSize = 13.dp
private val TileGap = 3.dp
private val TileCorner = 3.dp

/** Test and accessibility handle for one tile, e.g. `tile-2026-03-15`. */
fun tileTag(date: LocalDate): String = "tile-$date"

/**
 * A habit's completion grid: one column per week, seven rows of weekdays, today at the
 * right-hand edge.
 *
 * `reverseLayout` is what anchors today rather than a scroll-to-end after measuring, so a
 * three-year history costs nothing on first frame — the LazyRow only ever composes the
 * columns actually on screen.
 */
@Composable
fun HabitGrid(
    createdDate: LocalDate,
    today: LocalDate,
    completions: Set<LocalDate>,
    color: Color,
    onToggle: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnCount = remember(createdDate, today) { weekColumnCount(createdDate, today) }
    LazyRow(
        modifier = modifier,
        reverseLayout = true,
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        items(columnCount) { index ->
            WeekColumn(
                weekStart = weekStartAt(index, today),
                createdDate = createdDate,
                today = today,
                completions = completions,
                color = color,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun WeekColumn(
    weekStart: LocalDate,
    createdDate: LocalDate,
    today: LocalDate,
    completions: Set<LocalDate>,
    color: Color,
    onToggle: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
        repeat(GRID_ROWS) { row ->
            val date = cellDate(weekStart, row)
            Tile(
                date = date,
                state = tileState(date, createdDate, today, completions.contains(date)),
                color = color,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun Tile(
    date: LocalDate,
    state: TileState,
    color: Color,
    onToggle: (LocalDate) -> Unit,
) {
    val box = Modifier
        .testTag(tileTag(date))
        .size(TileSize)
        .clip(RoundedCornerShape(TileCorner))

    if (state == TileState.OUTSIDE) {
        // Bare card background: before the habit existed, or after today. It gets no click
        // handler at all, so a tap falls through to the row it is scrolling in.
        Box(modifier = box)
    } else {
        val label = stringResource(
            if (state == TileState.FILLED) R.string.tile_done else R.string.tile_not_done,
            date.format(DateFormat),
        )
        Box(
            modifier = box
                .background(
                    if (state == TileState.FILLED) color else color.copy(alpha = EMPTY_TILE_ALPHA),
                )
                .clickable(
                    // A 13dp tile is smaller than a ripple, so the default indication would
                    // bleed over its neighbours. Semantics stay, for TalkBack and for tests.
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggle(date) },
                )
                .semantics { contentDescription = label },
        )
    }
}

private val DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
