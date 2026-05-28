package com.example.simpleclouddrive.data.repository

import android.net.Uri
import android.util.Log
import com.example.simpleclouddrive.data.local.FileStorageManager
import com.example.simpleclouddrive.data.local.dao.CloudFileDao
import com.example.simpleclouddrive.data.local.dao.RecentTransferDao
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.local.entity.RecentTransferEntity
import com.example.simpleclouddrive.data.mapper.toDomain
import com.example.simpleclouddrive.data.mapper.toEntity
import com.example.simpleclouddrive.data.remote.FakeCloudApi
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType
import com.example.simpleclouddrive.domain.repository.FileRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FileRepositoryImpl(
    private val cloudFileDao: CloudFileDao,
    private val recentTransferDao: RecentTransferDao,
    private val fakeCloudApi: FakeCloudApi,
    private val fileStorageManager: FileStorageManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileRepository {
    override suspend fun initializeIfNeeded() {
        withContext(ioDispatcher) {
            val fileCount = cloudFileDao.count()
            if (fileCount > 0) {
                Log.d(TAG, "Mock data initialization skipped, cloud_file count=$fileCount")
                return@withContext
            }

            val files = fakeCloudApi.fetchFiles().map { dto -> dto.toEntity() }
            cloudFileDao.insertAll(files)
            Log.d(TAG, "Mock data initialization success, inserted=${files.size}")
        }
    }

    override fun observeFiles(parentId: String?): Flow<List<CloudFile>> {
        return cloudFileDao.observeFiles(parentId).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getFileById(fileId: String): CloudFile? {
        return withContext(ioDispatcher) {
            cloudFileDao.getFileById(fileId)?.toDomain()
        }
    }

    override suspend fun uploadLocalFile(uri: Uri, parentId: String?): CloudFile {
        return withContext(ioDispatcher) {
            val fileId = UUID.randomUUID().toString()
            val displayName = fileStorageManager.getDisplayName(uri)
            val size = fileStorageManager.getFileSize(uri)
            val mimeType = fileStorageManager.getMimeType(uri)
            val type = resolveFileType(mimeType, displayName)
            val privatePath = fileStorageManager.copyUriToPrivateStorage(
                uri = uri,
                fileId = fileId,
                displayName = displayName
            )
            val now = System.currentTimeMillis()
            val entity = CloudFileEntity(
                fileId = fileId,
                name = displayName,
                size = size,
                path = privatePath,
                parentId = parentId,
                type = type.name,
                timestamp = now,
                modifiedAt = now
            )

            cloudFileDao.insert(entity)
            recentTransferDao.upsert(
                RecentTransferEntity(
                    fileId = fileId,
                    transferTime = now
                )
            )
            entity.toDomain()
        }
    }

    private fun resolveFileType(mimeType: String?, displayName: String): FileType {
        val lowerName = displayName.lowercase()
        return when {
            mimeType == "text/plain" || lowerName.endsWith(".txt") -> FileType.TXT
            mimeType?.startsWith("video/") == true -> FileType.VIDEO
            else -> FileType.OTHER
        }
    }

    private companion object {
        const val TAG = "FileRepository"
    }
}
