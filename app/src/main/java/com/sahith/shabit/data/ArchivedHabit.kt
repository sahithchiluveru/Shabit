package com.sahith.shabit.data

import androidx.room.Embedded
import java.time.LocalDate

/**
 * An archived habit and the size of the grid it is holding onto.
 *
 * @param completionCount how many tiles are filled. Zero is a real answer.
 * @param archivedDate the habit day it was archived on. With [Habit.createdDate] that is
 *   the span the grid covers, which is what the settings list shows instead of drawing
 *   four hundred tiles nobody asked for.
 */
data class ArchivedHabit(
    val habit: Habit,
    val completionCount: Int,
    val archivedDate: LocalDate,
)

/**
 * What the query returns, before [HabitRepository] resolves `archivedAt` — an absolute
 * moment — into a habit day, which it can only do with its own clock's zone.
 *
 * The count comes from the query rather than from loading every completion: the list only
 * ever shows the number.
 *
 * Public only because [HabitDao] is; nothing outside the data package should name it.
 */
data class ArchivedHabitRow(
    @Embedded val habit: Habit,
    val completionCount: Int,
)
