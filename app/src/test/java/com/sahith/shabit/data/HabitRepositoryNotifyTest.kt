package com.sahith.shabit.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
 * Every write tells the widget.
 *
 * Glance only collects the repository's flows while its own session is alive, so a change
 * made in the app while the home screen is elsewhere leaves the last `RemoteViews` on
 * display until something pushes an update. That something is this callback, and the
 * acceptance criterion it carries — "checking in the app updates the widget" — is one
 * forgotten call away from being false. Hence a test per write.
 */
@RunWith(RobolectricTestRunner::class)
class HabitRepositoryNotifyTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val clock: Clock =
        Clock.fixed(LocalDateTime.parse("2026-03-15T10:00:00").atZone(zone).toInstant(), zone)
    private val today: LocalDate = LocalDate.of(2026, 3, 15)

    private var notifications = 0
    private lateinit var database: ShabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShabitDatabase::class.java)
            .build()
        repository = HabitRepository(database, clock) { notifications++ }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `creating notifies`() = runTest {
        addHabit("Gym")

        assertEquals(1, notifications)
    }

    @Test
    fun `a create refused by the cap still notifies, harmlessly`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }
        notifications = 0

        repository.create(newHabit("One too many"))

        // Redrawing what has not changed costs a redraw. Not redrawing what has changed
        // costs a wrong widget, so the callback is unconditional on purpose.
        assertEquals(1, notifications)
    }

    @Test
    fun `toggling notifies`() = runTest {
        val id = addHabit("Gym")
        notifications = 0

        repository.toggle(id, today)
        repository.toggle(id, today)

        assertEquals(2, notifications)
    }

    @Test
    fun `editing notifies`() = runTest {
        val id = addHabit("Gym")
        notifications = 0

        repository.updateDetails(id, "Swimming", "", "ic_bike", "#22D3EE")

        assertEquals(1, notifications)
    }

    @Test
    fun `archiving notifies`() = runTest {
        val id = addHabit("Gym")
        notifications = 0

        repository.archive(id)

        assertEquals(1, notifications)
    }

    @Test
    fun `restoring notifies`() = runTest {
        val id = addHabit("Gym")
        repository.archive(id)
        notifications = 0

        repository.restore(id)

        assertEquals(1, notifications)
    }

    @Test
    fun `deleting notifies`() = runTest {
        val id = addHabit("Gym")
        notifications = 0

        repository.delete(id)

        assertEquals(1, notifications)
    }

    @Test
    fun `reading notifies nothing`() = runTest {
        val id = addHabit("Gym")
        notifications = 0

        repository.activeHabits().first()
        repository.archivedHabits().first()
        repository.completions(id).first()
        repository.habit(id)
        repository.activeCount()

        assertEquals(0, notifications)
    }

    private fun newHabit(name: String) = Habit(
        name = name,
        description = "",
        iconKey = "ic_dumbbell",
        colorHex = "#FBBF24",
        createdDate = today,
    )

    private suspend fun addHabit(name: String): Long = checkNotNull(repository.create(newHabit(name)))
}
