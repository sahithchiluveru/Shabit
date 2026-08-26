package com.sahith.shabit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ShabitColorScheme = darkColorScheme(
    primary = ShabitAccent,
    onPrimary = ShabitBackground,
    background = ShabitBackground,
    onBackground = ShabitTextPrimary,
    surface = ShabitCard,
    onSurface = ShabitTextPrimary,
    surfaceVariant = ShabitCard,
    onSurfaceVariant = ShabitTextSecondary,
)

/**
 * Shabit is dark-only, deliberately — an incomplete tile is its habit's colour
 * at ~15% alpha, which reads as a rich shade on near-black and as washed-out
 * mud on white. Supporting light mode would mean a second fade formula tuned
 * separately for all 21 habit colours.
 *
 * So this ignores the system light/dark setting on purpose. Don't add
 * isSystemInDarkTheme() here without revisiting that decision.
 */
@Composable
fun ShabitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShabitColorScheme,
        typography = ShabitTypography,
        content = content,
    )
}
