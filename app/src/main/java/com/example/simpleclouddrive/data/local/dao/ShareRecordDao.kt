package com.example.simpleclouddrive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simpleclouddrive.data.local.entity.ShareRecordEntity

@Dao
interface ShareRecordDao {
    @Query("SELECT * FROM share_record WHERE shareId = :shareId LIMIT 1")
    suspend fun getShareRecord(shareId: String): ShareRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shareRecord: ShareRecordEntity)
}
