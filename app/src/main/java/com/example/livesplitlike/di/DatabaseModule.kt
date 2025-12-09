package com.example.livesplitlike.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.livesplitlike.data.local.db.AppDatabase
import com.example.livesplitlike.data.local.model.RunEntity
import com.example.livesplitlike.data.local.model.SplitEntity
import com.example.livesplitlike.data.local.db.RunDao
import com.example.livesplitlike.data.local.db.SplitDao
import com.example.livesplitlike.data.local.db.ComparisonDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        // Creamos la instancia y la usamos dentro del callback para pre-populate
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(appContext, AppDatabase::class.java, "livesplit_db")
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate en background
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val runId = instance.runDao().insertRun(
                                RunEntity(
                                    name = "Demo Run",
                                    startedAtMillis = 0L,
                                    createdAtMillis = System.currentTimeMillis()
                                )
                            )
                            val names = listOf("Start", "Segment 1", "Segment 2", "Segment 3", "Finish")
                            names.forEachIndexed { idx, name ->
                                instance.splitDao().insertSplit(
                                    SplitEntity(
                                        runId = runId,
                                        indexInRun = idx,
                                        name = name,
                                        timeFromStartMillis = -1L,
                                        recordedAtMillis = 0L
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Ignorar errores de pre-populate
                        }
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()

        return instance
    }

    @Provides
    fun provideRunDao(db: AppDatabase): RunDao = db.runDao()

    @Provides
    fun provideSplitDao(db: AppDatabase): SplitDao = db.splitDao()

    @Provides
    fun provideComparisonDao(db: AppDatabase): ComparisonDao = db.comparisonDao()
}