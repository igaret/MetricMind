package net.rslvd.metricmind.habit

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the recurring [HabitWorker]. We use a periodic (inexact) request every ~30 min so
 * smart/fixed reminder windows are caught without exact-alarm permission or Doze headaches.
 * For users who choose an exact fixed time, swap in AlarmManager.setExactAndAllowWhileIdle.
 */
@Singleton
class HabitScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<HabitWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val WORK_NAME = "metricmind_habit_worker"
    }
}
