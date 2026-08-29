package com.sahith.shabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sahith.shabit.MainActivity
import com.sahith.shabit.R
import com.sahith.shabit.data.Habit
import com.sahith.shabit.data.HabitRepository
import com.sahith.shabit.ui.HabitIcons
import com.sahith.shabit.ui.habitColor
import com.sahith.shabit.ui.theme.ShabitTextPrimary
import com.sahith.shabit.ui.theme.ShabitTextSecondary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** One habit as the widget draws it. */
private data class WidgetHabit(val habit: Habit, val completions: Set<LocalDate>)

/**
 * The headline feature: on a normal day this is the only part of Shabit anyone touches.
 *
 * One widget showing every active habit, in dashboard order, with a check button that
 * toggles today without launching an Activity (decisions 3 and 4 in #1). A tap anywhere
 * else opens the app, which is where setup and corrections live.
 *
 * An `object` rather than a class so that [updateAll] has one obvious caller-facing name:
 * [HabitRepository] pokes it after every write, which is what keeps the widget in step
 * with the app even while its own Glance session is not running.
 */
object ShabitWidget : GlanceAppWidget() {
    /**
     * Exact rather than Responsive, because Responsive reports the bucket, not the widget.
     *
     * A placement larger than the largest declared bucket still composes against that
     * bucket: `LocalSize` said 250dp inside a 341dp widget, so the row was laid out for
     * 250 and the 91dp left over pooled in the weighted box as a void between a habit's
     * name and its history. The row now scales to the space it is given, which only works
     * if it is told the truth about that space.
     *
     * A placement too narrow for a real grid still loses it rather than squashing it —
     * that is [WidgetGrid.columnCount]'s job, and it never depended on the buckets.
     */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = HabitRepository.getInstance(context)
        provideContent {
            // Recomputed on every composition, and the 4am alarm forces one — so a tile
            // stops being "today" on time whether or not anyone opens anything.
            val today = repository.today()
            val habits by remember { repository.widgetHabits() }.collectAsState(initial = null)
            WidgetContent(habits = habits, today = today)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun HabitRepository.widgetHabits(): Flow<List<WidgetHabit>> =
    activeHabits().flatMapLatest { habits ->
        if (habits.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                habits.map { habit -> completions(habit.id).map { WidgetHabit(habit, it) } },
            ) { it.toList() }
        }
    }

@Composable
private fun WidgetContent(habits: List<WidgetHabit>?, today: LocalDate) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            // A drawable rather than a colour so the corners round on every supported
            // release, and so the widget carries its own ground over any wallpaper.
            .background(ImageProvider(R.drawable.widget_background))
            .padding(WidgetRow.PADDING_DP.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when {
            // The first frame, before the database has answered. Drawing nothing beats
            // flashing "no habits yet" over four that exist.
            habits == null -> Unit
            habits.isEmpty() -> EmptyState()
            else -> {
                // One scale for every row, so four habits stay a grid rather than four
                // independently sized ones.
                val size = LocalSize.current
                val fit = WidgetRow.scale(
                    widthDp = size.width.value,
                    heightDp = size.height.value,
                    habits = habits.size,
                )
                habits.forEach { entry ->
                    // defaultWeight lives in ColumnScope, so the row's share of the height
                    // is decided here rather than inside HabitRow: four habits split it
                    // evenly, and a short placement shrinks them all instead of clipping
                    // the last.
                    HabitRow(
                        entry = entry,
                        today = today,
                        fit = fit,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = LocalContext.current.getString(R.string.widget_empty),
            style = TextStyle(
                color = ColorProvider(ShabitTextSecondary),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun HabitRow(
    entry: WidgetHabit,
    today: LocalDate,
    fit: Float,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    val habit = entry.habit
    val color = habitColor(habit.colorHex)
    val density = context.resources.displayMetrics.density
    val done = entry.completions.contains(today)

    val scale = fit.coerceAtLeast(WidgetRow.MIN_SCALE)
    val labelWidth = WidgetRow.LABEL_WIDTH_DP * scale
    val checkSize = WidgetCheck.SIZE_DP * scale
    val gaps = WidgetRow.GAPS_DP * scale

    // columnCount counts unscaled tiles, so the width it is handed has to come back out
    // of the scale first — and then the drawing puts it back in.
    val gridWidthDp = WidgetRow.gridWidthDp(LocalSize.current.width.value, scale)
    val columns =
        if (fit < WidgetRow.MIN_SCALE) 0 else WidgetGrid.columnCount(gridWidthDp / scale)
    val grid = remember(habit.id, habit.colorHex, columns, today, entry.completions, scale) {
        WidgetGrid.render(density, columns, today, entry.completions, color, scale)
    }
    val check = remember(habit.colorHex, done, density, scale) {
        WidgetCheck.render(context, density, done, color, scale)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.width(labelWidth.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(HabitIcons.resolve(habit.iconKey)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(color)),
                modifier = GlanceModifier.size((WidgetRow.ICON_DP * scale).dp),
            )
            Text(
                text = habit.name,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(ShabitTextPrimary),
                    fontSize = (WidgetRow.NAME_SP * scale).sp,
                ),
                modifier = GlanceModifier.padding(start = (WidgetRow.LABEL_GAP_DP * scale).dp),
            )
        }

        Box(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = (gaps / 2).dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (grid != null) {
                Image(
                    provider = ImageProvider(grid),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .size(checkSize.dp)
                .clickable(
                    actionRunCallback<ToggleHabitAction>(
                        ToggleHabitAction.parametersFor(habit.id),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(check),
                contentDescription = context.getString(
                    if (done) R.string.uncheck_today else R.string.check_today,
                ),
            )
        }
    }
}
