package com.sahith.shabit.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The 4am rule. These four cases are the whole contract: 03:59 still belongs to yesterday,
 * 04:00 starts today. Everything else in the app derives its date from here.
 */
class HabitClockTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")

    private fun clockAt(wallClock: String): Clock =
        Clock.fixed(LocalDateTime.parse(wallClock).atZone(zone).toInstant(), zone)

    @Test
    fun `just before midnight is still that day`() {
        assertEquals(
            LocalDate.of(2026, 3, 14),
            habitToday(clockAt("2026-03-14T23:59:00")),
        )
    }

    @Test
    fun `a quarter past midnight still fills yesterday's tile`() {
        assertEquals(
            LocalDate.of(2026, 3, 14),
            habitToday(clockAt("2026-03-15T00:15:00")),
        )
    }

    @Test
    fun `one minute before rollover is still yesterday`() {
        assertEquals(
            LocalDate.of(2026, 3, 14),
            habitToday(clockAt("2026-03-15T03:59:00")),
        )
    }

    @Test
    fun `rollover at 4am starts the new habit day`() {
        assertEquals(
            LocalDate.of(2026, 3, 15),
            habitToday(clockAt("2026-03-15T04:00:00")),
        )
    }
}
