package com.sahith.shabit.data

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The hour the habit day rolls over. See [habitToday]. */
private const val ROLLOVER_HOUR = 4L

/**
 * Today, as the *user* means it: the day rolls over at 4am, not midnight.
 *
 * Logging a workout at 00:15 should fill last night's tile — nobody thinks of 1am as the
 * next day. Shifting the wall clock back four hours and dropping the time does exactly
 * that: everything from 04:00 to 03:59 the following morning is one habit day.
 *
 * This lives here and **nowhere else**. No screen, widget, ViewModel or DAO should ever
 * call `LocalDate.now()` — one of them will eventually forget the shift and write a row
 * into tomorrow. Callers with a repository should use [HabitRepository.today], which
 * threads that repository's clock through.
 */
fun habitToday(clock: Clock = Clock.systemDefaultZone()): LocalDate =
    LocalDateTime.now(clock).minusHours(ROLLOVER_HOUR).toLocalDate()

/**
 * Which habit day an absolute moment fell on, under the same 4am rule as [habitToday].
 *
 * Only `Habit.archivedAt` is stored as a moment, so this is only for showing when a habit
 * was archived. It needs a [zone] because an [Instant] has none of its own — pass the zone
 * of the clock that produced it.
 */
fun habitDate(instant: Instant, zone: ZoneId): LocalDate =
    LocalDateTime.ofInstant(instant, zone).minusHours(ROLLOVER_HOUR).toLocalDate()

/**
 * The next moment the habit day rolls over, strictly after now.
 *
 * The widget has to redraw at 4am whether or not anyone opens anything: a tile that was
 * "today" stops being today, and every grid's anchor column moves one along. This is the
 * arithmetic behind that alarm, kept here with the rest of the rollover rule and away from
 * `AlarmManager` so it can be tested.
 */
fun nextRollover(clock: Clock = Clock.systemDefaultZone()): Instant {
    val now = LocalDateTime.now(clock)
    val todayRollover = now.toLocalDate().atTime(ROLLOVER_HOUR.toInt(), 0)
    val next = if (now.isBefore(todayRollover)) todayRollover else todayRollover.plusDays(1)
    return next.atZone(clock.zone).toInstant()
}
