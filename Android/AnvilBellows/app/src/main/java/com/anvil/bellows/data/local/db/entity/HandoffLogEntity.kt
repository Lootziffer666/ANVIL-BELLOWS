package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "handoff_logs")
data class HandoffLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val fromProviderId: String,
    val toProviderId: String,
    val reason: String,
    val timestamp: Long,
    val messageCountAtHandoff: Int
)
