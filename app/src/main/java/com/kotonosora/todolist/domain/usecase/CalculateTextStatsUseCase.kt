package com.kotonosora.todolist.domain.usecase

import com.kotonosora.todolist.data.native.MdNativeHelper

data class TextStats(
    val wordCount: Int,
    val charCount: Int,
    val lineCount: Int,
    val readingTimeMinutes: Int
)

class CalculateTextStatsUseCase {
    operator fun invoke(content: String): TextStats {
        return try {
            val statsArray = MdNativeHelper.calculateTextStatsNative(content)
            TextStats(
                wordCount = statsArray.getOrElse(0) { 0 },
                charCount = statsArray.getOrElse(1) { 0 },
                lineCount = statsArray.getOrElse(2) { 0 },
                readingTimeMinutes = statsArray.getOrElse(3) { 1 }
            )
        } catch (_: Throwable) {
            val words = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
            val chars = content.length
            val lines = if (content.isBlank()) 0 else content.lines().size
            val readingTime = (words / 200).coerceAtLeast(1)
            TextStats(words, chars, lines, readingTime)
        }
    }
}
