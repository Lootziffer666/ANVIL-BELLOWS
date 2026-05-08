package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "request_logs",
    indices = [Index(value = ["providerId", "timestamp"])]
)
data class RequestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: String,
    val modelId: String,
    val sessionId: String,
    val timestamp: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMs: Long = 0L,
    val wasHandoff: Boolean = false,
    val errorCode: Int? = null
)
