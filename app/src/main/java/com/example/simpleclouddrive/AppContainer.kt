package com.example.simpleclouddrive

import android.content.Context
import androidx.room.Room
import com.example.simpleclouddrive.data.local.AppDatabase
import com.example.simpleclouddrive.data.local.FileStorageManager
import com.example.simpleclouddrive.data.remote.FakeCloudApi
import com.example.simpleclouddrive.data.repository.FileRepositoryImpl
import com.example.simpleclouddrive.domain.repository.FileRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        context = appContext,
        klass = AppDatabase::class.java,
        name = "simple_cloud_drive.db"
    ).build()

    private val fakeCloudApi: FakeCloudApi = FakeCloudApi(appContext)
    private val fileStorageManager: FileStorageManager = FileStorageManager(appContext)

    val fileRepository: FileRepository = FileRepositoryImpl(
        cloudFileDao = database.cloudFileDao(),
        recentBrowseDao = database.recentBrowseDao(),
        recentTransferDao = database.recentTransferDao(),
        fakeCloudApi = fakeCloudApi,
        fileStorageManager = fileStorageManager
    )
}
