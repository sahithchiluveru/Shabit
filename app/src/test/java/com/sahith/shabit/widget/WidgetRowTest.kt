package com.sahith.shabit.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a row sizes itself to the placement it lands in.
 *
 * Pure arithmetic, so no Robolectric: the point of these is the numbers that decide
 * whether the widget looks deliberate or looks broken, and nobody can see those from a
 * home screen until it is too late.
 */
class WidgetRowTest {
    /** The 4x4 the shape in #7 was drawn for: ~250x230dp holding four habits. */
    @Test
    fun `the placement the design was drawn for lands at about its own size`() {
        val scale = WidgetRow.scale(widthDp = 250f, heightDp = 230f, habits = 4)

        assertEquals(1f, scale, 0.06f)
    }

    /**
     * The regression. A 4x2 on a Galaxy A30 is 341x236dp, and with `SizeMode.Responsive`
     * the row was composed against the 250dp bucket: a 127dp grid right-aligned in a 217dp
     * box, leaving ~90dp of nothing between the habit's name and its history.
     */
    @Test
    fun `a placement wider than the design fills it rather than pooling a void`() {
        val width = 341f

        val scale = WidgetRow.scale(widthDp = width, heightDp = 236f, habits = 1)
        val available = WidgetRow.gridWidthDp(width, scale)
        val columns = WidgetGrid.columnCount(available / scale)
        val step = WidgetGrid.TILE_DP + WidgetGrid.GAP_DP
        val drawn = (columns * step - WidgetGrid.GAP_DP) * scale

        assertEquals(WidgetGrid.MAX_WEEKS, columns)
        assertTrue(
            "the grid left ${available - drawn}dp of the row empty",
            available - drawn < 12f,
        )
    }

    @Test
    fun `growth stops before the widget out-shouts the dashboard`() {
        assertEquals(WidgetRow.MAX_SCALE, WidgetRow.scale(2000f, 2000f, habits = 1), 0.001f)
    }

    /**
     * Four habits in a placement resized down to near `minResizeHeight` gives each row
     * ~22dp, well under the 55dp a grid needs. The caller reads a scale below MIN_SCALE as
     * "drop the grid", so it has to actually come back below it rather than being clamped.
     */
    @Test
    fun `a row too short for a history reports it instead of clamping`() {
        val scale = WidgetRow.scale(widthDp = 341f, heightDp = 110f, habits = 4)

        assertTrue("expected < ${WidgetRow.MIN_SCALE}, got $scale", scale < WidgetRow.MIN_SCALE)
    }

    /** Belt and braces: the empty-habits path draws no rows, but division still happens. */
    @Test
    fun `no habits does not divide by zero`() {
        val scale = WidgetRow.scale(widthDp = 250f, heightDp = 230f, habits = 0)

        assertTrue("expected a finite scale, got $scale", scale.isFinite())
    }

    @Test
    fun `the base width is the row the issue budgeted for`() {
        assertEquals(250f, WidgetRow.BASE_WIDTH_DP, 5f)
    }
}
