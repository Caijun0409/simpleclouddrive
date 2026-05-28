package com.example.simpleclouddrive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_browse")
data class RecentBrowseEntity(
    @PrimaryKey val fileId: String,
    val browseTime: Long
)
