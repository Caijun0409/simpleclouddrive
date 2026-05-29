package com.example.simpleclouddrive.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileStorageManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun copyUriToPrivateStorage(
        uri: Uri,
        fileId: String,
        displayName: String
    ): String = withContext(ioDispatcher) {
        val cloudDir = File(context.filesDir, CLOUD_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val targetFile = File(cloudDir, "${fileId}_${displayName.sanitizeFileName()}")
        context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "无法读取所选文件" }
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        targetFile.absolutePath
    }

    suspend fun deletePrivateFile(path: String): Boolean = withContext(ioDispatcher) {
        val targetFile = File(path)
        targetFile.exists() && targetFile.isFile && targetFile.delete()
    }

    fun isPrivateCloudFile(path: String): Boolean {
        val cloudDir = File(context.filesDir, CLOUD_DIR_NAME)
        val targetFile = File(path)
        return runCatching {
            targetFile.canonicalPath.startsWith(cloudDir.canonicalPath + File.separator)
        }.getOrDefault(false)
    }

    suspend fun getFileSize(uri: Uri): Long = withContext(ioDispatcher) {
        queryOpenable(uri, OpenableColumns.SIZE)?.toLongOrNull()
            ?: context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length
            }
            ?: 0L
    }

    suspend fun getDisplayName(uri: Uri): String = withContext(ioDispatcher) {
        queryOpenable(uri, OpenableColumns.DISPLAY_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: "未命名文件"
    }

    fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    private fun queryOpenable(uri: Uri, columnName: String): String? {
        val projection = arrayOf(columnName)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(columnName)
            if (columnIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(columnIndex)
            } else {
                null
            }
        }
    }

    private fun String.sanitizeFileName(): String {
        return replace(Regex("""[\\/:*?"<>|]"""), "_")
    }

    private companion object {
        const val CLOUD_DIR_NAME = "cloud"
    }
}
