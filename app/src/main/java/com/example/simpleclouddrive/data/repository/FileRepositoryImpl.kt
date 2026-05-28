package com.example.simpleclouddrive.data.repository

import android.util.Log
import com.example.simpleclouddrive.data.local.dao.CloudFileDao
import com.example.simpleclouddrive.data.mapper.toDomain
import com.example.simpleclouddrive.data.mapper.toEntity
import com.example.simpleclouddrive.data.remote.FakeCloudApi
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.repository.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FileRepositoryImpl(
    private val cloudFileDao: CloudFileDao,
    private val fakeCloudApi: FakeCloudApi,
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

    private companion object {
        const val TAG = "FileRepository"
    }
}
