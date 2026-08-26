package com.sahith.shabit.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.sahith.shabit.ui.HabitIcons
import com.sahith.shabit.ui.HabitPalette
import com.sahith.shabit.ui.theme.ShabitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The form against the real composition. It is stateless, so these check what it renders
 * for a given state and what it reports back — the writing itself is
 * [HabitEditorViewModelTest]'s job.
 */
@RunWith(RobolectricTestRunner::class)
class HabitEditorComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private var name = ""
    private var description = ""
    private val pickedIcons = mutableListOf<String>()
    private val pickedColors = mutableListOf<String>()
    private var saves = 0
    private var closes = 0

    /**
     * The screen is stateless, but a text field that never sees its own edits come back is
     * not the thing the app runs, so the state is hoisted here the way the view model does
     * it — every change is fed straight back in.
     */
    private fun showEditor(initial: HabitEditorUiState) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            ShabitTheme {
                HabitEditorScreen(
                    state = state,
                    onNameChange = {
                        name = it
                        state = state.copy(name = it)
                    },
                    onDescriptionChange = {
                        description = it
                        state = state.copy(description = it)
                    },
                    onIconChange = {
                        pickedIcons += it
                        state = state.copy(iconKey = it)
                    },
                    onColorChange = {
                        pickedColors += it
                        state = state.copy(colorHex = it)
                    },
                    onSave = { saves++ },
                    onClose = { closes++ },
                )
            }
        }
    }

    @Test
    fun `saving is refused while the name is blank`() {
        showEditor(HabitEditorUiState(editing = false))

        compose.onNodeWithTag(SAVE_HABIT_TAG).assertIsNotEnabled()
    }

    @Test
    fun `saving is offered once the name is not blank`() {
        showEditor(HabitEditorUiState(editing = false, name = "Gym"))

        compose.onNodeWithTag(SAVE_HABIT_TAG).assertIsEnabled()
        compose.onNodeWithTag(SAVE_HABIT_TAG).performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `whitespace is not a name`() {
        showEditor(HabitEditorUiState(editing = false, name = "   "))

        compose.onNodeWithTag(SAVE_HABIT_TAG).assertIsNotEnabled()
    }

    @Test
    fun `typing reports the new name and description upwards`() {
        showEditor(HabitEditorUiState(editing = false))

        compose.onNodeWithTag(NAME_FIELD_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(NAME_FIELD_TAG).performTextInput("Gym")
        compose.onNodeWithTag(DESCRIPTION_FIELD_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(DESCRIPTION_FIELD_TAG).performTextInput("Lift things")

        assertEquals("Gym", name)
        assertEquals("Lift things", description)
    }

    @Test
    fun `every icon and every swatch is on screen and pickable`() {
        showEditor(HabitEditorUiState(editing = false))

        HabitIcons.keys.forEach { compose.onNodeWithTag(iconTag(it)).performScrollTo().performClick() }
        HabitPalette.forEach { compose.onNodeWithTag(colorTag(it)).performScrollTo().performClick() }

        assertEquals(HabitIcons.keys, pickedIcons)
        assertEquals(HabitPalette, pickedColors)
    }

    @Test
    fun `the current icon and colour are the selected ones`() {
        showEditor(
            HabitEditorUiState(editing = true, name = "Gym", iconKey = "ic_bike", colorHex = "#22D3EE"),
        )

        compose.onNodeWithTag(iconTag("ic_bike")).assertIsSelected()
        compose.onNodeWithTag(iconTag("ic_dumbbell")).assertIsNotSelected()
        compose.onNodeWithTag(colorTag("#22D3EE")).assertIsSelected()
        compose.onNodeWithTag(colorTag("#FBBF24")).assertIsNotSelected()
    }

    @Test
    fun `the title says which of the two jobs this is`() {
        showEditor(HabitEditorUiState(editing = true, name = "Gym"))

        compose.onNodeWithText("Edit Habit").assertExists()
    }

    @Test
    fun `closing reports upwards without saving`() {
        showEditor(HabitEditorUiState(editing = false, name = "Gym"))

        compose.onNodeWithContentDescription("Cancel").performClick()

        assertEquals(1, closes)
        assertEquals(0, saves)
    }
}
