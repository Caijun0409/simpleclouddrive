package com.example.simpleclouddrive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudFileDao {
    @Query("SELECT COUNT(*) FROM cloud_file")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM cloud_file
        WHERE (:parentId IS NULL AND parentId IS NULL) OR parentId = :parentId
        ORDER BY type ASC, modifiedAt DESC
        """
    )
    fun observeFiles(parentId: String?): Flow<List<CloudFileEntity>>

    @Query("SELECT * FROM cloud_file WHERE fileId = :fileId LIMIT 1")
    suspend fun getFileById(fileId: String): CloudFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: CloudFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CloudFileEntity>)
}
