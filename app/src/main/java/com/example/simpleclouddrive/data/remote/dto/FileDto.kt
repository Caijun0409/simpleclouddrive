package com.example.simpleclouddrive.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FileDto(
    val fileId: String,
    val name: String,
    val size: Long,
    val path: String,
    val parentId: String? = null,
    val type: String,
    val timestamp: Long,
    val modifiedAt: Long
)
