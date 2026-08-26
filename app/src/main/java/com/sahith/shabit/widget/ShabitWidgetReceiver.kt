package com.sahith.shabit.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The system's handle on [ShabitWidget], and the only place the rollover alarm's lifetime
 * is managed: it is booked when the first widget is placed and cancelled when the last one
 * goes, so a device with no widget on it never wakes for a redraw that has nowhere to go.
 *
 * `onUpdate` rebooks rather than assuming: an alarm can be dropped by a force-stop or by
 * an OEM battery manager, and an update is the one moment we are certain to be running.
 */
class ShabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShabitWidget

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RolloverAlarm.schedule(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        RolloverAlarm.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RolloverAlarm.cancel(context)
    }
}
