package com.sahith.shabit.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("Smoking", archived.habit.name)
        assertNotNull(archived.habit.archivedAt)
        assertEquals(setOf(today), repository.completions(id).first())
    }

    @Test
    fun `an archived habit reports the size and span of the grid it is keeping`() = runTest {
        val created = today.minusWeeks(4)
        val id = addHabit("Smoking", createdDate = created)
        repository.toggle(id, today)
        repository.toggle(id, today.minusDays(1))
        repository.toggle(id, today.minusDays(2))

        repository.archive(id)

        val archived = repository.archivedHabits().first().single()
        assertEquals(3, archived.completionCount)
        assertEquals(created, archived.habit.createdDate)
        assertEquals(today, archived.archivedDate)
    }

    @Test
    fun `an archived habit with no completions still appears, counting zero`() = runTest {
        repository.archive(addHabit("Smoking"))

        assertEquals(0, repository.archivedHabits().first().single().completionCount)
    }

    @Test
    fun `restoring returns a habit to the active list`() = runTest {
        val id = addHabit("Smoking")
        repository.archive(id)

        assertTrue(repository.restore(id))

        assertEquals(1, repository.activeCount())
        assertTrue(repository.archivedHabits().first().isEmpty())
        assertNull(repository.activeHabits().first().single().archivedAt)
    }

    @Test
    fun `restoring is refused when all four slots are taken`() = runTest {
        val id = addHabit("Smoking")
        repository.archive(id)
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }

        assertFalse(repository.restore(id))

        assertEquals(HabitRepository.MAX_ACTIVE_HABITS, repository.activeCount())
        assertEquals("Smoking", repository.archivedHabits().first().single().habit.name)
    }

    @Test
    fun `an archive and restore round trip keeps every completion`() = runTest {
        val id = addHabit("Smoking", createdDate = today.minusWeeks(4))
        val filled = setOf(today, today.minusDays(3), today.minusDays(11))
        filled.forEach { repository.toggle(id, it) }

        repository.archive(id)
        assertTrue(repository.restore(id))

        assertEquals(filled, repository.completions(id).first())
        assertEquals(today.minusWeeks(4), repository.activeHabits().first().single().createdDate)
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
    fun `creating is refused once four habits are active`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }

        assertNull(repository.create(newHabit("One too many")))
        assertEquals(HabitRepository.MAX_ACTIVE_HABITS, repository.activeCount())
    }

    @Test
    fun `archiving frees a slot for a new habit`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }
        repository.archive(repository.activeHabits().first().first().id)

        assertNotNull(repository.create(newHabit("Replacement")))
    }

    @Test
    fun `updating details edits in place`() = runTest {
        val id = addHabit("Gym")

        repository.updateDetails(id, "Gym & swim", "Twice a week", "ic_bike", "#22D3EE")

        val habit = repository.activeHabits().first().single()
        assertEquals(id, habit.id)
        assertEquals("Gym & swim", habit.name)
        assertEquals("Twice a week", habit.description)
        assertEquals("ic_bike", habit.iconKey)
        assertEquals("#22D3EE", habit.colorHex)
    }

    @Test
    fun `updating details keeps the created date and the completions`() = runTest {
        val created = today.minusWeeks(6)
        val id = addHabit("Gym", createdDate = created)
        repository.toggle(id, today)
        repository.toggle(id, today.minusDays(3))

        repository.updateDetails(id, "Gym", "", "ic_bike", "#22D3EE")

        assertEquals(created, repository.activeHabits().first().single().createdDate)
        assertEquals(setOf(today, today.minusDays(3)), repository.completions(id).first())
    }

    @Test
    fun `habit reads one row back by id`() = runTest {
        val id = addHabit("Gym")

        assertEquals("Gym", repository.habit(id)?.name)
        assertNull(repository.habit(id + 1))
    }

    private fun newHabit(name: String, createdDate: LocalDate = today) = Habit(
        name = name,
        description = "",
        iconKey = "ic_dumbbell",
        colorHex = "#FBBF24",
        createdDate = createdDate,
    )

    private suspend fun addHabit(name: String, createdDate: LocalDate = today): Long =
        checkNotNull(repository.create(newHabit(name, createdDate)))
}
