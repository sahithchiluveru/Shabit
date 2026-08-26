package com.sahith.shabit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The single on-device store. The app process and the widget's Glance worker both open it,
 * so [getInstance] hands back one instance per process rather than letting each caller
 * build its own — two `RoomDatabase` objects over one file is how you get a stale widget.
 *
 * Foreign keys are on by default in Room, which is what makes deleting a habit take its
 * completions with it.
 */
@Database(
    entities = [Habit::class, Completion::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ShabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun completionDao(): CompletionDao

    companion object {
        private const val NAME = "shabit.db"

        @Volatile
        private var instance: ShabitDatabase? = null

        fun getInstance(context: Context): ShabitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, ShabitDatabase::class.java, NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
