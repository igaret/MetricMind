package net.rslvd.metricmind.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import net.rslvd.metricmind.data.local.entity.HabitEntity
import net.rslvd.metricmind.data.local.entity.HabitTaskEntity
import net.rslvd.metricmind.data.local.entity.MetricEntryEntity
import net.rslvd.metricmind.data.local.entity.ScreenTimeDailyEntity
import net.rslvd.metricmind.data.local.entity.VitalReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {
    @Upsert
    suspend fun upsert(entry: MetricEntryEntity)

    @Query("SELECT * FROM metric_entry WHERE type = :type AND day BETWEEN :from AND :to ORDER BY day")
    fun observe(type: String, from: Long, to: Long): Flow<List<MetricEntryEntity>>

    @Query("SELECT * FROM metric_entry WHERE type = :type AND day BETWEEN :from AND :to ORDER BY day")
    suspend fun range(type: String, from: Long, to: Long): List<MetricEntryEntity>

    @Query("DELETE FROM metric_entry WHERE type = :type AND day = :day")
    suspend fun delete(type: String, day: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit ORDER BY createdAt DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE active = 1")
    suspend fun activeHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun deleteHabit(id: Long)
}

@Dao
interface HabitTaskDao {
    @Query("SELECT * FROM habit_task WHERE day = :day")
    fun observeForDay(day: Long): Flow<List<HabitTaskEntity>>

    @Query("SELECT COUNT(*) FROM habit_task WHERE habitId = :habitId AND day = :day")
    suspend fun countFor(habitId: Long, day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: HabitTaskEntity): Long

    @Query("UPDATE habit_task SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, completedAt: Long?)

    /** Returns the days this habit was completed, most recent first, for streak math. */
    @Query("SELECT day FROM habit_task WHERE habitId = :habitId AND status = 'DONE' ORDER BY day DESC")
    suspend fun completedDays(habitId: Long): List<Long>
}

@Dao
interface VitalDao {
    @Insert
    suspend fun insert(reading: VitalReadingEntity): Long

    @Query("SELECT * FROM vital_reading WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(type: String, limit: Int): Flow<List<VitalReadingEntity>>

    @Query("SELECT * FROM vital_reading ORDER BY timestamp DESC LIMIT :limit")
    fun observeAllRecent(limit: Int): Flow<List<VitalReadingEntity>>

    @Query("UPDATE vital_reading SET verification = :verification, correctedValue = :correctedValue WHERE id = :id")
    suspend fun setVerification(id: Long, verification: String, correctedValue: Float?)

    @Query("SELECT COUNT(*) FROM vital_reading WHERE type = :type")
    suspend fun countAll(type: String): Int

    @Query("SELECT COUNT(*) FROM vital_reading WHERE type = :type AND verification = :verification")
    suspend fun countByVerification(type: String, verification: String): Int
}

@Dao
interface ScreenTimeDao {
    @Upsert
    suspend fun upsert(entity: ScreenTimeDailyEntity)

    @Query("SELECT * FROM screen_time_daily WHERE day = :day")
    suspend fun forDay(day: Long): ScreenTimeDailyEntity?
}
