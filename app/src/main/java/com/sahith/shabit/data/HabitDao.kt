package com.sahith.shabit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Room's view of the `habits` table. Everything outside the data package talks to
 * [HabitRepository] instead — the DAOs are an implementation detail of it.
 */
@Dao
interface HabitDao {
    /** Active habits, oldest first. `id` breaks ties so the order is stable within a day. */
    @Query("SELECT * FROM habits WHERE archivedAt IS NULL ORDER BY createdDate ASC, id ASC")
    fun activeHabits(): Flow<List<Habit>>

    /**
     * Archived habits, most recently archived first — that is the one you came looking for
     * — each with the number of tiles its grid is holding.
     *
     * A LEFT JOIN so a habit archived before it was ever ticked still appears, with a
     * count of zero rather than no row at all.
     */
    @Query(
        """
        SELECT habits.*, COUNT(completions.habitId) AS completionCount
        FROM habits LEFT JOIN completions ON completions.habitId = habits.id
        WHERE habits.archivedAt IS NOT NULL
        GROUP BY habits.id
        ORDER BY habits.archivedAt DESC, habits.id ASC
        """,
    )
    fun archivedHabits(): Flow<List<ArchivedHabitRow>>

    /** A one-shot read for the edit screen, which loads a habit once into a form. */
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun habit(habitId: Long): Habit?

    @Insert
    suspend fun insert(habit: Habit): Long

    /**
     * The four user-editable fields, and deliberately only those.
     *
     * Editing must not disturb `createdDate` (the left edge of the grid) or `archivedAt`,
     * so rather than trust callers to copy a row faithfully, the statement itself has no
     * way to reach them.
     */
    @Query(
        """
        UPDATE habits
        SET name = :name, description = :description, iconKey = :iconKey, colorHex = :colorHex
        WHERE id = :habitId
        """,
    )
    suspend fun updateDetails(
        habitId: Long,
        name: String,
        description: String,
        iconKey: String,
        colorHex: String,
    )

    /** Archive with a non-null moment, restore with null. */
    @Query("UPDATE habits SET archivedAt = :archivedAt WHERE id = :habitId")
    suspend fun setArchivedAt(habitId: Long, archivedAt: Instant?)

    /** Cascades to `completions` via the foreign key on [Completion]. */
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun delete(habitId: Long)

    @Query("SELECT COUNT(*) FROM habits WHERE archivedAt IS NULL")
    suspend fun activeCount(): Int
}
