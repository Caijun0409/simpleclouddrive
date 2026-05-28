package com.example.simpleclouddrive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.simpleclouddrive.data.local.dao.CloudFileDao
import com.example.simpleclouddrive.data.local.dao.RecentBrowseDao
import com.example.simpleclouddrive.data.local.dao.RecentTransferDao
import com.example.simpleclouddrive.data.local.dao.ShareRecordDao
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.local.entity.RecentBrowseEntity
import com.example.simpleclouddrive.data.local.entity.RecentTransferEntity
import com.example.simpleclouddrive.data.local.entity.ShareRecordEntity

@Database(
    entities = [
        CloudFileEntity::class,
        RecentBrowseEntity::class,
        RecentTransferEntity::class,
        ShareRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cloudFileDao(): CloudFileDao

    abstract fun recentBrowseDao(): RecentBrowseDao

    abstract fun recentTransferDao(): RecentTransferDao

    abstract fun shareRecordDao(): ShareRecordDao
}
