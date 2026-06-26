package com.metricmind.di

import android.content.Context
import com.metricmind.data.repository.HabitRepositoryImpl
import com.metricmind.data.repository.MetricRepositoryImpl
import com.metricmind.data.repository.ScreenTimeRepositoryImpl
import com.metricmind.domain.repository.HabitRepository
import com.metricmind.domain.repository.MetricRepository
import com.metricmind.domain.repository.ScreenTimeRepository
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
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
