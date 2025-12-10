package com.example.livesplitlike.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.livesplitlike.data.local.db.AppDatabase
import com.example.livesplitlike.data.local.model.GroupEntity
import com.example.livesplitlike.data.local.model.SplitTemplateEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton
import androidx.room.RoomDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(ctx, AppDatabase::class.java, "livesplit_db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulate: un grupo de prueba con 5 splits
                    // Dentro de DatabaseModule provideDatabase, en onCreate:
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val groupId = instance.groupDao().insertGroup(GroupEntity(name = "Demo Group"))
                            val names = listOf("Start", "Segment 1", "Segment 2", "Segment 3", "Finish")
                            names.forEachIndexed { idx, name ->
                                instance.splitTemplateDao().insertTemplate(
                                    SplitTemplateEntity(
                                        groupId = groupId,
                                        indexInGroup = idx,
                                        name = name
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // swallow to avoid crash on first launch
                        }
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
        return instance
    }

    @Provides fun provideGroupDao(db: AppDatabase) = db.groupDao()
    @Provides fun provideSplitTemplateDao(db: AppDatabase) = db.splitTemplateDao()
    @Provides fun provideRunDao(db: AppDatabase) = db.runDao()
    @Provides fun provideRunTimeDao(db: AppDatabase) = db.runTimeDao()
}