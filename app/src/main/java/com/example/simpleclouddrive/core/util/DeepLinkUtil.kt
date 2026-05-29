package com.example.simpleclouddrive.core.util

import android.net.Uri

object DeepLinkUtil {
    fun buildShareLink(shareId: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_SHARE)
            .appendQueryParameter(QUERY_SHARE_ID, shareId)
            .build()
            .toString()
    }

    fun parseShareId(uri: Uri?): String? {
        if (uri == null) {
            return null
        }
        return uri
            .takeIf { it.scheme == SCHEME && it.host == HOST_SHARE }
            ?.getQueryParameter(QUERY_SHARE_ID)
            ?.takeIf { shareId -> shareId.isNotBlank() }
    }

    private const val SCHEME = "simplecloud"
    private const val HOST_SHARE = "share"
    private const val QUERY_SHARE_ID = "shareId"
}
