package com.example.simpleclouddrive.data.mapper

import com.example.simpleclouddrive.data.local.entity.CloudFileEntity
import com.example.simpleclouddrive.data.remote.dto.FileDto
import com.example.simpleclouddrive.domain.model.CloudFile
import com.example.simpleclouddrive.domain.model.FileType

fun FileDto.toEntity(): CloudFileEntity {
    return CloudFileEntity(
        fileId = fileId,
        name = name,
        size = size,
        path = path,
        parentId = parentId,
        type = type,
        timestamp = timestamp,
        modifiedAt = modifiedAt
    )
}

fun CloudFileEntity.toDomain(): CloudFile {
    return CloudFile(
        fileId = fileId,
        name = name,
        size = size,
        path = path,
        parentId = parentId,
        type = type.toFileType(),
        timestamp = timestamp,
        modifiedAt = modifiedAt
    )
}

private fun String.toFileType(): FileType {
    return runCatching { FileType.valueOf(uppercase()) }.getOrDefault(FileType.OTHER)
}
