package com.anvil.bellows.di

import android.content.Context
import androidx.room.Room
import com.anvil.bellows.data.local.db.AppDatabase
import com.anvil.bellows.data.local.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "anvil_bellows.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProviderConfigDao(db: AppDatabase) = db.providerConfigDao()
    @Provides fun provideModelConfigDao(db: AppDatabase) = db.modelConfigDao()
    @Provides fun provideRequestLogDao(db: AppDatabase) = db.requestLogDao()
    @Provides fun provideProjectDao(db: AppDatabase) = db.projectDao()
    @Provides fun provideConversationSessionDao(db: AppDatabase) = db.conversationSessionDao()
    @Provides fun provideConversationMessageDao(db: AppDatabase) = db.conversationMessageDao()
    @Provides fun provideMemoryChunkDao(db: AppDatabase) = db.memoryChunkDao()
    @Provides fun provideHandoffLogDao(db: AppDatabase) = db.handoffLogDao()
    @Provides fun provideAgentPresetDao(db: AppDatabase) = db.agentPresetDao()
}
