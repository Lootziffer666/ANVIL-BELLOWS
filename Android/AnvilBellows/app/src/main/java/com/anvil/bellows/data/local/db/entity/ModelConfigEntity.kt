package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_configs",
    foreignKeys = [ForeignKey(
        entity = ProviderConfigEntity::class,
        parentColumns = ["id"],
        childColumns = ["providerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("providerId")]
)
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val contextWindow: Int,
    val maxOutput: Int,
    val rpmLimit: Int = Int.MAX_VALUE,
    val rpdLimit: Int = Int.MAX_VALUE,
    val supportsVision: Boolean = false,
    val supportsReasoning: Boolean = false,
    val specialties: String = "GENERAL"   // comma-separated Specialty names
)
