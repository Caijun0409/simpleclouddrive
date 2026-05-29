package com.example.simpleclouddrive.feature.reader

import kotlin.math.floor
import kotlin.math.max

object TxtPaginator {
    fun paginate(
        text: String,
        widthPx: Int,
        heightPx: Int,
        fontSizePx: Float,
        lineHeightPx: Float
    ): List<String> {
        val normalizedText = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        if (normalizedText.isEmpty()) {
            return emptyList()
        }

        val charsPerLine = max(
            MIN_CHARS_PER_LINE,
            floor(widthPx / (fontSizePx * AVERAGE_CHAR_WIDTH_RATIO)).toInt()
        )
        val linesPerPage = max(
            MIN_LINES_PER_PAGE,
            floor(heightPx / lineHeightPx).toInt()
        )
        val charsPerPage = max(1, charsPerLine * linesPerPage)

        val pages = mutableListOf<String>()
        var start = 0
        while (start < normalizedText.length) {
            val maxEnd = (start + charsPerPage).coerceAtMost(normalizedText.length)
            val end = findPageEnd(
                text = normalizedText,
                start = start,
                maxEnd = maxEnd,
                minBreakIndex = start + (charsPerPage * MIN_BREAK_RATIO).toInt()
            )
            pages += normalizedText.substring(start, end).trim('\n')
            start = end
            while (start < normalizedText.length && normalizedText[start] == '\n') {
                start++
            }
        }
        return pages
    }

    private fun findPageEnd(
        text: String,
        start: Int,
        maxEnd: Int,
        minBreakIndex: Int
    ): Int {
        if (maxEnd >= text.length) {
            return text.length
        }

        val newlineIndex = text.lastIndexOf('\n', startIndex = maxEnd - 1)
        return if (newlineIndex >= minBreakIndex && newlineIndex > start) {
            newlineIndex
        } else {
            maxEnd
        }
    }

    private const val AVERAGE_CHAR_WIDTH_RATIO = 0.56f
    private const val MIN_BREAK_RATIO = 0.55f
    private const val MIN_CHARS_PER_LINE = 8
    private const val MIN_LINES_PER_PAGE = 4
}
