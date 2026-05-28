package com.example.simpleclouddrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_transfer")
data class RecentTransferEntity(
    @PrimaryKey val fileId: String,
    val transferTime: Long
)
