package com.sahith.shabit.ui.dashboard

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
 * The dashboard's state assembly over a real database: several habits, each with its own
 * completions flow, folded into one state that has to update the instant a tile is toggled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val clock: Clock =
        Clock.fixed(LocalDateTime.parse("2026-03-15T10:00:00").atZone(zone).toInstant(), zone)
    private val today: LocalDate = LocalDate.of(2026, 3, 15)

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var database: ShabitDatabase
    private lateinit var repository: HabitRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        database = Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShabitDatabase::class.java)
            .build()
        repository = HabitRepository(database, clock)
        viewModel = DashboardViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `an empty database offers a free slot`() = runTest(dispatcher) {
        val state = collectUiState().first { !it.loading }

        assertTrue(state.habits.isEmpty())
        assertFalse(state.atCap)
        assertTrue(state.canAddHabit)
        assertEquals(today, state.today)
    }

    @Test
    fun `habits arrive as cards carrying their own completions`() = runTest(dispatcher) {
        val gym = addHabit("Gym")
        addHabit("Reading")
        repository.toggle(gym, today)
        repository.toggle(gym, today.minusDays(2))

        val state = collectUiState().first { it.habits.size == 2 }

        val gymCard = state.habits.single { it.habit.id == gym }
        assertEquals(setOf(today, today.minusDays(2)), gymCard.completions)
        assertEquals(emptySet<LocalDate>(), state.habits.single { it.habit.id != gym }.completions)
    }

    @Test
    fun `toggling is reflected in the state`() = runTest(dispatcher) {
        val gym = addHabit("Gym")
        val uiState = collectUiState()
        uiState.first { it.habits.isNotEmpty() }

        viewModel.toggle(gym, today)
        val filled = uiState.first { it.habits.first().completions.isNotEmpty() }
        assertEquals(setOf(today), filled.habits.first().completions)

        viewModel.toggle(gym, today)
        val cleared = uiState.first { it.habits.first().completions.isEmpty() }
        assertTrue(cleared.habits.first().completions.isEmpty())
    }

    @Test
    fun `the fourth habit closes the cap`() = runTest(dispatcher) {
        repeat(HabitRepository.MAX_ACTIVE_HABITS - 1) { addHabit("Habit $it") }
        val uiState = collectUiState()

        assertTrue(uiState.first { it.habits.size == 3 }.canAddHabit)

        addHabit("Fourth")
        val full = uiState.first { it.habits.size == HabitRepository.MAX_ACTIVE_HABITS }
        assertTrue(full.atCap)
        assertFalse(full.canAddHabit)
    }

    @Test
    fun `archiving frees a slot on the dashboard`() = runTest(dispatcher) {
        val ids = List(HabitRepository.MAX_ACTIVE_HABITS) { addHabit("Habit $it") }
        val uiState = collectUiState()
        uiState.first { it.habits.size == HabitRepository.MAX_ACTIVE_HABITS }

        viewModel.archive(ids.first())

        val state = uiState.first { it.habits.size == 3 }
        assertTrue(state.canAddHabit)
        assertTrue(state.habits.none { it.habit.id == ids.first() })
    }

    @Test
    fun `deleting removes the card`() = runTest(dispatcher) {
        val gym = addHabit("Gym")
        val uiState = collectUiState()
        uiState.first { it.habits.isNotEmpty() }

        viewModel.delete(gym)

        assertTrue(uiState.first { it.habits.isEmpty() }.habits.isEmpty())
    }

    /**
     * `uiState` is a `WhileSubscribed` flow, so nothing runs until something collects it.
     * `backgroundScope` keeps that collector alive for the test and cancels it afterwards.
     */
    private fun TestScope.collectUiState() =
        viewModel.uiState.also { state -> backgroundScope.launch { state.collect {} } }

    private suspend fun addHabit(name: String): Long = checkNotNull(
        repository.create(
            Habit(
                name = name,
                description = "",
                iconKey = "ic_dumbbell",
                colorHex = "#FBBF24",
                createdDate = today.minusWeeks(4),
            ),
        ),
    )
}
