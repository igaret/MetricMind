package com.metricmind.data.repository

import com.metricmind.data.local.dao.HabitDao
import com.metricmind.data.local.dao.HabitTaskDao
import com.metricmind.data.local.entity.HabitEntity
import com.metricmind.data.local.entity.HabitTaskEntity
import com.metricmind.domain.model.Habit
import com.metricmind.domain.model.HabitTask
import com.metricmind.domain.model.TaskStatus
import com.metricmind.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val taskDao: HabitTaskDao,
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeHabits().map { list -> list.map { it.toDomain() } }

    override fun observeTasksFor(day: LocalDate): Flow<List<HabitTask>> =
        taskDao.observeForDay(day.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun activeHabits(): List<Habit> = habitDao.activeHabits().map { it.toDomain() }

    override suspend fun upsertHabit(habit: Habit): Long {
        val entity = HabitEntity(
            id = habit.id,
            title = habit.title,
            template = habit.template,
            reminderMode = habit.reminderMode,
            reminderMinute = habit.reminderMinute,
            active = habit.active,
            createdAt = System.currentTimeMillis(),
        )
        return if (habit.id == 0L) habitDao.insertHabit(entity)
        else { habitDao.updateHabit(entity); habit.id }
    }

    override suspend fun deleteHabit(id: Long) = habitDao.deleteHabit(id)

    override suspend fun upsertTask(task: HabitTask) {
        taskDao.insert(
            HabitTaskEntity(
                id = task.id,
                habitId = task.habitId,
                day = task.day.toEpochDay(),
                prompt = task.prompt,
                status = task.status,
                completedAt = null,
            ),
        )
    }

    override suspend fun setTaskStatus(taskId: Long, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.DONE) System.currentTimeMillis() else null
        taskDao.setStatus(taskId, status.name, completedAt)
    }

    override suspend fun taskExists(habitId: Long, day: LocalDate): Boolean =
        taskDao.countFor(habitId, day.toEpochDay()) > 0

    override suspend fun currentStreak(habitId: Long, today: LocalDate): Int {
        val done = taskDao.completedDays(habitId).toHashSet()
        var streak = 0
        var cursor = today
        // Count consecutive completed days ending today (or yesterday if today not yet done).
        if (!done.contains(cursor.toEpochDay())) cursor = cursor.minusDays(1)
        while (done.contains(cursor.toEpochDay())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun HabitEntity.toDomain() = Habit(
        id = id, title = title, template = template, reminderMode = reminderMode,
        reminderMinute = reminderMinute, active = active,
    )

    private fun HabitTaskEntity.toDomain() = HabitTask(
        id = id, habitId = habitId, day = LocalDate.ofEpochDay(day), prompt = prompt, status = status,
    )
}
