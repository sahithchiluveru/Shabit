package com.sahith.shabit.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Both columns are stored as integers so SQLite can compare and range-scan them directly.
 *
 * A [LocalDate] is an epoch *day* and never a timestamp — the whole 4am rule depends on a
 * date carrying no time and no zone, so that a row written at 00:15 in one timezone still
 * means the same square of the grid after a flight. An [Instant] is an epoch milli, used
 * only for `archivedAt`, where an absolute moment is genuinely what we mean.
 */
object Converters {
    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()
}
