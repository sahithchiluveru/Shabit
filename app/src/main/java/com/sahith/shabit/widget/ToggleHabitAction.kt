package com.sahith.shabit.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.sahith.shabit.data.HabitRepository

/**
 * Tap-to-check from the home screen.
 *
 * This runs in the app process — no Activity is launched, no window opens (decision 3 in
 * #1). It goes through the same [HabitRepository.toggle] the app's tiles do, so a tap here
 * and a tap there cannot disagree about what a completion is, and the transaction inside
 * it is what makes two taps arriving together safe.
 *
 * Redrawing is not this callback's job: the repository pokes the widget after every write,
 * so a toggle from here refreshes for the same reason a toggle in the app does.
 */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val repository = HabitRepository.getInstance(context)
        repository.toggle(habitId, repository.today())
    }

    companion object {
        private val HabitIdKey = ActionParameters.Key<Long>("habitId")

        fun parametersFor(habitId: Long): ActionParameters =
            actionParametersOf(HabitIdKey to habitId)
    }
}
