package com.sahith.shabit.ui

import com.sahith.shabit.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The two fixed sets a habit's look is drawn from.
 *
 * The icon table is worth a test because it is hand-maintained and because the widget
 * resolves the same keys through `RemoteViews` in #7: a key with no drawable behind it
 * silently becomes a tick mark on the dashboard and would be just as silent on the home
 * screen. Resolving every key here is what catches that at build time instead.
 */
@RunWith(RobolectricTestRunner::class)
class HabitLookTest {
    @Test
    fun `every picker icon resolves to its own drawable`() {
        val resolved = HabitIcons.keys.map(HabitIcons::resolve)

        assertEquals(25, HabitIcons.keys.size)
        assertTrue(resolved.none { it == 0 })
        assertEquals(HabitIcons.keys.size, resolved.distinct().size)
    }

    @Test
    fun `an unknown key falls back to the tick rather than throwing`() {
        assertEquals(R.drawable.ic_check, HabitIcons.resolve("ic_nonexistent"))
    }

    @Test
    fun `the default icon and colour are in their pickers`() {
        assertTrue(HabitIcons.DEFAULT_ICON_KEY in HabitIcons.keys)
        assertTrue(DEFAULT_COLOR_HEX in HabitPalette)
    }

    @Test
    fun `the palette is 21 distinct hex swatches`() {
        assertEquals(21, HabitPalette.size)
        assertEquals(21, HabitPalette.distinct().size)
        assertTrue(HabitPalette.all { Regex("^#[0-9A-F]{6}$").matches(it) })
    }

    @Test
    fun `every swatch parses to an opaque colour`() {
        HabitPalette.forEach { hex ->
            val color = habitColor(hex)
            assertEquals(1f, color.alpha)
            // Falling back to the accent is what a bad hex looks like, so anything that
            // is not the accent proves the parse worked.
            if (hex != "#A78BFA") assertNotEquals(habitColor("not a colour"), color)
        }
    }
}
