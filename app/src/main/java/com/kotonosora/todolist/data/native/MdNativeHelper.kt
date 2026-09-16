package com.kotonosora.todolist.data.native

import kotlin.math.max

/**
 * Pure Kotlin helper replacing former Markdown C++ parsing & text analytics functions.
 */
object MdNativeHelper {

    private val wikiLinkRegex = Regex("\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]")
    private val tagRegex = Regex("(?:^|\\s)#([a-zA-Z_][a-zA-Z0-9_\\-]*)")
    private val titleRegex =
        Regex("^title:\\s*[\"']?([^\"'\\n\\r]+)[\"']?", RegexOption.IGNORE_CASE)

    /**
     * Extracts WikiLink target titles (e.g. [[Target Note]]) from Markdown text.
     */
    fun extractWikiLinks(mdContent: String): Array<String> {
        return wikiLinkRegex.findAll(mdContent)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            }
            .toList()
            .toTypedArray()
    }

    /**
     * Extracts tags (e.g. #tag) from Markdown text.
     */
    fun extractTags(mdContent: String): Array<String> {
        return tagRegex.findAll(mdContent)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)?.let { "#$it" }
            }
            .distinct()
            .toList()
            .toTypedArray()
    }

    /**
     * Extracts title from YAML frontmatter or first H1 header.
     */
    fun parseTitle(mdContent: String): String {
        val lines = mdContent.lines()
        var inFrontmatter = false
        var lineNum = 0

        for (line in lines) {
            lineNum++
            if ((lineNum == 1) && line.startsWith("---")) {
                inFrontmatter = true
                continue
            }
            if (inFrontmatter) {
                if (line.startsWith("---")) {
                    inFrontmatter = false
                    continue
                }
                val match = titleRegex.find(line)
                if (match != null) {
                    val title = match.groupValues.getOrNull(1)?.trim()
                    if (!title.isNullOrEmpty()) {
                        return title
                    }
                }
            } else if (line.trimStart().startsWith("# ")) {
                return line.trimStart().removePrefix("# ").trim()
            }
        }
        return ""
    }

    /**
     * Text Stats Analytics.
     * Returns IntArray(4): [wordCount, charCount, lineCount, readingTimeMinutes]
     */
    fun calculateTextStatsNative(mdContent: String): IntArray {
        if (mdContent.isEmpty()) {
            return intArrayOf(0, 0, 0, 0)
        }

        val charCount = mdContent.length
        var wordCount = 0
        var lineCount = 0
        var inWord = false

        for (c in mdContent) {
            if (c == '\n') {
                lineCount++
            }
            if (c.isWhitespace()) {
                if (inWord) {
                    wordCount++
                    inWord = false
                }
            } else {
                inWord = true
            }
        }
        if (inWord) wordCount++
        if (mdContent.isNotEmpty()) lineCount++

        val readingTimeMinutes = max(1, wordCount / 200)

        return intArrayOf(wordCount, charCount, lineCount, readingTimeMinutes)
    }
}
