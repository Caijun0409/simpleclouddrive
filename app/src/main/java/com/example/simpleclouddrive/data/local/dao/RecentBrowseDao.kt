package com.example.simpleclouddrive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.local.entity.RecentBrowseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentBrowseDao {
    @Query("SELECT * FROM recent_browse ORDER BY browseTime DESC")
    fun observeRecentBrowses(): Flow<List<RecentBrowseEntity>>

    @Query(
        """
        SELECT cloud_file.* FROM recent_browse
        INNER JOIN cloud_file ON cloud_file.fileId = recent_browse.fileId
        ORDER BY recent_browse.browseTime DESC
        LIMIT :limit
        """
    )
    fun observeRecentBrowseFiles(limit: Int): Flow<List<CloudFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recentBrowse: RecentBrowseEntity)

    @Query("DELETE FROM recent_browse WHERE fileId IN (:fileIds)")
    suspend fun deleteByFileIds(fileIds: List<String>)
}
