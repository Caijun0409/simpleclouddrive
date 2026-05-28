package com.example.simpleclouddrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "share_record")
data class ShareRecordEntity(
    @PrimaryKey val shareId: String,
    val fileIdsJson: String,
    val createdAt: Long
)
