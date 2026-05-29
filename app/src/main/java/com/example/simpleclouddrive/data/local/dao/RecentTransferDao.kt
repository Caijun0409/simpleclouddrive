package com.example.simpleclouddrive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.local.entity.RecentTransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentTransferDao {
    @Query("SELECT * FROM recent_transfer ORDER BY transferTime DESC")
    fun observeRecentTransfers(): Flow<List<RecentTransferEntity>>

    @Query(
        """
        SELECT cloud_file.* FROM recent_transfer
        INNER JOIN cloud_file ON cloud_file.fileId = recent_transfer.fileId
        ORDER BY recent_transfer.transferTime DESC
        LIMIT :limit
        """
    )
    fun observeRecentTransferFiles(limit: Int): Flow<List<CloudFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recentTransfer: RecentTransferEntity)

    @Query("DELETE FROM recent_transfer WHERE fileId IN (:fileIds)")
    suspend fun deleteByFileIds(fileIds: List<String>)
}
