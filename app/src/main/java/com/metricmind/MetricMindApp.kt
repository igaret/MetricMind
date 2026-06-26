package com.metricmind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.metricmind.billing.BillingManager
import com.metricmind.habit.HabitScheduler
import com.metricmind.habit.Notifications
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
