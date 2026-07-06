package net.rslvd.metricmind.di

import android.content.Context
import net.rslvd.metricmind.data.repository.HabitRepositoryImpl
import net.rslvd.metricmind.data.repository.MetricRepositoryImpl
import net.rslvd.metricmind.data.repository.ScreenTimeRepositoryImpl
import net.rslvd.metricmind.data.repository.VitalsRepositoryImpl
import net.rslvd.metricmind.domain.repository.HabitRepository
import net.rslvd.metricmind.domain.repository.MetricRepository
import net.rslvd.metricmind.domain.repository.ScreenTimeRepository
import net.rslvd.metricmind.domain.repository.VitalsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindMetricRepository(impl: MetricRepositoryImpl): MetricRepository

    @Binds @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds @Singleton
    abstract fun bindScreenTimeRepository(impl: ScreenTimeRepositoryImpl): ScreenTimeRepository

    @Binds @Singleton
    abstract fun bindVitalsRepository(impl: VitalsRepositoryImpl): VitalsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
