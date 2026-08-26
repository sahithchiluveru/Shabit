package com.sahith.shabit.ui.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.ui.theme.ShabitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * The tap rules, against the real composition. Everything here is stateless — the card and
 * the screen take a state and report taps — so these need no database.
 *
 * "Today" is deliberately a Wednesday: it leaves real future cells inside the current
 * column, which is the only place an untappable future tile can actually be observed.
 */
@RunWith(RobolectricTestRunner::class)
class DashboardComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate.of(2026, 3, 11)

    private val habit = Habit(
        id = 7,
        name = "Sport",
        description = "Weightlifting",
        iconKey = "ic_dumbbell",
        colorHex = "#FBBF24",
        createdDate = today.minusWeeks(3),
    )

    private fun showCard(
        completions: Set<LocalDate> = emptySet(),
        onToggle: (LocalDate) -> Unit = {},
    ) {
        compose.setContent {
            ShabitTheme {
                HabitCard(
                    habit = habit,
                    completions = completions,
                    today = today,
                    onToggle = onToggle,
                    onEdit = {},
                    onArchive = {},
                    onDelete = {},
                )
            }
        }
    }

    private fun showDashboard(habitCount: Int) {
        val habits = List(habitCount) { index ->
            HabitCardState(habit.copy(id = index + 1L, name = "Habit $index"), emptySet())
        }
        compose.setContent {
            ShabitTheme {
                DashboardScreen(
                    state = DashboardUiState(today = today, habits = habits),
                    onToggle = { _, _ -> },
                    onAddHabit = {},
                    onEditHabit = {},
                    onArchiveHabit = {},
                    onDeleteHabit = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun `tapping a past tile toggles that date`() {
        val toggled = mutableListOf<LocalDate>()
        showCard(onToggle = { toggled += it })

        compose.onNodeWithTag(tileTag(today.minusDays(1))).performClick()

        assertEquals(listOf(today.minusDays(1)), toggled)
    }

    @Test
    fun `tapping today's tile toggles today`() {
        val toggled = mutableListOf<LocalDate>()
        showCard(onToggle = { toggled += it })

        compose.onNodeWithTag(tileTag(today)).performClick()

        assertEquals(listOf(today), toggled)
    }

    @Test
    fun `future tiles do not respond to taps`() {
        showCard()

        compose.onNodeWithTag(tileTag(today.plusDays(1))).assertHasNoClickAction()
        compose.onNodeWithTag(tileTag(today.plusDays(2))).assertHasNoClickAction()
    }

    @Test
    fun `cells before the habit existed do not respond to taps`() {
        showCard()

        compose.onNodeWithTag(tileTag(habit.createdDate.minusDays(1))).assertHasNoClickAction()
        compose.onNodeWithTag(tileTag(habit.createdDate)).assertHasClickAction()
    }

    @Test
    fun `the check button toggles today, exactly as the tile does`() {
        val toggled = mutableListOf<LocalDate>()
        showCard(onToggle = { toggled += it })

        compose.onNodeWithTag(checkButtonTag(habit.id)).performClick()

        assertEquals(listOf(today), toggled)
    }

    @Test
    fun `adding is offered below the cap`() {
        showDashboard(HabitRepository.MAX_ACTIVE_HABITS - 1)

        compose.onNodeWithTag(ADD_HABIT_TAG).assertIsEnabled()
    }

    @Test
    fun `adding is refused at exactly four active habits`() {
        showDashboard(HabitRepository.MAX_ACTIVE_HABITS)

        compose.onNodeWithTag(ADD_HABIT_TAG).assertIsNotEnabled()
    }

    @Test
    fun `deleting names the habit and counts the tiles that would go with it`() {
        showCard(completions = setOf(today, today.minusDays(1), today.minusDays(4)))

        compose.onNodeWithContentDescription("Habit options").performClick()
        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("Delete Sport?").assertIsDisplayed()
        compose
            .onNodeWithText(
                "This permanently removes 3 completed days. This cannot be undone. " +
                    "Archive instead if you want to keep the history.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun `settings is always reachable, cap or no cap`() {
        showDashboard(HabitRepository.MAX_ACTIVE_HABITS)

        compose.onNodeWithTag(SETTINGS_TAG).assertIsEnabled()
    }
}
