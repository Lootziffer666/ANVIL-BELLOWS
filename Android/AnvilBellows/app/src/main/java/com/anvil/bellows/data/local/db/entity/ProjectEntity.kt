package com.anvil.bellows.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String = "#FF5A5F",
    val icon: String = "folder",
    val parentId: String? = null,
    val createdAt: Long,
    val sortOrder: Int = 0
)
