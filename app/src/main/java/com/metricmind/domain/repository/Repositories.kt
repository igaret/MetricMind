package com.metricmind.domain.repository

import com.metricmind.domain.model.Habit
import com.metricmind.domain.model.HabitTask
import com.metricmind.domain.model.MetricEntry
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.model.ScreenTimeDay
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

interface ScreenTimeRepository {
    /** Reads daily totals from UsageStatsManager if access is granted; else null. */
    suspend fun todayUsage(): ScreenTimeDay?
    fun hasUsageAccess(): Boolean
}
