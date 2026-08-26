package com.sahith.shabit.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.sahith.shabit.ui.theme.ShabitAccent

/** How much of a habit's colour an incomplete tile shows. See the theme's dark-only note. */
const val EMPTY_TILE_ALPHA = 0.15f

/**
 * The 21 swatches a habit's colour may be, in the order the picker lays them out: three
 * rows of seven, warm → cool → pinks and neutrals, as in `design/`.
 *
 * A fixed palette rather than a free colour picker because every one of these has been
 * checked against the two things Shabit does with a colour — a solid tile and the same
 * colour at [EMPTY_TILE_ALPHA] on near-black. An arbitrary hex can fail both.
 */
val HabitPalette: List<String> = listOf(
    "#F87171", "#FB923C", "#FBBF24", "#FACC15", "#A3E635", "#22C55E", "#34D399",
    "#2DD4BF", "#22D3EE", "#38BDF8", "#60A5FA", "#818CF8", "#A78BFA", "#C084FC",
    "#E879F9", "#EC4899", "#FB7185", "#94A3B8", "#A1A1AA", "#A8A29E", "#D6D3D1",
)

/** What a brand-new habit starts as, before the user picks anything. */
val DEFAULT_COLOR_HEX: String = HabitPalette.first()

/**
 * `Habit.colorHex` is a "#RRGGBB" string from the fixed palette. It is stored as text
 * because that is what the user picked and what the widget will read back; parsing failure
 * means someone wrote a bad row, so fall back to the accent rather than crash a list.
 */
fun habitColor(colorHex: String): Color =
    runCatching { Color(colorHex.toColorInt()) }.getOrDefault(ShabitAccent)
