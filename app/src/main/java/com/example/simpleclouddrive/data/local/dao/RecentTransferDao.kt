package com.example.simpleclouddrive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simpleclouddrive.data.local.entity.RecentTransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentTransferDao {
    @Query("SELECT * FROM recent_transfer ORDER BY transferTime DESC")
    fun observeRecentTransfers(): Flow<List<RecentTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recentTransfer: RecentTransferEntity)
}
