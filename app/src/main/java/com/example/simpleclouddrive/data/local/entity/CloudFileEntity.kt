package com.example.simpleclouddrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_file")
data class CloudFileEntity(
    @PrimaryKey val fileId: String,
    val name: String,
    val size: Long,
    val path: String,
    val parentId: String?,
    val type: String,
    val timestamp: Long,
    val modifiedAt: Long
)
