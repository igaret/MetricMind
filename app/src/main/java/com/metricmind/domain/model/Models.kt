package com.metricmind.domain.model

import java.time.LocalDate

/** Numeric, chartable metrics the user logs once per day. */
enum class MetricType(val unit: String, val min: Float, val max: Float) {
    MOOD("1-5", 1f, 5f),
    SLEEP("hours", 0f, 16f),
    ENERGY("1-5", 1f, 5f),
    PRODUCTIVITY("1-5", 1f, 5f),
    CAFFEINE("mg", 0f, 1000f);
}

data class MetricEntry(
    val id: Long = 0,
    val type: MetricType,
    val value: Float,
    val note: String? = null,
    val day: LocalDate,
)

data class SymptomLog(
    val day: LocalDate,
    val tags: List<String>,
)

data class ScreenTimeDay(
    val day: LocalDate,
    val totalMinutes: Int,
    val topPackage: String?,
    val topMinutes: Int,
)

enum class HabitTemplate(val displayName: String) {
    WATER("Water reminder"),
    SPANISH_VOCAB("Spanish vocab"),
    DECLUTTER("Declutter one item"),
    GRATITUDE("Gratitude prompt"),
    CUSTOM("Custom"), // premium
}

enum class ReminderMode { FIXED, SMART, NONE }

enum class TaskStatus { PENDING, DONE, SKIPPED }

data class Habit(
    val id: Long = 0,
    val title: String,
    val template: HabitTemplate,
    val reminderMode: ReminderMode = ReminderMode.SMART,
    /** Minutes from midnight for FIXED reminders; null for SMART/NONE. */
    val reminderMinute: Int? = null,
    val active: Boolean = true,
)

data class HabitTask(
    val id: Long = 0,
    val habitId: Long,
    val day: LocalDate,
    val prompt: String,
    val status: TaskStatus = TaskStatus.PENDING,
)

/** A point pair for correlation/scatter analysis. */
data class CorrelationResult(
    val n: Int,
    val pearsonR: Float,
    val points: List<Pair<Float, Float>>,
)
