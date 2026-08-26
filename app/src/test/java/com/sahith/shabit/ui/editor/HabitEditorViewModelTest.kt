package com.sahith.shabit.ui.editor

import androidx.room.Room
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.data.ShabitDatabase
import com.sahith.shabit.ui.DEFAULT_COLOR_HEX
import com.sahith.shabit.ui.HabitIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
 * The add/edit form over a real database. What is worth testing here is what the form is
 * allowed to write: a blank name never, a fifth habit never, and — when editing — nothing
 * beyond the four fields on screen.
 *
 * Loading and saving are both `viewModelScope` work over a real Room database, so the
 * tests wait for the state that says it landed rather than reading `uiState.value` the
 * instant after asking for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HabitEditorViewModelTest {
    private val zone: ZoneId = ZoneId.of("Europe/London")
    private val clock: Clock =
        Clock.fixed(LocalDateTime.parse("2026-03-15T10:00:00").atZone(zone).toInstant(), zone)
    private val today: LocalDate = LocalDate.of(2026, 3, 15)

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var database: ShabitDatabase
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        database = Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), ShabitDatabase::class.java)
            .build()
        repository = HabitRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new habit starts on the first icon and the first colour`() {
        val state = creating().uiState.value

        assertFalse(state.editing)
        assertFalse(state.loading)
        assertEquals(HabitIcons.DEFAULT_ICON_KEY, state.iconKey)
        assertEquals(DEFAULT_COLOR_HEX, state.colorHex)
    }

    @Test
    fun `saving is refused while the name is blank`() {
        val viewModel = creating()

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setName("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setName("Gym")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `calling save with a blank name writes nothing`() = runTest {
        val viewModel = creating()
        viewModel.setName("  ")

        viewModel.save()

        assertTrue(repository.activeHabits().first().isEmpty())
        assertFalse(viewModel.uiState.value.finished)
    }

    @Test
    fun `creating writes the form, trimmed, dated today`() = runTest {
        val viewModel = creating()
        viewModel.setName("  Gym  ")
        viewModel.setDescription("  Lift things  ")
        viewModel.setIconKey("ic_bike")
        viewModel.setColorHex("#22D3EE")

        viewModel.save()
        assertTrue(viewModel.awaitSettled().finished)

        val habit = repository.activeHabits().first().single()
        assertEquals("Gym", habit.name)
        assertEquals("Lift things", habit.description)
        assertEquals("ic_bike", habit.iconKey)
        assertEquals("#22D3EE", habit.colorHex)
        assertEquals(today, habit.createdDate)
    }

    @Test
    fun `creating a fifth habit is refused even though the form allowed it`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { seed("Habit $it") }
        val viewModel = creating()
        viewModel.setName("One too many")

        viewModel.save()

        val settled = viewModel.awaitSettled()
        assertTrue(settled.rejectedAtCap)
        assertFalse(settled.finished)
        assertEquals(HabitRepository.MAX_ACTIVE_HABITS, repository.activeCount())
    }

    @Test
    fun `editing loads the habit into the form`() = runTest {
        val id = seed("Gym", description = "Lift things", iconKey = "ic_bike", colorHex = "#22D3EE")

        val state = editing(id).awaitLoaded()

        assertTrue(state.editing)
        assertEquals("Gym", state.name)
        assertEquals("Lift things", state.description)
        assertEquals("ic_bike", state.iconKey)
        assertEquals("#22D3EE", state.colorHex)
    }

    @Test
    fun `editing preserves the created date and every completion`() = runTest {
        val created = today.minusWeeks(9)
        val id = seed("Gym", createdDate = created)
        repository.toggle(id, today)
        repository.toggle(id, today.minusDays(5))

        val viewModel = editing(id)
        viewModel.awaitLoaded()
        viewModel.setName("Swimming")
        viewModel.setColorHex("#22D3EE")
        viewModel.save()
        assertTrue(viewModel.awaitSettled().finished)

        val habit = repository.activeHabits().first().single()
        assertEquals("Swimming", habit.name)
        assertEquals("#22D3EE", habit.colorHex)
        assertEquals(created, habit.createdDate)
        assertEquals(setOf(today, today.minusDays(5)), repository.completions(id).first())
    }

    @Test
    fun `editing a fourth habit is never refused by the cap`() = runTest {
        repeat(HabitRepository.MAX_ACTIVE_HABITS) { seed("Habit $it") }
        val id = repository.activeHabits().first().last().id

        val viewModel = editing(id)
        viewModel.awaitLoaded()
        viewModel.setName("Renamed")
        viewModel.save()

        val settled = viewModel.awaitSettled()
        assertTrue(settled.finished)
        assertFalse(settled.rejectedAtCap)
    }

    @Test
    fun `editing a habit that has since been deleted just closes`() = runTest {
        val id = seed("Gym")
        repository.delete(id)

        assertTrue(editing(id).awaitSettled().finished)
    }

    private fun creating() = HabitEditorViewModel(repository, habitId = null)

    private fun editing(habitId: Long) = HabitEditorViewModel(repository, habitId)

    private suspend fun HabitEditorViewModel.awaitLoaded(): HabitEditorUiState =
        uiState.first { !it.loading }

    private suspend fun HabitEditorViewModel.awaitSettled(): HabitEditorUiState =
        uiState.first { it.finished || it.rejectedAtCap }

    private suspend fun seed(
        name: String,
        description: String = "",
        iconKey: String = "ic_dumbbell",
        colorHex: String = "#FBBF24",
        createdDate: LocalDate = today,
    ): Long = checkNotNull(
        repository.create(
            Habit(
                name = name,
                description = description,
                iconKey = iconKey,
                colorHex = colorHex,
                createdDate = createdDate,
            ),
        ),
    )
}
