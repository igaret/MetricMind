package com.metricmind.domain.repository

import com.metricmind.domain.model.Habit
import com.metricmind.domain.model.HabitTask
import com.metricmind.domain.model.MetricEntry
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.model.ScreenTimeDay
import com.metricmind.domain.model.VitalAccuracy
import com.metricmind.domain.model.VitalReading
import com.metricmind.domain.model.VitalType
import com.metricmind.domain.model.VitalVerification
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MetricRepository {
    fun observe(type: MetricType, from: LocalDate, to: LocalDate): Flow<List<MetricEntry>>
    suspend fun upsert(entry: MetricEntry)
    suspend fun delete(type: MetricType, day: LocalDate)
    suspend fun range(type: MetricType, from: LocalDate, to: LocalDate): List<MetricEntry>
}

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeTasksFor(day: LocalDate): Flow<List<HabitTask>>
    suspend fun activeHabits(): List<Habit>
    suspend fun upsertHabit(habit: Habit): Long
    suspend fun deleteHabit(id: Long)
    suspend fun upsertTask(task: HabitTask)
    suspend fun setTaskStatus(taskId: Long, status: com.metricmind.domain.model.TaskStatus)
    suspend fun taskExists(habitId: Long, day: LocalDate): Boolean
    suspend fun currentStreak(habitId: Long, today: LocalDate): Int
}

interface VitalsRepository {
    fun observeRecent(type: VitalType, limit: Int = 20): Flow<List<VitalReading>>
    fun observeAllRecent(limit: Int = 20): Flow<List<VitalReading>>
    suspend fun record(type: VitalType, value: Float): VitalReading
    suspend fun verify(id: Long, verification: VitalVerification, correctedValue: Float? = null)
    suspend fun accuracy(type: VitalType): VitalAccuracy
}

interface ScreenTimeRepository {
    /** Reads daily totals from UsageStatsManager if access is granted; else null. */
    suspend fun todayUsage(): ScreenTimeDay?
    fun hasUsageAccess(): Boolean
}
