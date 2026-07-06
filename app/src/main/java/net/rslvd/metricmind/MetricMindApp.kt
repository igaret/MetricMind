package net.rslvd.metricmind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import net.rslvd.metricmind.billing.BillingManager
import net.rslvd.metricmind.habit.HabitScheduler
import net.rslvd.metricmind.habit.Notifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MetricMindApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var habitScheduler: HabitScheduler
    @Inject lateinit var notifications: Notifications
    @Inject lateinit var billingManager: BillingManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        habitScheduler.ensureScheduled()
        billingManager.start()
    }
}
