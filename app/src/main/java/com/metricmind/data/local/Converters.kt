package com.metricmind.data.local

import androidx.room.TypeConverter
import com.metricmind.domain.model.HabitTemplate
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.model.ReminderMode
import com.metricmind.domain.model.TaskStatus

/** Enums are stored by name (stable across versions; never reorder/rename without a migration). */
class Converters {
    @TypeConverter fun metricToString(v: MetricType): String = v.name
    @TypeConverter fun stringToMetric(v: String): MetricType = MetricType.valueOf(v)

    @TypeConverter fun templateToString(v: HabitTemplate): String = v.name
    @TypeConverter fun stringToTemplate(v: String): HabitTemplate = HabitTemplate.valueOf(v)

    @TypeConverter fun reminderToString(v: ReminderMode): String = v.name
    @TypeConverter fun stringToReminder(v: String): ReminderMode = ReminderMode.valueOf(v)

    @TypeConverter fun statusToString(v: TaskStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): TaskStatus = TaskStatus.valueOf(v)
}
