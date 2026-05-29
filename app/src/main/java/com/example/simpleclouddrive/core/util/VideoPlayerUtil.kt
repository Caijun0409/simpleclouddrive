package com.example.simpleclouddrive.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.simpleclouddrive.domain.model.CloudFile
import java.io.File

object VideoPlayerUtil {
    fun openVideo(
        context: Context,
        file: CloudFile
    ): Result<Unit> {
        val diskFile = File(file.path)
        if (!diskFile.exists() || !diskFile.isFile) {
            return Result.failure(IllegalStateException("文件不存在：${file.name}"))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            diskFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, VIDEO_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return runCatching {
            context.startActivity(intent)
        }.recoverCatching { throwable ->
            if (throwable is ActivityNotFoundException) {
                error("未找到可打开该视频的应用")
            } else {
                throw throwable
            }
        }
    }

    private const val VIDEO_MIME_TYPE = "video/*"
}
