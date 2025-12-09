package com.example.livesplitlike.di

import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.db.SplitDao
import com.example.livesplitlike.data.repositories.RunRepository
import com.example.livesplitlike.data.repositories.SplitRepository
import com.example.livesplitlike.data.repositories.impl.RunRepositoryImpl
import com.example.livesplitlike.data.repositories.impl.SplitRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRunRepository(runDao: RunDao): RunRepository = RunRepositoryImpl(runDao)

    @Provides
    @Singleton
    fun provideSplitRepository(splitDao: SplitDao): SplitRepository = SplitRepositoryImpl(splitDao)
}