package com.example.simpleclouddrive.data.remote

import android.content.Context
import com.example.simpleclouddrive.data.remote.dto.FileDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class FakeCloudApi(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun fetchFiles(): List<FileDto> = withContext(ioDispatcher) {
        delay(500L)
        val content = context.assets.open(MOCK_FILES_ASSET).bufferedReader().use { reader ->
            reader.readText()
        }
        json.decodeFromString<List<FileDto>>(content)
    }

    private companion object {
        const val MOCK_FILES_ASSET = "mock_files.json"
    }
}
