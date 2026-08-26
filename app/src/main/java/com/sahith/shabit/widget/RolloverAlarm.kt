package com.sahith.shabit.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.sahith.shabit.data.nextRollover
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The 4am redraw.
 *
 * At the rollover every grid's anchor column moves one along and yesterday's tile stops
 * being today's, and none of that can wait for the user to open something — the widget is
 * the only part of Shabit most days touch. So the alarm is scheduled rather than inferred.
 *
 * `setAndAllowWhileIdle` because an exact alarm would mean asking for `SCHEDULE_EXACT_ALARM`,
 * which is a permission prompt for a redraw. Inexact means the grid can be a few minutes
 * late at 4am; nobody is looking, and the next tap is correct either way.
 */
internal object RolloverAlarm {
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextRollover().toEpochMilli(),
            pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RolloverReceiver::class.java).setAction(ACTION_ROLLOVER),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    const val ACTION_ROLLOVER = "com.sahith.shabit.action.ROLLOVER"
}

/**
 * Redraws the widget and books the next rollover.
 *
 * Also listens for boot, a timezone change and a manual clock change: an alarm does not
 * survive a reboot, and "4am" moves when the zone does.
 */
class RolloverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The receiver is not exported, so nothing but the system and the alarm above can
        // reach it — but naming the four things it answers to keeps that true by intent
        // rather than by configuration.
        if (intent.action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        val application = context.applicationContext
        // The widget update is suspending, so the broadcast has to be held open across it.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                ShabitWidget.updateAll(application)
            } finally {
                RolloverAlarm.schedule(application)
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            RolloverAlarm.ACTION_ROLLOVER,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
