package net.rslvd.metricmind.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import net.rslvd.metricmind.domain.model.ScreenTimeDay
import net.rslvd.metricmind.domain.repository.ScreenTimeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject

class ScreenTimeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScreenTimeRepository {

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun todayUsage(): ScreenTimeDay? {
        if (!hasUsageAccess()) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            ?.filter { it.totalTimeInForeground > 0 }
            ?: return null
        if (stats.isEmpty()) return null
        val totalMs = stats.sumOf { it.totalTimeInForeground }
        val top = stats.maxByOrNull { it.totalTimeInForeground }
        return ScreenTimeDay(
            day = LocalDate.now(ZoneId.systemDefault()),
            totalMinutes = (totalMs / 60_000L).toInt(),
            topPackage = top?.packageName,
            topMinutes = ((top?.totalTimeInForeground ?: 0L) / 60_000L).toInt(),
        )
    }
}
