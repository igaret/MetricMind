package com.metricmind.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.metricmind.data.local.dao.HabitDao
import com.metricmind.data.local.dao.HabitTaskDao
import com.metricmind.data.local.dao.MetricDao
import com.metricmind.data.local.dao.ScreenTimeDao
import com.metricmind.data.local.dao.VitalDao
import com.metricmind.data.local.entity.HabitEntity
import com.metricmind.data.local.entity.HabitTaskEntity
import com.metricmind.data.local.entity.MetricEntryEntity
import com.metricmind.data.local.entity.ScreenTimeDailyEntity
import com.metricmind.data.local.entity.SymptomLogEntity
import com.metricmind.data.local.entity.SymptomTagEntity
import com.metricmind.data.local.entity.VitalReadingEntity

@Database(
    entities = [
        MetricEntryEntity::class,
        SymptomTagEntity::class,
        SymptomLogEntity::class,
        ScreenTimeDailyEntity::class,
        HabitEntity::class,
        HabitTaskEntity::class,
        VitalReadingEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MetricMindDatabase : RoomDatabase() {
    abstract fun metricDao(): MetricDao
    abstract fun habitDao(): HabitDao
    abstract fun habitTaskDao(): HabitTaskDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun vitalDao(): VitalDao

    companion object {
        const val NAME = "metricmind.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vital_reading` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`value` REAL NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`verification` TEXT NOT NULL, " +
                        "`correctedValue` REAL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vital_reading_type_timestamp` " +
                        "ON `vital_reading` (`type`, `timestamp`)",
                )
            }
        }
    }
}
