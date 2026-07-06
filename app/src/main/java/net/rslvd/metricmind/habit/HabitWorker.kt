package net.rslvd.metricmind.habit

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.rslvd.metricmind.domain.repository.HabitRepository
import net.rslvd.metricmind.domain.repository.ScreenTimeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime

/**
 * Daily worker: (1) ensures today's micro-tasks exist, (2) fires the single daily reminder per
 * habit whose smart/fixed time has arrived and whose task is still pending. Idempotent — safe to
 * run multiple times per day (WorkManager may retry).
 */
@HiltWorker
class HabitWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: HabitEngine,
    private val habits: HabitRepository,
    private val screenTime: ScreenTimeRepository,
    private val notifications: Notifications,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        engine.generateFor(today)

        val nowMinute = LocalTime.now().hour * 60 + LocalTime.now().minute
        val hasUsage = screenTime.hasUsageAccess()
        val tasksToday = habits.activeHabits().associateBy { it.id }

        habits.activeHabits().forEach { habit ->
            val due = engine.reminderMinuteFor(habit, hasUsage) ?: return@forEach
            // Fire within a window after the due minute (worker runs periodically).
            if (nowMinute in due until (due + REMINDER_WINDOW_MIN)) {
                val prompt = engine.promptFor(habit, today)
                notifications.showHabitReminder(habit.id, tasksToday[habit.id]?.title ?: habit.title, prompt)
            }
        }
        return Result.success()
    }

    companion object {
        const val REMINDER_WINDOW_MIN = 30
    }
}
