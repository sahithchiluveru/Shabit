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
 * #5 grows this to the full ~25 Lucide set alongside the icon picker; these are the ones
 * the dashboard needs today.
 */
object HabitIcons {
    private val byKey: Map<String, Int> = mapOf(
        "ic_dumbbell" to R.drawable.ic_dumbbell,
        "ic_code" to R.drawable.ic_code,
        "ic_book_open" to R.drawable.ic_book_open,
        "ic_leaf" to R.drawable.ic_leaf,
        "ic_check" to R.drawable.ic_check,
    )

    /**
     * An unrecognised key is a data bug rather than a state the user can reach, so this
     * falls back to a neutral mark instead of throwing in the middle of a list.
     */
    @DrawableRes
    fun resolve(iconKey: String): Int = byKey[iconKey] ?: R.drawable.ic_check
}
