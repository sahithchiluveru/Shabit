package com.sahith.shabit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

/**
 * A filled tile.
 *
 * The presence of a row *is* the completion — there is deliberately no boolean column and
 * no "unchecked" row. Unchecking deletes. That keeps the table proportional to work done
 * rather than to days elapsed, and makes "is this tile filled?" a primary-key lookup.
 *
 * [date] is a habit day, not a calendar day: it always comes from [habitToday] or from a
 * tile the user tapped, never from `LocalDate.now()`.
 */
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class Completion(
    val habitId: Long,
    val date: LocalDate,
)
