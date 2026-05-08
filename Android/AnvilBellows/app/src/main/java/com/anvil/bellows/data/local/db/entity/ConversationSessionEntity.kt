package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_sessions",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("projectId")]
)
data class ConversationSessionEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val currentProviderId: String,
    val currentModelId: String,
    val handoffCount: Int = 0,
    val systemPrompt: String = "",
    val presetId: String? = null
)
