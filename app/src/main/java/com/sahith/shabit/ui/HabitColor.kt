package com.sahith.shabit.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.sahith.shabit.ui.theme.ShabitAccent

/** How much of a habit's colour an incomplete tile shows. See the theme's dark-only note. */
const val EMPTY_TILE_ALPHA = 0.15f

/**
 * `Habit.colorHex` is a "#RRGGBB" string from the fixed palette. It is stored as text
 * because that is what the user picked and what the widget will read back; parsing failure
 * means someone wrote a bad row, so fall back to the accent rather than crash a list.
 */
fun habitColor(colorHex: String): Color =
    runCatching { Color(colorHex.toColorInt()) }.getOrDefault(ShabitAccent)
