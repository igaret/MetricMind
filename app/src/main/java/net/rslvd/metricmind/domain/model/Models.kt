package net.rslvd.metricmind.domain.model

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

/** Body-sensor vitals captured on demand (never in the background). */
enum class VitalType(val unit: String) {
    HEART_RATE("bpm"),
    BODY_TEMP("\u00b0C"),
    /** 0 = awake, 1 = asleep (heuristic estimate). */
    SLEEP_STATE(""),
}

/** User feedback on how accurate a sensor reading was. */
enum class VitalVerification { UNVERIFIED, CONFIRMED, CORRECTED }

data class VitalReading(
    val id: Long = 0,
    val type: VitalType,
    val value: Float,
    val timestamp: Long,
    val verification: VitalVerification = VitalVerification.UNVERIFIED,
    /** Value the user says is correct, when verification == CORRECTED. */
    val correctedValue: Float? = null,
)

/** Per-sensor accuracy derived from user verifications. */
data class VitalAccuracy(
    val type: VitalType,
    val total: Int,
    val confirmed: Int,
    val corrected: Int,
) {
    val verified: Int get() = confirmed + corrected
    /** Percent of verified readings the user confirmed as correct; null until something is verified. */
    val percentCorrect: Int? get() = if (verified == 0) null else (confirmed * 100) / verified
}

/** Result of the offline awake/asleep heuristic. */
data class SleepEstimate(
    val asleep: Boolean,
    /** 0..1 confidence in the estimate. */
    val confidence: Float,
)

/** A point pair for correlation/scatter analysis. */
data class CorrelationResult(
    val n: Int,
    val pearsonR: Float,
    val points: List<Pair<Float, Float>>,
)
