package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val apiKeyAlias: String,
    val rpmLimit: Int,
    val rpdLimit: Int,
    val contextWindow: Int,
    val maxOutput: Int,
    val tier: Int,
    val isByok: Boolean,
    val enabled: Boolean,
    val isCustom: Boolean = false,
    val authType: String = "API_KEY",
    val vertexProjectId: String? = null,
    val vertexLocation: String? = null,
    val selectedModel: String,
    val notes: String = "",
    val registrationUrl: String = ""
)
