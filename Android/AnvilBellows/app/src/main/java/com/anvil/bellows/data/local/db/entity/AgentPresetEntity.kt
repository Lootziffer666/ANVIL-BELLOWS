package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_presets")
data class AgentPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val systemPrompt: String = "",
    val defaultProviderId: String? = null,
    val defaultModelId: String? = null,
    val enabledToolIdsJson: String = "[]",
    val fileScopeIdsJson: String = "[]",
    val functionSchemaJson: String? = null,
    val isPinned: Boolean = false,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
