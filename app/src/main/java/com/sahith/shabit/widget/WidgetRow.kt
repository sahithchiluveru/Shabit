package com.sahith.shabit.widget

import kotlin.math.min

/**
 * One habit's row in the widget: how big its parts are for the space the placement gives.
 *
 * The shape in #7 was drawn for a 4x4 placement — about 250dp across, ~57dp per row, four
 * habits. Nothing holds a widget to that. A 4x2 holding one habit is 341dp across with
 * 216dp of row to fill, and drawing the original row in the middle of it leaves the name
 * stranded at one edge and the history at the other with a hand's width of nothing
 * between them. So the row has a scale, and every part of it is a multiple of the size it
 * had in the design rather than a fixed dp.
 *
 * Kept out of the composition, and free of Glance and Compose types, because it is
 * arithmetic — and arithmetic about a widget nobody can see from a unit test is exactly
 * the part worth testing.
 */
internal object WidgetRow {
    /** Width reserved for a habit's icon and name, before scaling. */
    const val LABEL_WIDTH_DP = 64f

    /** Padding inside the widget's own card, on every edge. Not scaled: it is the card's. */
    const val PADDING_DP = 10f

    /** The gaps between label, grid and check button in one row. */
    const val GAPS_DP = 8f

    /** The habit's icon, and the gap between it and the name. */
    const val ICON_DP = 14f
    const val LABEL_GAP_DP = 4f

    /** The habit's name, in sp. */
    const val NAME_SP = 11f

    /**
     * Below this the grid is a smudge rather than a history, so [scale]'s caller drops it —
     * the same call [WidgetGrid.columnCount] makes for a row too narrow, made here for one
     * too short. The name and the check button carry on at this size regardless: they are
     * the parts that have to survive a small placement.
     */
    const val MIN_SCALE = 0.75f

    /** The dashboard's tiles are 13dp to the widget's 7dp; past 2x the widget out-shouts it. */
    const val MAX_SCALE = 2f

    /** A full sixteen weeks of grid at the base tile size — 127dp. */
    val BASE_GRID_WIDTH_DP =
        WidgetGrid.MAX_WEEKS * (WidgetGrid.TILE_DP + WidgetGrid.GAP_DP) - WidgetGrid.GAP_DP

    /**
     * The width one row was drawn for: label, gaps, check button, the card's padding and
     * the full grid. Comes to ~251dp, which is the ~250dp #7 budgets — derived rather than
     * written down so it stays true when one of the parts moves.
     */
    val BASE_WIDTH_DP = LABEL_WIDTH_DP + GAPS_DP + WidgetCheck.SIZE_DP +
        2 * PADDING_DP + BASE_GRID_WIDTH_DP

    /**
     * How much bigger than the design a row actually is, from whichever of width and
     * height runs out first.
     *
     * The lower end is deliberately unclamped: a value below [MIN_SCALE] is the signal to
     * drop the grid, and clamping here would hide it. Callers size the row's chrome with
     * `coerceAtLeast(MIN_SCALE)` and test the raw value for the grid.
     */
    fun scale(widthDp: Float, heightDp: Float, habits: Int): Float {
        val rowHeight = (heightDp - 2 * PADDING_DP) / habits.coerceAtLeast(1)
        return min(widthDp / BASE_WIDTH_DP, rowHeight / WidgetGrid.HEIGHT_DP)
            .coerceAtMost(MAX_SCALE)
    }

    /**
     * What is left across the row for the grid once the label, the check button, the gaps
     * and the card's padding have taken theirs — all of which grow with [scale] except the
     * padding, which belongs to the card rather than the row.
     */
    fun gridWidthDp(widthDp: Float, scale: Float): Float =
        widthDp - LABEL_WIDTH_DP * scale - WidgetCheck.SIZE_DP * scale -
            2 * PADDING_DP - GAPS_DP * scale
}
