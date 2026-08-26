package com.sahith.shabit.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sahith.shabit.data.ArchivedHabit
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.ui.DELETE_CANCEL_TAG
import com.sahith.shabit.ui.DELETE_CONFIRM_TAG
import com.sahith.shabit.ui.theme.ShabitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/**
 * What settings offers, against the real composition: the two destinations for an archived
 * habit, and the reason when one of them is unavailable.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val restored = mutableListOf<Long>()
    private val deleted = mutableListOf<Long>()

    private val smoking = ArchivedHabit(
        habit = Habit(
            id = 7,
            name = "Smoking",
            description = "",
            iconKey = "ic_cigarette_off",
            colorHex = "#F87171",
            createdDate = LocalDate.of(2025, 6, 14),
            archivedAt = Instant.parse("2026-08-20T09:00:00Z"),
        ),
        completionCount = 412,
        archivedDate = LocalDate.of(2026, 8, 20),
    )

    private fun showSettings(
        archived: List<ArchivedHabit> = listOf(smoking),
        activeCount: Int = 1,
    ) {
        compose.setContent {
            ShabitTheme {
                SettingsScreen(
                    state = SettingsUiState(archived = archived, activeCount = activeCount),
                    versionName = "0.1.0",
                    onRestore = { restored += it },
                    onDelete = { deleted += it },
                    onOpenRepo = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun `an empty list says what archiving is for`() {
        showSettings(archived = emptyList())

        compose.onNodeWithTag(ARCHIVED_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `restoring is offered while a slot is free`() {
        showSettings(activeCount = HabitRepository.MAX_ACTIVE_HABITS - 1)

        compose.onNodeWithTag(restoreTag(smoking.habit.id)).assertIsEnabled()
        compose.onNodeWithTag(RESTORE_BLOCKED_TAG).assertDoesNotExist()

        compose.onNodeWithTag(restoreTag(smoking.habit.id)).performClick()
        assertEquals(listOf(smoking.habit.id), restored)
    }

    @Test
    fun `restoring is refused at four active habits, and says why`() {
        showSettings(activeCount = HabitRepository.MAX_ACTIVE_HABITS)

        compose.onNodeWithTag(restoreTag(smoking.habit.id)).assertIsNotEnabled()
        compose.onNodeWithTag(RESTORE_BLOCKED_TAG).assertIsDisplayed()
    }

    @Test
    fun `the row shows the span and the size of the grid it is keeping`() {
        showSettings()

        compose.onNodeWithText("412 completed days").assertIsDisplayed()
    }

    @Test
    fun `deleting asks first, naming the habit and its real completion count`() {
        showSettings()

        compose.onNodeWithTag(deleteArchivedTag(smoking.habit.id)).performClick()

        compose.onNodeWithText("Delete Smoking?").assertIsDisplayed()
        compose
            .onNodeWithText(
                "This permanently removes 412 completed days. This cannot be undone.",
            )
            .assertIsDisplayed()
        assertEquals(emptyList<Long>(), deleted)
    }

    @Test
    fun `confirming the dialog is what actually deletes`() {
        showSettings()

        compose.onNodeWithTag(deleteArchivedTag(smoking.habit.id)).performClick()
        compose.onNodeWithTag(DELETE_CONFIRM_TAG).performClick()

        assertEquals(listOf(smoking.habit.id), deleted)
    }

    @Test
    fun `cancelling the dialog deletes nothing`() {
        showSettings()

        compose.onNodeWithTag(deleteArchivedTag(smoking.habit.id)).performClick()
        compose.onNodeWithTag(DELETE_CANCEL_TAG).performClick()

        assertEquals(emptyList<Long>(), deleted)
    }

    @Test
    fun `the version is on screen`() {
        showSettings()

        compose.onNodeWithText("Shabit 0.1.0").assertIsDisplayed()
    }
}
