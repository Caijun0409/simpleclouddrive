package com.example.simpleclouddrive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.simpleclouddrive.core.util.DeepLinkUtil
import com.example.simpleclouddrive.core.ui.theme.SimpleCloudDriveTheme

class MainActivity : ComponentActivity() {
    private var pendingShareId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingShareId = DeepLinkUtil.parseShareId(intent?.data)
        val appContainer = (application as SimpleCloudDriveApplication).appContainer
        setContent {
            SimpleCloudDriveTheme {
                SimpleCloudApp(
                    fileRepository = appContainer.fileRepository,
                    pendingShareId = pendingShareId,
                    onPendingShareConsumed = { pendingShareId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShareId = DeepLinkUtil.parseShareId(intent.data)
    }
}
