package com.sahith.shabit.widget

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sahith.shabit.ui.theme.ShabitBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import android.graphics.Color as AndroidColor

/**
 * The widget's check button. Two states, and the difference has to be obvious at 32dp on a
 * home screen: filled in the habit's colour when today is done, an outline around empty
 * ground when it is not — the same two states as the card's button.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetCheckTest {
    private val color = Color(0xFFFBBF24)

    private fun render(done: Boolean): Bitmap = WidgetCheck.render(
        context = RuntimeEnvironment.getApplication(),
        density = 1f,
        done = done,
        color = color,
    )

    private fun Bitmap.pixels(): List<Int> =
        (0 until height).flatMap { y -> (0 until width).map { x -> getPixel(x, y) } }

    @Test
    fun `the button is the size it says it is`() {
        val bitmap = render(done = true)

        assertEquals(WidgetCheck.SIZE_DP.toInt(), bitmap.width)
        assertEquals(WidgetCheck.SIZE_DP.toInt(), bitmap.height)
    }

    @Test
    fun `done fills the button with the habit's colour`() {
        val bitmap = render(done = true)

        // Top edge, mid-width: inside the rounding and clear of the tick.
        val pixel = bitmap.getPixel(bitmap.width / 2, 1)
        assertEquals(255, AndroidColor.alpha(pixel))
        assertEquals(0xFB, AndroidColor.red(pixel))
        assertEquals(0xBF, AndroidColor.green(pixel))
        assertEquals(0x24, AndroidColor.blue(pixel))
    }

    @Test
    fun `a filled button's tick is the widget's ground, so the mark reads`() {
        val ground = ShabitBackground.toArgb()

        assertTrue(render(done = true).pixels().any { it == ground })
    }

    @Test
    fun `not done leaves the inside of the button empty`() {
        val bitmap = render(done = false)

        // Between the outline and the tick there is nothing but the widget's own ground
        // showing through.
        assertEquals(0, bitmap.getPixel(3, bitmap.height / 2))
    }

    @Test
    fun `not done still draws an outline in the habit's colour`() {
        val bitmap = render(done = false)

        assertTrue(AndroidColor.alpha(bitmap.getPixel(bitmap.width / 2, 0)) > 0)
        assertTrue(bitmap.pixels().none { it == ShabitBackground.toArgb() })
    }
}
