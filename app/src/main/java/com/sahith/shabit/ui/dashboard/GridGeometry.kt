package com.sahith.shabit.ui.dashboard

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Where every tile in a habit's grid goes, as plain date arithmetic.
 *
 * The grid is weekday-aligned: a row is a weekday and a column is one calendar week, so
 * "I never manage Mondays" is visible as a pale stripe. That is the whole reason not to
 * simply flow days left-to-right.
 *
 * This is deliberately free of Compose so it can be unit-tested exhaustively, and reused
 * by the widget in #7, which draws the same grid through RemoteViews.
 */

/** Monday … Sunday. */
const val GRID_ROWS = 7

/** The Monday of [date]'s week — the identity of the column [date] falls in. */
fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * How many week columns a habit's grid has: from the week it was created through the week
 * containing [today], inclusive. That is the scroll extent — scrolling back reaches
 * `createdDate`'s week and stops.
 */
fun weekColumnCount(createdDate: LocalDate, today: LocalDate): Int {
    if (today.isBefore(createdDate)) return 1
    val weeks = ChronoUnit.WEEKS.between(weekStart(createdDate), weekStart(today))
    return weeks.toInt() + 1
}

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
 * [OUTSIDE] is not a tile at all — it is bare card background. It covers both ends: the
 * ragged first column, where the habit did not exist yet, and the rest of the current
 * week, which has not happened. Neither is tappable; you cannot complete a habit on a day
 * before you had it, or on a day that has not arrived.
 */
enum class TileState { OUTSIDE, EMPTY, FILLED }

fun tileState(
    date: LocalDate,
    createdDate: LocalDate,
    today: LocalDate,
    completed: Boolean,
): TileState = when {
    date.isBefore(createdDate) || date.isAfter(today) -> TileState.OUTSIDE
    completed -> TileState.FILLED
    else -> TileState.EMPTY
}
