package com.anvil.bellows.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anvil.bellows.data.local.db.dao.*
import com.anvil.bellows.data.local.db.entity.*

@Database(
    entities = [
        ProviderConfigEntity::class,
        ModelConfigEntity::class,
        RequestLogEntity::class,
        ProjectEntity::class,
        ConversationSessionEntity::class,
        ConversationMessageEntity::class,
        MemoryChunkEntity::class,
        HandoffLogEntity::class,
        AgentPresetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerConfigDao(): ProviderConfigDao
    abstract fun modelConfigDao(): ModelConfigDao
    abstract fun requestLogDao(): RequestLogDao
    abstract fun projectDao(): ProjectDao
    abstract fun conversationSessionDao(): ConversationSessionDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun memoryChunkDao(): MemoryChunkDao
    abstract fun handoffLogDao(): HandoffLogDao
    abstract fun agentPresetDao(): AgentPresetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE provider_configs ADD COLUMN registrationUrl TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE model_configs ADD COLUMN specialties TEXT NOT NULL DEFAULT 'GENERAL'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS agent_presets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        systemPrompt TEXT NOT NULL DEFAULT '',
                        defaultProviderId TEXT,
                        defaultModelId TEXT,
                        enabledToolIdsJson TEXT NOT NULL DEFAULT '[]',
                        fileScopeIdsJson TEXT NOT NULL DEFAULT '[]',
                        functionSchemaJson TEXT,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("ALTER TABLE conversation_sessions ADD COLUMN presetId TEXT")
            }
        }
    }
}
