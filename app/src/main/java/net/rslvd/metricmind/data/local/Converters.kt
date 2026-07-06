package net.rslvd.metricmind.data.local

import androidx.room.TypeConverter
import net.rslvd.metricmind.domain.model.HabitTemplate
import net.rslvd.metricmind.domain.model.MetricType
import net.rslvd.metricmind.domain.model.ReminderMode
import net.rslvd.metricmind.domain.model.TaskStatus
import net.rslvd.metricmind.domain.model.VitalType
import net.rslvd.metricmind.domain.model.VitalVerification

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

    @TypeConverter fun vitalToString(v: VitalType): String = v.name
    @TypeConverter fun stringToVital(v: String): VitalType = VitalType.valueOf(v)

    @TypeConverter fun verificationToString(v: VitalVerification): String = v.name
    @TypeConverter fun stringToVerification(v: String): VitalVerification = VitalVerification.valueOf(v)
}
