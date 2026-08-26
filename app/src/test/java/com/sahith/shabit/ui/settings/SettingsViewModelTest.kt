package com.sahith.shabit.ui.settings

import androidx.room.Room
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.data.ShabitDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * The settings screen over a real database: what is archived, whether a slot is free, and
 * what restoring and deleting actually do to the grid.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val clock: Clock =
        Clock.fixed(LocalDateTime.parse("2026-03-15T10:00:00").atZone(zone).toInstant(), zone)
    private val today: LocalDate = LocalDate.of(2026, 3, 15)

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var database: ShabitDatabase
    private lateinit var repository: HabitRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        database = Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShabitDatabase::class.java)
            .build()
        repository = HabitRepository(database, clock)
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `nothing archived is an empty list, not a missing one`() = runTest {
        addHabit("Gym")

        val state = collectUiState().first { !it.loading }

        assertTrue(state.archived.isEmpty())
        assertTrue(state.canRestore)
    }

    @Test
    fun `an archived habit appears with the size of its grid`() = runTest {
        val id = addHabit("Smoking")
        repository.toggle(id, today)
        repository.toggle(id, today.minusDays(2))
        repository.archive(id)

        val archived = collectUiState().first { it.archived.isNotEmpty() }.archived.single()

        assertEquals("Smoking", archived.habit.name)
        assertEquals(2, archived.completionCount)
        assertEquals(today, archived.archivedDate)
    }

    @Test
    fun `restoring is offered while a slot is free and refused once four are active`() = runTest {
        repository.archive(addHabit("Smoking"))
        val uiState = collectUiState()

        assertTrue(uiState.first { it.archived.isNotEmpty() }.canRestore)

        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }

        assertFalse(uiState.first { !it.canRestore }.canRestore)
    }

    @Test
    fun `restoring returns the habit and its whole grid to the dashboard`() = runTest {
        val id = addHabit("Smoking", createdDate = today.minusWeeks(20))
        val filled = setOf(today, today.minusDays(4), today.minusWeeks(3))
        filled.forEach { repository.toggle(id, it) }
        repository.archive(id)
        val uiState = collectUiState()
        uiState.first { it.archived.isNotEmpty() }

        viewModel.restore(id)

        assertTrue(uiState.first { it.archived.isEmpty() }.archived.isEmpty())
        val restored = repository.activeHabits().first().single()
        assertEquals("Smoking", restored.name)
        assertEquals(today.minusWeeks(20), restored.createdDate)
        assertEquals(filled, repository.completions(id).first())
    }

    @Test
    fun `restoring at the cap leaves the habit archived`() = runTest {
        val id = addHabit("Smoking")
        repository.archive(id)
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }
        val uiState = collectUiState()
        uiState.first { !it.canRestore }

        viewModel.restore(id)

        assertEquals(1, uiState.value.archived.size)
        assertEquals(HabitRepository.MAX_ACTIVE_HABITS, repository.activeCount())
    }

    @Test
    fun `deleting an archived habit takes its completions with it`() = runTest {
        val id = addHabit("Smoking")
        repository.toggle(id, today)
        repository.archive(id)
        val uiState = collectUiState()
        uiState.first { it.archived.isNotEmpty() }

        viewModel.delete(id)

        assertTrue(uiState.first { it.archived.isEmpty() }.archived.isEmpty())
        assertTrue(repository.completions(id).first().isEmpty())
    }

    @Test
    fun `archiving frees a slot straight away`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }
        val uiState = collectUiState()
        uiState.first { !it.canRestore }

        repository.archive(repository.activeHabits().first().first().id)

        assertTrue(uiState.first { it.canRestore }.canRestore)
        assertEquals(HabitRepository.MAX_ACTIVE_HABITS - 1, uiState.value.activeCount)
    }

    /**
     * `uiState` is a `WhileSubscribed` flow, so nothing runs until something collects it.
     * `backgroundScope` keeps that collector alive for the test and cancels it afterwards.
     */
    private fun TestScope.collectUiState() =
        viewModel.uiState.also { state -> backgroundScope.launch { state.collect {} } }

    private suspend fun addHabit(name: String, createdDate: LocalDate = today): Long =
        checkNotNull(
            repository.create(
                Habit(
                    name = name,
                    description = "",
                    iconKey = "ic_dumbbell",
                    colorHex = "#FBBF24",
                    createdDate = createdDate,
                ),
            ),
        )
}
