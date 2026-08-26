package com.sahith.shabit.ui

import androidx.annotation.DrawableRes
import com.sahith.shabit.R

/**
 * `Habit.iconKey` holds a drawable *name*, and this is where a name becomes a resource id.
 *
 * The lookup is an explicit table rather than `Resources.getIdentifier` because the widget
 * resolves the same keys through RemoteViews, and because a table survives resource
 * shrinking — anything reached only by name string gets stripped by R8.
 *
 * The 25 below are the whole set: a curated slice of [Lucide](https://lucide.dev) covering
 * the habits people actually track, imported as vector drawables so there is no third-party
 * dependency between a key and a `RemoteViews`-resolvable resource. There is no "more
 * icons" affordance — with a cap of four habits, a search field would be furniture.
 */
object HabitIcons {
    /**
     * Picker order, five rows of five: movement, mind and making, body and nature,
     * quitting, then the odds and ends.
     */
    val keys: List<String> = listOf(
        "ic_dumbbell", "ic_bike", "ic_footprints", "ic_heart", "ic_droplet",
        "ic_book_open", "ic_code", "ic_pen", "ic_brain", "ic_music",
        "ic_palette", "ic_gamepad", "ic_leaf", "ic_sprout", "ic_sun",
        "ic_moon", "ic_apple", "ic_coffee", "ic_pill", "ic_wallet",
        "ic_dollar_sign", "ic_cigarette_off", "ic_wine_off", "ic_phone_off", "ic_check",
    )

    /** What a brand-new habit starts as, before the user picks anything. */
    val DEFAULT_ICON_KEY: String = keys.first()

    private val byKey: Map<String, Int> = mapOf(
        "ic_dumbbell" to R.drawable.ic_dumbbell,
        "ic_bike" to R.drawable.ic_bike,
        "ic_footprints" to R.drawable.ic_footprints,
        "ic_heart" to R.drawable.ic_heart,
        "ic_droplet" to R.drawable.ic_droplet,
        "ic_book_open" to R.drawable.ic_book_open,
        "ic_code" to R.drawable.ic_code,
        "ic_pen" to R.drawable.ic_pen,
        "ic_brain" to R.drawable.ic_brain,
        "ic_music" to R.drawable.ic_music,
        "ic_palette" to R.drawable.ic_palette,
        "ic_gamepad" to R.drawable.ic_gamepad,
        "ic_leaf" to R.drawable.ic_leaf,
        "ic_sprout" to R.drawable.ic_sprout,
        "ic_sun" to R.drawable.ic_sun,
        "ic_moon" to R.drawable.ic_moon,
        "ic_apple" to R.drawable.ic_apple,
        "ic_coffee" to R.drawable.ic_coffee,
        "ic_pill" to R.drawable.ic_pill,
        "ic_wallet" to R.drawable.ic_wallet,
        "ic_dollar_sign" to R.drawable.ic_dollar_sign,
        "ic_cigarette_off" to R.drawable.ic_cigarette_off,
        "ic_wine_off" to R.drawable.ic_wine_off,
        "ic_phone_off" to R.drawable.ic_phone_off,
        "ic_check" to R.drawable.ic_check,
    )

    /**
     * An unrecognised key is a data bug rather than a state the user can reach, so this
     * falls back to a neutral mark instead of throwing in the middle of a list.
     */
    @DrawableRes
    fun resolve(iconKey: String): Int = byKey[iconKey] ?: R.drawable.ic_check
}
