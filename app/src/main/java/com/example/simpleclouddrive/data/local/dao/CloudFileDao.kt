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

    @Query("SELECT * FROM cloud_file WHERE parentId = :parentId")
    suspend fun getFilesByParentId(parentId: String): List<CloudFileEntity>

    @Query("SELECT * FROM cloud_file WHERE type = 'FOLDER' ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFolders(): List<CloudFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: CloudFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CloudFileEntity>)

    @Query("UPDATE cloud_file SET name = :name, modifiedAt = :modifiedAt WHERE fileId = :fileId")
    suspend fun updateName(fileId: String, name: String, modifiedAt: Long)

    @Query("UPDATE cloud_file SET parentId = :parentId, modifiedAt = :modifiedAt WHERE fileId = :fileId")
    suspend fun updateParent(fileId: String, parentId: String?, modifiedAt: Long)

    @Query("DELETE FROM cloud_file WHERE fileId IN (:fileIds)")
    suspend fun deleteByIds(fileIds: List<String>)
}
