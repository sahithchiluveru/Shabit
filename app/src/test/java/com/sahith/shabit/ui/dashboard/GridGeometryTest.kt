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
    fun `cells before the habit existed are outside the grid`() {
        val created = LocalDate.of(2026, 3, 11)
        assertEquals(
            TileState.OUTSIDE,
            tileState(monday, createdDate = created, today = sunday, completed = false),
        )
        assertEquals(
            TileState.EMPTY,
            tileState(created, createdDate = created, today = sunday, completed = false),
        )
    }

    @Test
    fun `a completed cell before the habit existed is still outside`() {
        // Defensive: a stray row must not draw a tile where the grid has no business
        // being tappable.
        assertEquals(
            TileState.OUTSIDE,
            tileState(monday, createdDate = sunday, today = sunday, completed = true),
        )
    }

    @Test
    fun `future cells are outside the grid`() {
        assertEquals(
            TileState.OUTSIDE,
            tileState(sunday.plusDays(1), createdDate = monday, today = sunday, completed = false),
        )
    }

    @Test
    fun `today is inside the grid`() {
        assertEquals(
            TileState.FILLED,
            tileState(sunday, createdDate = monday, today = sunday, completed = true),
        )
        assertEquals(
            TileState.EMPTY,
            tileState(sunday, createdDate = monday, today = sunday, completed = false),
        )
    }
}
