package com.sahith.shabit.data

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

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
