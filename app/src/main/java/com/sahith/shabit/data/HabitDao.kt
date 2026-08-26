package com.sahith.shabit.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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

    /** Archived habits, most recently archived first — that is the one you came looking for. */
    @Query("SELECT * FROM habits WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC, id ASC")
    fun archivedHabits(): Flow<List<Habit>>

    @Upsert
    suspend fun upsert(habit: Habit)

    /** Archive with a non-null moment, restore with null. */
    @Query("UPDATE habits SET archivedAt = :archivedAt WHERE id = :habitId")
    suspend fun setArchivedAt(habitId: Long, archivedAt: Instant?)

    /** Cascades to `completions` via the foreign key on [Completion]. */
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun delete(habitId: Long)

    @Query("SELECT COUNT(*) FROM habits WHERE archivedAt IS NULL")
    suspend fun activeCount(): Int
}
