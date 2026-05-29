package com.example.simpleclouddrive.domain.repository

import android.net.Uri
import com.example.simpleclouddrive.domain.model.CloudFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun initializeIfNeeded()

    fun observeFiles(parentId: String?): Flow<List<CloudFile>>

    fun observeFileCount(): Flow<Int>

    fun observeUsedBytes(): Flow<Long>

    fun observeRecentTransferFiles(limit: Int = 10): Flow<List<CloudFile>>

    fun observeRecentBrowseFiles(limit: Int = 10): Flow<List<CloudFile>>

    suspend fun getFileById(fileId: String): CloudFile?

    suspend fun uploadLocalFile(uri: Uri, parentId: String?): CloudFile

    suspend fun readTextFile(fileId: String): String

    suspend fun markFileBrowsed(fileId: String)

    suspend fun createShareLink(fileId: String): String

    suspend fun getSharedFiles(shareId: String): List<CloudFile>

    suspend fun renameFile(fileId: String, newName: String)

    suspend fun deleteFile(fileId: String)

    suspend fun moveFile(fileId: String, targetParentId: String?)

    suspend fun getMoveTargetFolders(excludeFileId: String): List<CloudFile>
}
