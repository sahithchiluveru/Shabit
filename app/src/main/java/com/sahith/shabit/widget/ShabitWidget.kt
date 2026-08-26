package com.sahith.shabit.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
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

/** Width reserved for a habit's icon and name. */
private const val LABEL_WIDTH_DP = 64f

/** Padding inside the widget's own card, on every edge. */
private const val WIDGET_PADDING_DP = 10f

/** The gaps between label, grid and check button in one row. */
private const val ROW_GAPS_DP = 8f

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
     * Too narrow for a grid. Everything still works — the name and the check button are
     * the parts that matter — the history just isn't drawn, rather than being squashed
     * into two illegible columns.
     */
    private val Compact = DpSize(150.dp, 110.dp)

    /** The target placement from #7: 4x4, about 250x230dp. */
    private val Full = DpSize(250.dp, 180.dp)

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(Compact, Full))

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
            .padding(WIDGET_PADDING_DP.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when {
            // The first frame, before the database has answered. Drawing nothing beats
            // flashing "no habits yet" over four that exist.
            habits == null -> Unit
            habits.isEmpty() -> EmptyState()
            else -> habits.forEach { entry ->
                // defaultWeight lives in ColumnScope, so the row's share of the height is
                // decided here rather than inside HabitRow: four habits split it evenly,
                // and a short placement shrinks them all instead of clipping the last.
                HabitRow(
                    entry = entry,
                    today = today,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
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
private fun HabitRow(entry: WidgetHabit, today: LocalDate, modifier: GlanceModifier) {
    val context = LocalContext.current
    val habit = entry.habit
    val color = habitColor(habit.colorHex)
    val density = context.resources.displayMetrics.density
    val done = entry.completions.contains(today)

    val gridWidthDp = LocalSize.current.width.value -
        LABEL_WIDTH_DP - WidgetCheck.SIZE_DP - 2 * WIDGET_PADDING_DP - ROW_GAPS_DP
    val columns = WidgetGrid.columnCount(gridWidthDp)
    val grid = remember(habit.id, habit.colorHex, columns, today, entry.completions) {
        WidgetGrid.render(density, columns, today, entry.completions, color)
    }
    val check = remember(habit.colorHex, done, density) {
        WidgetCheck.render(context, density, done, color)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.width(LABEL_WIDTH_DP.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(HabitIcons.resolve(habit.iconKey)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(color)),
                modifier = GlanceModifier.size(14.dp),
            )
            Text(
                text = habit.name,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(ShabitTextPrimary), fontSize = 11.sp),
                modifier = GlanceModifier.padding(start = 4.dp),
            )
        }

        Box(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
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
                .size(WidgetCheck.SIZE_DP.dp)
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
