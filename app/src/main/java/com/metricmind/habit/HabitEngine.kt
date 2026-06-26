package com.metricmind.habit

import com.metricmind.domain.model.Habit
import com.metricmind.domain.model.HabitTask
import com.metricmind.domain.model.HabitTemplate
import com.metricmind.domain.model.ReminderMode
import com.metricmind.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Pure-ish engine that turns a habit + a day into exactly one micro-task, and computes the
 * reminder time. Generation is deterministic per (habitId, day) so re-running the daily worker
 * is idempotent and produces the same prompt.
 */
@Singleton
class HabitEngine @Inject constructor(
    private val habits: HabitRepository,
) {
    /** Ensures today's tasks exist for all active habits. Safe to call repeatedly. */
    suspend fun generateFor(day: LocalDate) {
        habits.activeHabits().forEach { habit ->
            if (!habits.taskExists(habit.id, day)) {
                habits.upsertTask(
                    HabitTask(
                        habitId = habit.id,
                        day = day,
                        prompt = promptFor(habit, day),
                    ),
                )
            }
        }
    }

    fun promptFor(habit: Habit, day: LocalDate): String {
        val pool = TEMPLATE_PROMPTS[habit.template] ?: listOf(habit.title)
        // Deterministic rotation seeded by habit + day.
        val idx = (abs((habit.id * 31 + day.toEpochDay()).toInt())) % pool.size
        return pool[idx]
    }

    /** Returns minutes-from-midnight for the single daily reminder, or null if NONE. */
    fun reminderMinuteFor(habit: Habit, hasUsageAccess: Boolean): Int? = when (habit.reminderMode) {
        ReminderMode.NONE -> null
        ReminderMode.FIXED -> habit.reminderMinute
        ReminderMode.SMART -> smartDefault(habit.template)
    }

    private fun smartDefault(template: HabitTemplate): Int = when (template) {
        HabitTemplate.WATER -> 14 * 60        // mid-afternoon
        HabitTemplate.SPANISH_VOCAB -> 19 * 60 // evening study
        HabitTemplate.DECLUTTER -> 18 * 60
        HabitTemplate.GRATITUDE -> 21 * 60     // wind-down
        HabitTemplate.CUSTOM -> 9 * 60
    }

    companion object {
        val TEMPLATE_PROMPTS: Map<HabitTemplate, List<String>> = mapOf(
            HabitTemplate.WATER to listOf(
                "Drink one glass of water now 💧",
                "Hydrate — a full glass of water 💧",
                "Quick water break. One glass.",
            ),
            HabitTemplate.SPANISH_VOCAB to listOf(
                "Learn: agua = water",
                "Learn: gracias = thank you",
                "Learn: mañana = tomorrow",
                "Learn: trabajo = work",
            ),
            HabitTemplate.DECLUTTER to listOf(
                "Find one item to toss or donate",
                "Clear one surface for 60 seconds",
                "Remove one thing you don't use",
            ),
            HabitTemplate.GRATITUDE to listOf(
                "Name one thing you're grateful for",
                "Who made your day a little better?",
                "Write one small win from today",
            ),
        )
    }
}
