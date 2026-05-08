package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation_messages",
    foreignKeys = [ForeignKey(
        entity = ConversationSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val providerId: String? = null,
    val modelId: String? = null,
    val isHandoffBoundary: Boolean = false,
    val tokenCount: Int = 0
)
