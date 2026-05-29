package com.example.simpleclouddrive.data.repository

import android.net.Uri
import android.util.Log
import com.example.simpleclouddrive.data.local.FileStorageManager
import com.example.simpleclouddrive.data.local.dao.CloudFileDao
import com.example.simpleclouddrive.data.local.dao.RecentBrowseDao
import com.example.simpleclouddrive.data.local.dao.RecentTransferDao
import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.local.entity.RecentBrowseEntity
import com.example.simpleclouddrive.data.local.entity.RecentTransferEntity
import com.example.simpleclouddrive.data.mapper.toDomain
import com.example.simpleclouddrive.data.mapper.toEntity
import com.example.simpleclouddrive.data.remote.FakeCloudApi
import com.example.simpleclouddrive.data.remote.dto.FileDto
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType
import com.example.simpleclouddrive.domain.repository.FileRepository
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
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
            val mockFiles = fakeCloudApi.fetchFiles()
            if (fileCount > 0) {
                ensureMockTextFiles(mockFiles)
                Log.d(TAG, "Mock data initialization skipped, cloud_file count=$fileCount")
                return@withContext
            }

            val files = mockFiles.map { dto -> dto.toInitialEntity() }
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

    override suspend fun readTextFile(fileId: String): String {
        return withContext(ioDispatcher) {
            val file = cloudFileDao.getFileById(fileId) ?: error("文件不存在")
            require(file.type == FileType.TXT.name) { "不是 TXT 文件" }
            val diskFile = File(file.path)
            if (!diskFile.exists() || !diskFile.isFile) {
                error("文件不存在：${file.name}")
            }

            recentBrowseDao.upsert(
                RecentBrowseEntity(
                    fileId = file.fileId,
                    browseTime = System.currentTimeMillis()
                )
            )

            val bytes = diskFile.readBytes()
            decodeText(bytes)
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

    private suspend fun ensureMockTextFiles(mockFiles: List<FileDto>) {
        mockFiles
            .filter { dto -> dto.type == FileType.TXT.name }
            .forEach { dto ->
                val existing = cloudFileDao.getFileById(dto.fileId) ?: return@forEach
                val diskFile = File(existing.path)
                if (diskFile.exists() && diskFile.isFile) {
                    return@forEach
                }

                val realPath = fileStorageManager.writeTextToPrivateStorage(
                    fileId = existing.fileId,
                    displayName = existing.name,
                    content = buildMockTextContent(existing.name)
                )
                cloudFileDao.insert(
                    existing.copy(
                        path = realPath,
                        size = File(realPath).length(),
                        modifiedAt = System.currentTimeMillis()
                    )
                )
            }
    }

    private suspend fun FileDto.toInitialEntity(): CloudFileEntity {
        if (type != FileType.TXT.name) {
            return toEntity()
        }

        val realPath = fileStorageManager.writeTextToPrivateStorage(
            fileId = fileId,
            displayName = name,
            content = buildMockTextContent(name)
        )
        return toEntity().copy(
            path = realPath,
            size = File(realPath).length()
        )
    }

    private fun buildMockTextContent(fileName: String): String {
        return buildString {
            appendLine("这是 $fileName 的测试文档。")
            appendLine("这份内容由 SimpleCloudDrive 在初始化 mock 数据时写入 App 私有目录。")
            appendLine("它用于验证 TXT 阅读器可以读取真实文件、执行分页，并支持左右滑动翻页。")
            appendLine()
            repeat(36) { index ->
                appendLine(
                    "第 ${index + 1} 段：云盘里的文档可能很短，也可能很长。阅读器需要根据屏幕尺寸、字体大小和行高估算每页内容，让文本在不同设备上都能稳定展示。"
                )
            }
        }
    }

    private fun decodeText(bytes: ByteArray): String {
        return runCatching {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        }.getOrElse {
            bytes.toString(Charset.forName("GBK"))
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
