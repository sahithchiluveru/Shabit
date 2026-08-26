package com.sahith.shabit.data

import android.content.Context
import androidx.room.withTransaction
import androidx.glance.appwidget.updateAll
import com.sahith.shabit.widget.ShabitWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * The only door into Shabit's data, for the app and for the widget alike. Both consumers
 * hit this same API so that a tap on the home screen and a tap in the app cannot disagree
 * about what a completion is.
 *
 * The [clock] is injectable so tests can pin "now"; production uses the system zone.
 *
 * @param onDataChanged run after every write that lands. In production this redraws the
 *   widget: Glance only collects these flows while its session is alive, so a change made
 *   in the app while the home screen is elsewhere would otherwise leave the last
 *   `RemoteViews` on display until something else happened to refresh them. Tests leave it
 *   as the no-op it defaults to.
 */
class HabitRepository(
    private val database: ShabitDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onDataChanged: suspend () -> Unit = {},
) {
    private val habits = database.habitDao()
    private val completions = database.completionDao()

    /** Today under the 4am rule. Callers must use this rather than `LocalDate.now()`. */
    fun today(): LocalDate = habitToday(clock)

    fun activeHabits(): Flow<List<Habit>> = habits.activeHabits()

    /**
     * Archived habits for the settings screen, newest first, each with the size and span
     * of the grid it is keeping.
     */
    fun archivedHabits(): Flow<List<ArchivedHabit>> = habits.archivedHabits().map { rows ->
        rows.map { row ->
            ArchivedHabit(
                habit = row.habit,
                completionCount = row.completionCount,
                archivedDate = habitDate(checkNotNull(row.habit.archivedAt), clock.zone),
            )
        }
    }

    /** The filled tiles for one habit. A date is present iff its tile is filled. */
    fun completions(habitId: Long): Flow<Set<LocalDate>> =
        completions.dates(habitId).map { it.toSet() }

    /**
     * Fill the tile if it is empty, empty it if it is filled.
     *
     * The check and the write share a transaction so two taps arriving together — the
     * widget and the app both firing, say — cannot both read "absent" and both insert.
     */
    suspend fun toggle(habitId: Long, date: LocalDate) {
        database.withTransaction {
            if (completions.exists(habitId, date)) {
                completions.delete(habitId, date)
            } else {
                completions.insert(Completion(habitId, date))
            }
        }
        onDataChanged()
    }

    /** One habit by id, or null once it has been deleted. */
    suspend fun habit(habitId: Long): Habit? = habits.habit(habitId)

    /**
     * Insert a new habit and return its id, or null when all four slots are already taken.
     *
     * The count and the insert share a transaction because the cap is a real invariant, not
     * a UI nicety: the dashboard's disabled + button is the polite refusal, and this is the
     * one that actually holds when something bypasses it.
     */
    suspend fun create(habit: Habit): Long? = database.withTransaction {
        if (habits.activeCount() >= MAX_ACTIVE_HABITS) null else habits.insert(habit)
    }.also { onDataChanged() }

    /**
     * Edit the four fields the add/edit screen owns. [Habit.createdDate] and the habit's
     * completions are untouchable from here by construction — see [HabitDao.updateDetails].
     */
    suspend fun updateDetails(
        habitId: Long,
        name: String,
        description: String,
        iconKey: String,
        colorHex: String,
    ) {
        habits.updateDetails(habitId, name, description, iconKey, colorHex)
        onDataChanged()
    }

    /** Frees one of the four active slots. The grid is kept — this is not a soft delete. */
    suspend fun archive(habitId: Long) {
        habits.setArchivedAt(habitId, Instant.now(clock))
        onDataChanged()
    }

    /**
     * Return an archived habit to the dashboard, or refuse with false when all four slots
     * are taken. Same transaction, same reason as [create]: the cap is the invariant, and
     * a disabled button is only the polite half of enforcing it.
     */
    suspend fun restore(habitId: Long): Boolean = database.withTransaction {
        if (habits.activeCount() >= MAX_ACTIVE_HABITS) {
            false
        } else {
            habits.setArchivedAt(habitId, null)
            true
        }
    }.also { onDataChanged() }

    /** Destroys the habit *and* its history. Confirm with the user before calling this. */
    suspend fun delete(habitId: Long) {
        habits.delete(habitId)
        onDataChanged()
    }

    /** How many of the four slots are taken. */
    suspend fun activeCount(): Int = habits.activeCount()

    companion object {
        /** The hard cap on active habits. See decision 12 in #1. */
        const val MAX_ACTIVE_HABITS = 4

        @Volatile
        private var instance: HabitRepository? = null

        /**
         * The one repository the app process shares, widget included.
         *
         * This is the only place the data package names anything in the UI, and it is
         * deliberate: the repository is the single door every write goes through, so it is
         * also the only place that can promise the widget hears about all of them.
         */
        fun getInstance(context: Context): HabitRepository {
            val application = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: HabitRepository(
                    database = ShabitDatabase.getInstance(application),
                    onDataChanged = { ShabitWidget.updateAll(application) },
                ).also { instance = it }
            }
        }
    }
}
