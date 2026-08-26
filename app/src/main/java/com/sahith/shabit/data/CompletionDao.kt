package com.sahith.shabit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Room's view of the `completions` table. The toggle semantics live in
 * [HabitRepository.toggle], which needs a transaction around the read and the write.
 */
@Dao
interface CompletionDao {
    @Query("SELECT date FROM completions WHERE habitId = :habitId")
    fun dates(habitId: Long): Flow<List<LocalDate>>

    @Query("SELECT EXISTS(SELECT 1 FROM completions WHERE habitId = :habitId AND date = :date)")
    suspend fun exists(habitId: Long, date: LocalDate): Boolean

    /**
     * IGNORE, not REPLACE: a second insert of the same tile is a no-op rather than a
     * delete-and-reinsert, which would churn the invalidation tracker and the widget.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: Completion)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: LocalDate)
}
