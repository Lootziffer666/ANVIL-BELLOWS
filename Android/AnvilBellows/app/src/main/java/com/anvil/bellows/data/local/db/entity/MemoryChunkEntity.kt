package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_chunks")
data class MemoryChunkEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val projectId: String? = null,
    val content: String,
    val embeddingBlob: ByteArray,
    val tags: String = "",
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryChunkEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
