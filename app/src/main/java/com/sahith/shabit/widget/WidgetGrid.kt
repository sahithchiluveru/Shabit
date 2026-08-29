package com.sahith.shabit.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.sahith.shabit.ui.EMPTY_TILE_ALPHA
import com.sahith.shabit.ui.FUTURE_TILE_ALPHA
import com.sahith.shabit.ui.dashboard.GRID_ROWS
import com.sahith.shabit.ui.dashboard.TileState
import com.sahith.shabit.ui.dashboard.cellDate
import com.sahith.shabit.ui.dashboard.tileState
import com.sahith.shabit.ui.dashboard.weekStartAt
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The habit grid, drawn to a bitmap for the widget.
 *
 * A bitmap rather than a tree of Glance boxes because sixteen weeks is 112 tiles per habit
 * and four habits is 448 of them. Glance turns every element into a `RemoteViews` node,
 * and `Row`/`Column` cap their children well below sixteen, so the element version would
 * be a nest of workarounds parcelled across a Binder transaction with a hard 1MB ceiling.
 * One `Canvas` per habit is both smaller on the wire and pixel-identical to the dashboard.
 *
 * The layout rules themselves are not reimplemented — [weekStartAt], [cellDate] and
 * [tileState] are the same functions the dashboard grid lays out with.
 */
internal object WidgetGrid {
    /**
     * A tile's side, in dp, roughly half the dashboard's.
     *
     * Seven rows at [TILE_DP] + [GAP_DP] is 55dp, which is what makes four habits fit the
     * ~230dp height of a 4x4 placement at the ~57dp per row the shape in #7 budgets.
     */
    const val TILE_DP = 7f

    /** The gap between tiles, in dp. */
    const val GAP_DP = 1f

    /** Tile corner rounding, in dp. */
    private const val CORNER_DP = 2f

    /** Decision 10 in #1: the widget shows a fixed recent window, not the whole history. */
    const val MAX_WEEKS = 16

    /**
     * The bitmap is drawn at the device's density, capped here.
     *
     * The whole `RemoteViews` tree has to fit in a 1MB Binder transaction, and four grids
     * at 3x would spend most of it on pixels. At 2x a tile is a 14px square, which is as
     * much resolution as a flat rounded square can use.
     *
     * The cap is a *resolution* decision and must not become a size one: a bitmap carries
     * its own density, and Android rescales it by target/bitmap density on the way to the
     * screen. Left at the default the bitmap would claim to be the device's density, so
     * capping 2.625 to 2 would silently shrink the drawn grid to 76% of the dp size asked
     * for. [render] tags every bitmap with the density it actually drew at instead.
     */
    private const val MAX_SCALE = 2f

    /** The grid's height in dp — seven weekdays, whatever the width turns out to be. */
    const val HEIGHT_DP = GRID_ROWS * (TILE_DP + GAP_DP) - GAP_DP

    /** How many week columns fit in [widthDp], ignoring how much history there is. */
    @VisibleForTesting
    fun columnsThatFit(widthDp: Float): Int =
        floor((widthDp + GAP_DP) / (TILE_DP + GAP_DP)).toInt().coerceAtLeast(0)

    /**
     * Fewer weeks than this is not a history, it is a smudge — so a placement too narrow
     * to hold [MIN_COLUMNS] gets no grid at all rather than a squashed one. The name and
     * the check button are the parts that have to survive a small widget.
     */
    private const val MIN_COLUMNS = 4

    /**
     * How many week columns to actually draw: what fits, never more than [MAX_WEEKS], and
     * none at all when the placement is too narrow to be worth it.
     *
     * How old the habit is does not come into it. As on the dashboard, the grid is a full
     * block of faded tiles from the day the habit is made, so a young habit fills the
     * widget exactly like an old one — only with fewer of them lit.
     */
    fun columnCount(widthDp: Float): Int {
        val fits = columnsThatFit(widthDp)
        if (fits < MIN_COLUMNS) return 0
        return min(fits, MAX_WEEKS)
    }

    /**
     * Draw [columns] weeks of grid, today's week at the right-hand edge.
     *
     * [uiScale] stretches the tiles themselves so the grid can fill the space a placement
     * actually gives it — a 4x2 widget is twice the width the shape in #7 was drawn for,
     * and 8dp tiles marooned in the middle of it read as a mistake. It is a layout choice
     * and is kept separate from [density], which only decides how many pixels each of
     * those dp end up being.
     *
     * Returns null when there is no room for even one column — the caller drops the grid
     * rather than drawing a sliver of one.
     */
    fun render(
        density: Float,
        columns: Int,
        today: LocalDate,
        completions: Set<LocalDate>,
        color: Color,
        uiScale: Float = 1f,
    ): Bitmap? {
        if (columns <= 0) return null
        val pixels = min(density, MAX_SCALE)
        val scale = uiScale * pixels
        val tile = TILE_DP * scale
        val gap = GAP_DP * scale
        val step = tile + gap

        val width = (columns * step - gap).toInt().coerceAtLeast(1)
        val height = (GRID_ROWS * step - gap).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        // Without this the bitmap claims the device's density and Android rescales it —
        // see the note on MAX_SCALE.
        bitmap.density = (pixels * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val corner = CORNER_DP * scale
        val rect = RectF()

        val filled = color.toArgb()
        val empty = color.copy(alpha = EMPTY_TILE_ALPHA).toArgb()
        val future = color.copy(alpha = FUTURE_TILE_ALPHA).toArgb()

        repeat(columns) { index ->
            // Index 0 is the week containing today, and today's column sits at the right
            // edge — the same anchoring the dashboard gets from its reversed LazyRow.
            val weekStart = weekStartAt(index, today)
            val left = width - (index + 1) * step + gap
            repeat(GRID_ROWS) { row ->
                val date = cellDate(weekStart, row)
                val state = tileState(date, today, completions.contains(date))
                paint.color = when (state) {
                    TileState.FILLED -> filled
                    TileState.EMPTY -> empty
                    TileState.FUTURE -> future
                }
                rect.set(left, row * step, left + tile, row * step + tile)
                canvas.drawRoundRect(rect, corner, corner, paint)
            }
        }
        return bitmap
    }
}
