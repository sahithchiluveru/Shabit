package com.sahith.shabit.widget

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.sahith.shabit.ui.EMPTY_TILE_ALPHA
import com.sahith.shabit.ui.dashboard.GRID_ROWS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

/**
 * The widget's grid: how much of the history fits, and what the pixels end up being.
 *
 * The layout rules themselves belong to `GridGeometryTest` — this covers the two things
 * only the widget has, which are a fixed width to fit into and a `Canvas` instead of a
 * composition. Native graphics, because the point of the last few tests is the pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetGridTest {
    /** A Wednesday, so the current week has real future cells in it. */
    private val today: LocalDate = LocalDate.of(2026, 3, 11)

    private val color = Color(0xFFFBBF24)

    /** 1f keeps a tile exactly [WidgetGrid.TILE_DP] pixels, which makes the maths readable. */
    private val density = 1f

    @Test
    fun `columns are whatever whole tiles fit the width`() {
        // One tile needs TILE_DP; every one after it needs TILE_DP + GAP_DP.
        assertEquals(0, WidgetGrid.columnsThatFit(WidgetGrid.TILE_DP - 1f))
        assertEquals(1, WidgetGrid.columnsThatFit(WidgetGrid.TILE_DP))
        assertEquals(2, WidgetGrid.columnsThatFit(WidgetGrid.TILE_DP * 2 + WidgetGrid.GAP_DP))
    }

    @Test
    fun `a negative width asks for no columns rather than a negative number of them`() {
        assertEquals(0, WidgetGrid.columnsThatFit(-40f))
    }

    @Test
    fun `a wide placement is still capped at the fixed window`() {
        val columns = WidgetGrid.columnCount(
            widthDp = 2000f,
            createdDate = today.minusYears(3),
            today = today,
        )

        assertEquals(WidgetGrid.MAX_WEEKS, columns)
    }

    @Test
    fun `a young habit shows only the weeks it has`() {
        val columns = WidgetGrid.columnCount(
            widthDp = 2000f,
            createdDate = today.minusWeeks(2),
            today = today,
        )

        assertEquals(3, columns)
    }

    @Test
    fun `the target placement fits about sixteen weeks`() {
        // 250dp wide, less the label, the check button, the padding and the gaps.
        val gridWidth = 250f - 64f - WidgetCheck.SIZE_DP - 20f - 8f

        val columns = WidgetGrid.columnCount(gridWidth, today.minusYears(1), today)

        assertTrue("expected 14..16 columns, got $columns", columns in 14..16)
    }

    @Test
    fun `a placement too narrow for a real history drops the grid rather than squashing it`() {
        // The compact size bucket, less everything that is not grid.
        val gridWidth = 150f - 64f - WidgetCheck.SIZE_DP - 20f - 8f

        assertEquals(0, WidgetGrid.columnCount(gridWidth, today.minusYears(1), today))
    }

    @Test
    fun `a brand new habit is not mistaken for a narrow widget`() {
        val columns = WidgetGrid.columnCount(
            widthDp = 2000f,
            createdDate = today,
            today = today,
        )

        assertEquals(1, columns)
    }

    @Test
    fun `no room for a column draws nothing at all`() {
        assertNull(render(columns = 0))
    }

    @Test
    fun `the bitmap is exactly the grid it was asked for`() {
        val bitmap = checkNotNull(render(columns = 4))

        val step = WidgetGrid.TILE_DP + WidgetGrid.GAP_DP
        assertEquals((4 * step - WidgetGrid.GAP_DP).toInt(), bitmap.width)
        assertEquals((GRID_ROWS * step - WidgetGrid.GAP_DP).toInt(), bitmap.height)
    }

    @Test
    fun `a completed day is the habit's colour, solid`() {
        // Row 0 of the only column is Monday 9 March; today is Wednesday 11 March.
        val bitmap = checkNotNull(render(columns = 1, completions = setOf(today.minusDays(2))))

        val pixel = centreOf(bitmap, row = 0)
        assertEquals(255, AndroidColor.alpha(pixel))
        assertEquals(0xFB, AndroidColor.red(pixel))
        assertEquals(0xBF, AndroidColor.green(pixel))
        assertEquals(0x24, AndroidColor.blue(pixel))
    }

    @Test
    fun `an incomplete day is the same colour, faded`() {
        val bitmap = checkNotNull(render(columns = 1))

        val alpha = AndroidColor.alpha(centreOf(bitmap, row = 0))
        assertEquals((EMPTY_TILE_ALPHA * 255).roundToInt().toFloat(), alpha.toFloat(), 2f)
    }

    @Test
    fun `days after today are left unpainted`() {
        val bitmap = checkNotNull(render(columns = 1))

        // Today is a Wednesday, row 2. Thursday onwards has not happened.
        assertEquals(0, centreOf(bitmap, row = 3))
        assertEquals(0, centreOf(bitmap, row = 6))
    }

    @Test
    fun `days before the habit existed are left unpainted`() {
        // Created on the Wednesday: Monday and Tuesday of that week are outside the grid.
        val bitmap = checkNotNull(render(columns = 1, createdDate = today))

        assertEquals(0, centreOf(bitmap, row = 0))
        assertEquals(0, centreOf(bitmap, row = 1))
        assertTrue(AndroidColor.alpha(centreOf(bitmap, row = 2)) > 0)
    }

    private fun render(
        columns: Int,
        createdDate: LocalDate = today.minusYears(1),
        completions: Set<LocalDate> = emptySet(),
    ) = WidgetGrid.render(density, columns, createdDate, today, completions, color)

    /** The centre pixel of the tile at [row] in the right-hand column. */
    private fun centreOf(bitmap: Bitmap, row: Int): Int {
        val step = WidgetGrid.TILE_DP + WidgetGrid.GAP_DP
        val x = bitmap.width - (WidgetGrid.TILE_DP / 2).toInt() - 1
        val y = (row * step + WidgetGrid.TILE_DP / 2).toInt()
        return bitmap.getPixel(x, y)
    }
}
