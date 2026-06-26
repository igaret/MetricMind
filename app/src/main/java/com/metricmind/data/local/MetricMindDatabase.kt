package com.metricmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.metricmind.data.local.dao.HabitDao
import com.metricmind.data.local.dao.HabitTaskDao
import com.metricmind.data.local.dao.MetricDao
import com.metricmind.data.local.dao.ScreenTimeDao
import com.metricmind.data.local.entity.HabitEntity
import com.metricmind.data.local.entity.HabitTaskEntity
import com.metricmind.data.local.entity.MetricEntryEntity
import com.metricmind.data.local.entity.ScreenTimeDailyEntity
import com.metricmind.data.local.entity.SymptomLogEntity
import com.metricmind.data.local.entity.SymptomTagEntity

@Database(
    entities = [
        MetricEntryEntity::class,
        SymptomTagEntity::class,
        SymptomLogEntity::class,
        ScreenTimeDailyEntity::class,
        HabitEntity::class,
        HabitTaskEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MetricMindDatabase : RoomDatabase() {
    abstract fun metricDao(): MetricDao
    abstract fun habitDao(): HabitDao
    abstract fun habitTaskDao(): HabitTaskDao
    abstract fun screenTimeDao(): ScreenTimeDao

    companion object {
        const val NAME = "metricmind.db"
        // Add Migration(1,2)... here as the schema evolves. Never use destructive fallback in release.
    }
}
