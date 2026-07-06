package net.rslvd.metricmind.di

import android.content.Context
import androidx.room.Room
import net.rslvd.metricmind.core.crypto.KeyManager
import net.rslvd.metricmind.data.local.MetricMindDatabase
import net.rslvd.metricmind.data.local.dao.HabitDao
import net.rslvd.metricmind.data.local.dao.HabitTaskDao
import net.rslvd.metricmind.data.local.dao.MetricDao
import net.rslvd.metricmind.data.local.dao.ScreenTimeDao
import net.rslvd.metricmind.data.local.dao.VitalDao
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
            .addMigrations(MetricMindDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides fun metricDao(db: MetricMindDatabase): MetricDao = db.metricDao()
    @Provides fun habitDao(db: MetricMindDatabase): HabitDao = db.habitDao()
    @Provides fun habitTaskDao(db: MetricMindDatabase): HabitTaskDao = db.habitTaskDao()
    @Provides fun screenTimeDao(db: MetricMindDatabase): ScreenTimeDao = db.screenTimeDao()
    @Provides fun vitalDao(db: MetricMindDatabase): VitalDao = db.vitalDao()
}
