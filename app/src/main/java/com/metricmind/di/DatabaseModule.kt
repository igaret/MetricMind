package com.metricmind.di

import android.content.Context
import androidx.room.Room
import com.metricmind.core.crypto.KeyManager
import com.metricmind.data.local.MetricMindDatabase
import com.metricmind.data.local.dao.HabitDao
import com.metricmind.data.local.dao.HabitTaskDao
import com.metricmind.data.local.dao.MetricDao
import com.metricmind.data.local.dao.ScreenTimeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: KeyManager,
    ): MetricMindDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(keyManager.databasePassphrase())
        return Room.databaseBuilder(context, MetricMindDatabase::class.java, MetricMindDatabase.NAME)
            .openHelperFactory(factory)
            // Migrations are added explicitly as the schema evolves; no destructive fallback.
            .build()
    }

    @Provides fun metricDao(db: MetricMindDatabase): MetricDao = db.metricDao()
    @Provides fun habitDao(db: MetricMindDatabase): HabitDao = db.habitDao()
    @Provides fun habitTaskDao(db: MetricMindDatabase): HabitTaskDao = db.habitTaskDao()
    @Provides fun screenTimeDao(db: MetricMindDatabase): ScreenTimeDao = db.screenTimeDao()
}
