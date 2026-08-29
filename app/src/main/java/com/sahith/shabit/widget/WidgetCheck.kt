package com.sahith.shabit.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.sahith.shabit.R
import com.sahith.shabit.ui.theme.ShabitBackground
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The check button, drawn to a bitmap: the habit's colour filled when today is done, an
 * outline in that colour when it is not — the same two states as the card's button.
 *
 * A bitmap because the button's colour belongs to the habit, so a static drawable cannot
 * express it, and Glance's `cornerRadius` only rounds on API 31 and up. Twenty kilobytes
 * of pixels buys the same button on every supported release.
 */
internal object WidgetCheck {
    /** The button's side, in dp. */
    const val SIZE_DP = 32f

    private const val CORNER_DP = 9f
    private const val STROKE_DP = 1.5f
    private const val TICK_INSET_DP = 7f
    private const val MAX_SCALE = 2f

    fun render(
        context: Context,
        density: Float,
        done: Boolean,
        color: Color,
        uiScale: Float = 1f,
    ): Bitmap {
        val pixels = min(density, MAX_SCALE)
        val scale = uiScale * pixels
        val side = (SIZE_DP * scale).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(side, side)
        // The button grows with its row, so the bitmap has to carry the density it was
        // drawn at rather than the device's — same reasoning as WidgetGrid.MAX_SCALE.
        bitmap.density = (pixels * DisplayMetrics.DENSITY_DEFAULT).roundToInt()
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
        val corner = CORNER_DP * scale

        if (done) {
            canvas.drawRoundRect(RectF(0f, 0f, side.toFloat(), side.toFloat()), corner, corner, paint)
        } else {
            val stroke = STROKE_DP * scale
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            val inset = stroke / 2f
            canvas.drawRoundRect(
                RectF(inset, inset, side - inset, side - inset),
                corner,
                corner,
                paint,
            )
        }

        // On a filled button the tick sits on the habit's own colour, so it has to be the
        // widget's ground rather than that colour again — exactly as on the card.
        val tick = ContextCompat.getDrawable(context, R.drawable.ic_check)?.mutate() ?: return bitmap
        tick.setTint(if (done) ShabitBackground.toArgb() else color.toArgb())
        val inset = (TICK_INSET_DP * scale).roundToInt()
        tick.setBounds(inset, inset, side - inset, side - inset)
        tick.draw(canvas)
        return bitmap
    }
}
