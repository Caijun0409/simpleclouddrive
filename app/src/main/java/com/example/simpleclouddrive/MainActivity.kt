package com.example.simpleclouddrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.simpleclouddrive.core.ui.theme.SimpleCloudDriveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as SimpleCloudDriveApplication).appContainer
        setContent {
            SimpleCloudDriveTheme {
                SimpleCloudApp(
                    fileRepository = appContainer.fileRepository
                )
            }
        }
    }
}
