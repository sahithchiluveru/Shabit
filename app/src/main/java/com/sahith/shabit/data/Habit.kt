package com.sahith.shabit.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * One tracked habit. At most four may be active at a time (see [HabitRepository.activeCount]);
 * archiving frees a slot without destroying the grid, which is the whole point of having
 * [archivedAt] rather than deleting.
 *
 * @param iconKey the *name* of a vector drawable, e.g. "ic_dumbbell", not a resource id —
 *   resource ids are not stable across builds and this row outlives the install.
 * @param colorHex one of the 21 fixed palette swatches, "#RRGGBB".
 * @param createdDate the habit day (4am rule) the habit was created on; also the sort key
 *   and the left edge of its grid.
 * @param archivedAt null means active. Non-null is the moment it was archived.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val iconKey: String,
    val colorHex: String,
    val createdDate: LocalDate,
    val archivedAt: Instant? = null,
)
