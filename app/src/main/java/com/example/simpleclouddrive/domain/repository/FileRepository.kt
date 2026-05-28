package com.example.simpleclouddrive.domain.repository

import com.example.simpleclouddrive.domain.model.CloudFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    suspend fun initializeIfNeeded()

    fun observeFiles(parentId: String?): Flow<List<CloudFile>>

    suspend fun getFileById(fileId: String): CloudFile?
}
