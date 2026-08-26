package com.sahith.shabit.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The grid's whole layout is date arithmetic, so it is worth pinning exactly rather than
 * eyeballing a screenshot. 2026-03-15 is a Sunday and 2026-03-09 the Monday before it,
 * which is what makes the ragged-column cases interesting.
 */
class GridGeometryTest {
    private val sunday = LocalDate.of(2026, 3, 15)
    private val monday = LocalDate.of(2026, 3, 9)

    @Test
    fun `week start is the Monday of that week`() {
        assertEquals(monday, weekStart(sunday))
        assertEquals(monday, weekStart(monday))
        assertEquals(monday, weekStart(LocalDate.of(2026, 3, 12)))
    }

    @Test
    fun `a habit created today has a single column`() {
        assertEquals(1, weekColumnCount(createdDate = sunday, today = sunday))
    }

    @Test
    fun `a habit created earlier in the same week still has a single column`() {
        assertEquals(1, weekColumnCount(createdDate = monday, today = sunday))
    }

    @Test
    fun `a habit created the previous week has two columns`() {
        assertEquals(2, weekColumnCount(createdDate = monday.minusDays(1), today = sunday))
    }

    @Test
    fun `column count spans whole weeks, not days`() {
        assertEquals(53, weekColumnCount(createdDate = sunday.minusWeeks(52), today = sunday))
    }

    @Test
    fun `a habit created in the future is clamped to one column`() {
        assertEquals(1, weekColumnCount(createdDate = sunday.plusWeeks(2), today = sunday))
    }

    @Test
    fun `column zero is the week containing today`() {
        assertEquals(monday, weekStartAt(index = 0, today = sunday))
        assertEquals(monday.minusWeeks(1), weekStartAt(index = 1, today = sunday))
        assertEquals(monday.minusWeeks(10), weekStartAt(index = 10, today = sunday))
    }

    @Test
    fun `row zero is Monday and row six is Sunday`() {
        assertEquals(monday, cellDate(monday, row = 0))
        assertEquals(sunday, cellDate(monday, row = GRID_ROWS - 1))
    }

    @Test
    fun `a grid never has fewer columns than fit the card it is drawn on`() {
        // Made this morning, but drawn on a card twenty columns wide: all twenty are there.
        assertEquals(20, gridColumnCount(createdDate = sunday, today = sunday, columnsToFill = 20))
    }

    @Test
    fun `a history longer than the card wins`() {
        assertEquals(
            53,
            gridColumnCount(createdDate = sunday.minusWeeks(52), today = sunday, columnsToFill = 20),
        )
    }

    @Test
    fun `columns to fill round up so no bare card shows at the edge`() {
        // Two whole 16dp columns is 29dp of tiles; a 30dp card needs a third, part-shown.
        assertEquals(2, columnsToFill(widthDp = 29f, tileDp = 13f, gapDp = 3f))
        assertEquals(3, columnsToFill(widthDp = 30f, tileDp = 13f, gapDp = 3f))
    }

    @Test
    fun `even a card with no width asks for one column`() {
        assertEquals(1, columnsToFill(widthDp = 0f, tileDp = 13f, gapDp = 3f))
    }

    @Test
    fun `cells before the habit existed are ordinary empty days`() {
        // The grid is a block of faded tiles from day one and every past day in it can be
        // filled in — remembering on Wednesday that you also ran on Monday is normal.
        assertEquals(
            TileState.EMPTY,
            tileState(monday, today = sunday, completed = false),
        )
        assertEquals(
            TileState.FILLED,
            tileState(monday, today = sunday, completed = true),
        )
    }

    @Test
    fun `future cells are future, completed or not`() {
        assertEquals(
            TileState.FUTURE,
            tileState(sunday.plusDays(1), today = sunday, completed = false),
        )
        // Defensive: a stray row must not light up a day that has not happened.
        assertEquals(
            TileState.FUTURE,
            tileState(sunday.plusDays(1), today = sunday, completed = true),
        )
    }

    @Test
    fun `today is the last day that counts`() {
        assertEquals(TileState.FILLED, tileState(sunday, today = sunday, completed = true))
        assertEquals(TileState.EMPTY, tileState(sunday, today = sunday, completed = false))
    }
}
