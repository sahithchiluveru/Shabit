package com.sahith.shabit.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Runs against a real in-memory Room database rather than a fake, because the parts most
 * worth testing here — the toggle transaction and the cascade — are SQLite's behaviour,
 * not ours. Robolectric is what lets that happen in a plain unit test, so CI covers it
 * without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
class HabitRepositoryTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val clock: Clock =
        Clock.fixed(LocalDateTime.parse("2026-03-15T10:00:00").atZone(zone).toInstant(), zone)
    private val today: LocalDate = LocalDate.of(2026, 3, 15)

    private lateinit var database: ShabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShabitDatabase::class.java)
            .build()
        repository = HabitRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `today follows the 4am rule through the injected clock`() {
        assertEquals(today, repository.today())
    }

    @Test
    fun `toggle fills an empty tile`() = runTest {
        val id = addHabit("Gym")

        repository.toggle(id, today)

        assertEquals(setOf(today), repository.completions(id).first())
    }

    @Test
    fun `toggling twice on the same date leaves no row`() = runTest {
        val id = addHabit("Gym")

        repository.toggle(id, today)
        repository.toggle(id, today)

        assertEquals(emptySet<LocalDate>(), repository.completions(id).first())
    }

    @Test
    fun `toggling one date does not disturb another`() = runTest {
        val id = addHabit("Gym")
        val yesterday = today.minusDays(1)

        repository.toggle(id, today)
        repository.toggle(id, yesterday)
        repository.toggle(id, today)

        assertEquals(setOf(yesterday), repository.completions(id).first())
    }

    @Test
    fun `completions are scoped to their habit`() = runTest {
        val gym = addHabit("Gym")
        val reading = addHabit("Reading")

        repository.toggle(gym, today)

        assertEquals(setOf(today), repository.completions(gym).first())
        assertEquals(emptySet<LocalDate>(), repository.completions(reading).first())
    }

    @Test
    fun `deleting a habit cascades its completions`() = runTest {
        val id = addHabit("Gym")
        repository.toggle(id, today)
        repository.toggle(id, today.minusDays(1))

        repository.delete(id)

        assertEquals(emptySet<LocalDate>(), repository.completions(id).first())
        assertTrue(repository.activeHabits().first().isEmpty())
    }

    @Test
    fun `active habits are ordered by creation date`() = runTest {
        addHabit("Reading", createdDate = today)
        addHabit("Gym", createdDate = today.minusDays(10))

        assertEquals(
            listOf("Gym", "Reading"),
            repository.activeHabits().first().map(Habit::name),
        )
    }

    @Test
    fun `archiving frees a slot and keeps the grid`() = runTest {
        val id = addHabit("Smoking")
        repository.toggle(id, today)

        repository.archive(id)

        assertTrue(repository.activeHabits().first().isEmpty())
        assertEquals(0, repository.activeCount())
        val archived = repository.archivedHabits().first().single()
        assertEquals("Smoking", archived.name)
        assertNotNull(archived.archivedAt)
        assertEquals(setOf(today), repository.completions(id).first())
    }

    @Test
    fun `restoring returns a habit to the active list`() = runTest {
        val id = addHabit("Smoking")
        repository.archive(id)

        repository.restore(id)

        assertEquals(1, repository.activeCount())
        assertTrue(repository.archivedHabits().first().isEmpty())
        assertNull(repository.activeHabits().first().single().archivedAt)
    }

    @Test
    fun `active count ignores archived habits`() = runTest {
        addHabit("Gym")
        addHabit("Reading")
        val third = addHabit("Water")
        repository.archive(third)

        assertEquals(2, repository.activeCount())
    }

    @Test
    fun `upsert on an existing id edits in place`() = runTest {
        val id = addHabit("Gym")

        repository.upsert(
            repository.activeHabits().first().single().copy(name = "Gym & swim"),
        )

        val habits = repository.activeHabits().first()
        assertEquals(1, habits.size)
        assertEquals("Gym & swim", habits.single().name)
        assertEquals(id, habits.single().id)
    }

    private suspend fun addHabit(name: String, createdDate: LocalDate = today): Long {
        repository.upsert(
            Habit(
                name = name,
                description = "",
                iconKey = "ic_dumbbell",
                colorHex = "#FBBF24",
                createdDate = createdDate,
            ),
        )
        return repository.activeHabits().first().first { it.name == name }.id
    }
}
