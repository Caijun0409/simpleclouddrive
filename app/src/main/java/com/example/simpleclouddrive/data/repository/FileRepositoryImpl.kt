package com.example.simpleclouddrive.data.repository

import android.net.Uri
import android.util.Log
import com.example.simpleclouddrive.data.local.FileStorageManager
import com.example.simpleclouddrive.data.local.dao.CloudFileDao
import com.example.simpleclouddrive.data.local.dao.RecentBrowseDao
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
    private val recentBrowseDao: RecentBrowseDao,
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

    override suspend fun renameFile(fileId: String, newName: String) {
        withContext(ioDispatcher) {
            val trimmedName = newName.trim()
            require(trimmedName.isNotEmpty()) { "文件名不能为空" }
            val file = cloudFileDao.getFileById(fileId) ?: error("文件不存在")
            cloudFileDao.updateName(
                fileId = file.fileId,
                name = trimmedName,
                modifiedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun deleteFile(fileId: String) {
        withContext(ioDispatcher) {
            val file = cloudFileDao.getFileById(fileId) ?: error("文件不存在")
            val filesToDelete = collectDescendants(file)
            val fileIds = filesToDelete.map { entity -> entity.fileId }

            filesToDelete
                .filter { entity -> entity.type != FileType.FOLDER.name }
                .filter { entity -> fileStorageManager.isPrivateCloudFile(entity.path) }
                .forEach { entity -> fileStorageManager.deletePrivateFile(entity.path) }

            recentBrowseDao.deleteByFileIds(fileIds)
            recentTransferDao.deleteByFileIds(fileIds)
            cloudFileDao.deleteByIds(fileIds)
        }
    }

    override suspend fun moveFile(fileId: String, targetParentId: String?) {
        withContext(ioDispatcher) {
            val file = cloudFileDao.getFileById(fileId) ?: error("文件不存在")
            if (targetParentId == file.fileId) {
                error("不能移动到自己")
            }
            val targetFolder = targetParentId?.let { id ->
                val folder = cloudFileDao.getFileById(id) ?: error("目标文件夹不存在")
                require(folder.type == FileType.FOLDER.name) { "目标不是文件夹" }
                folder
            }
            if (file.type == FileType.FOLDER.name && targetFolder != null) {
                require(!isDescendant(folderId = file.fileId, candidateId = targetFolder.fileId)) {
                    "不能移动到自己的子文件夹"
                }
            }

            cloudFileDao.updateParent(
                fileId = file.fileId,
                parentId = targetParentId,
                modifiedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun getMoveTargetFolders(excludeFileId: String): List<CloudFile> {
        return withContext(ioDispatcher) {
            val file = cloudFileDao.getFileById(excludeFileId) ?: error("文件不存在")
            cloudFileDao.getAllFolders()
                .filter { folder -> folder.fileId != file.fileId }
                .filter { folder ->
                    file.type != FileType.FOLDER.name ||
                        !isDescendant(folderId = file.fileId, candidateId = folder.fileId)
                }
                .map { entity -> entity.toDomain() }
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

    private suspend fun collectDescendants(root: CloudFileEntity): List<CloudFileEntity> {
        val result = mutableListOf<CloudFileEntity>()
        suspend fun visit(file: CloudFileEntity) {
            result += file
            if (file.type == FileType.FOLDER.name) {
                cloudFileDao.getFilesByParentId(file.fileId).forEach { child ->
                    visit(child)
                }
            }
        }
        visit(root)
        return result
    }

    private suspend fun isDescendant(folderId: String, candidateId: String): Boolean {
        val children = cloudFileDao.getFilesByParentId(folderId)
        return children.any { child ->
            child.fileId == candidateId ||
                (child.type == FileType.FOLDER.name && isDescendant(child.fileId, candidateId))
        }
    }

    private companion object {
        const val TAG = "FileRepository"
    }
}
