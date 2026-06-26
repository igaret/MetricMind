package com.metricmind.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.metricmind.domain.model.HabitTemplate
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.model.ReminderMode
import com.metricmind.domain.model.TaskStatus

@Entity(
    tableName = "metric_entry",
    indices = [
        Index(value = ["type", "day"], unique = true),
    ],
)
data class MetricEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MetricType,
    val value: Float,
    val note: String?,
    /** epochDay (LocalDate.toEpochDay()) */
    val day: Long,
    val createdAt: Long,
)

@Entity(tableName = "symptom_tag", indices = [Index(value = ["name"], unique = true)])
data class SymptomTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "symptom_log",
    foreignKeys = [
        ForeignKey(
            entity = SymptomTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("day"), Index("tagId")],
)
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val tagId: Long,
)

@Entity(tableName = "screen_time_daily")
data class ScreenTimeDailyEntity(
    @PrimaryKey val day: Long,
    val totalMinutes: Int,
    val topPackage: String?,
    val topMinutes: Int,
)

@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val template: HabitTemplate,
    val reminderMode: ReminderMode,
    val reminderMinute: Int?,
    val active: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "habit_task",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId", "day"], unique = true)],
)
data class HabitTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val day: Long,
    val prompt: String,
    val status: TaskStatus,
    val completedAt: Long?,
)
