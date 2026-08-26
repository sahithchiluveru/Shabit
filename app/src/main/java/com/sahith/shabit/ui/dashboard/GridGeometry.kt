package com.sahith.shabit.ui.dashboard

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

/**
 * Where every tile in a habit's grid goes, as plain date arithmetic.
 *
 * The grid is weekday-aligned: a row is a weekday and a column is one calendar week, so
 * "I never manage Mondays" is visible as a pale stripe. That is the whole reason not to
 * simply flow days left-to-right.
 *
 * This is deliberately free of Compose so it can be unit-tested exhaustively, and is
 * reused by the widget, which draws the same grid onto a Canvas.
 */

/** Monday … Sunday. */
const val GRID_ROWS = 7

/** The Monday of [date]'s week — the identity of the column [date] falls in. */
fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * How many week columns a habit's own history spans: from the week it was created through
 * the week containing [today], inclusive.
 */
fun weekColumnCount(createdDate: LocalDate, today: LocalDate): Int {
    if (today.isBefore(createdDate)) return 1
    val weeks = ChronoUnit.WEEKS.between(weekStart(createdDate), weekStart(today))
    return weeks.toInt() + 1
}

/**
 * How many columns it takes to cover [widthDp] with tiles of [tileDp] separated by
 * [gapDp] — rounded up, so the grid runs edge to edge rather than stopping short with a
 * strip of bare card showing.
 */
fun columnsToFill(widthDp: Float, tileDp: Float, gapDp: Float): Int {
    val step = tileDp + gapDp
    if (step <= 0f) return 1
    return ceil((widthDp + gapDp) / step).toInt().coerceAtLeast(1)
}

/**
 * The grid's real column count: enough to fill the card from the day the habit is made,
 * and never fewer than the habit's own history once that grows past the card.
 *
 * A brand-new habit therefore looks like a habit tracker rather than a blank card with a
 * single square on it — the whole block is there from day one, waiting to be filled in.
 * [columnsToFill] is also the scroll extent for a young habit, which is what bounds how
 * far back a day can be filled in.
 */
fun gridColumnCount(createdDate: LocalDate, today: LocalDate, columnsToFill: Int): Int =
    maxOf(weekColumnCount(createdDate, today), columnsToFill)

/**
 * The Monday of the column at [index], counted **from the right**: index 0 is the week
 * containing [today]. The grid is a `reverseLayout` LazyRow so that today is the anchor
 * and history extends off to the left, which is both the natural reading and what keeps
 * the newest week on screen without measuring the whole history first.
 */
fun weekStartAt(index: Int, today: LocalDate): LocalDate =
    weekStart(today).minusWeeks(index.toLong())

/** The date in a column starting at [weekStart] on [row] (0 = Monday). */
fun cellDate(weekStart: LocalDate, row: Int): LocalDate = weekStart.plusDays(row.toLong())

/**
 * What a single cell should draw.
 *
 * Every cell is a tile — the grid is a solid block of the habit's colour, faded where the
 * day is not done. Days before the habit was created are ordinary [EMPTY] days: you can
 * fill one in, because remembering on Wednesday that you also ran on Monday is the normal
 * case, and a tile that looks identical to its neighbour but silently swallows the tap is
 * worse than one that works.
 *
 * [FUTURE] is the exception. It is drawn fainter still and takes no tap: a day that has
 * not arrived cannot be done.
 */
enum class TileState { EMPTY, FILLED, FUTURE }

fun tileState(date: LocalDate, today: LocalDate, completed: Boolean): TileState = when {
    date.isAfter(today) -> TileState.FUTURE
    completed -> TileState.FILLED
    else -> TileState.EMPTY
}
