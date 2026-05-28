package com.example.simpleclouddrive.domain.model

data class CloudFile(
    val fileId: String,
    val name: String,
    val size: Long,
    val path: String,
    val parentId: String?,
    val type: FileType,
    val timestamp: Long,
    val modifiedAt: Long
)
