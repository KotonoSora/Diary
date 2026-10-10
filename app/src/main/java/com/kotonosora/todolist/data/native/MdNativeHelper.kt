package com.kotonosora.todolist.data.native

import kotlin.math.max

/**
 * Pure Kotlin helper for Markdown parsing & text analytics.
 * (Historically named "*Native" when backed by C++; now 100% Kotlin.)
 */
object MdNativeHelper {

    private val wikiLinkRegex = Regex("\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]")
    private val tagRegex = Regex("(?:^|\\s)#([a-zA-Z_][a-zA-Z0-9_\\-]*)")
    private val titleRegex =
        Regex("^title:\\s*[\"']?([^\"'\\n\\r]+)[\"']?", RegexOption.IGNORE_CASE)
    private val emotionRegex =
        Regex(
            "^\\s*emotion:\\s*[\"']?(\\w+)[\"']?",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        )
    private val actionsRegex =
        Regex(
            "^\\s*actions:\\s*\\[([^\\]\n]*)]",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        )
    private val frontmatterTagsRegex =
        Regex("^\\s*tags\\s*:\\s*(.+)$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
    private val tagNameRegex = Regex("^[A-Za-z_][A-Za-z0-9_\\-]*$")

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
     * Returns the YAML frontmatter block (lines between the leading `---` markers),
     * or empty string when the document has none. Stamp keys are only read from
     * here so body text like "emotion: happy" is never mistaken for metadata.
     */
    private fun frontmatterBlock(mdContent: String): String {
        val lines = mdContent.lines()
        if (lines.firstOrNull()?.trim() != "---") return ""
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end == -1) return ""
        return lines.drop(1).take(end).joinToString("\n")
    }

    /**
     * Reads the frontmatter `emotion:` stamp name (e.g. `emotion: HAPPY`).
     * Returns null when absent. Callers map the name to EmotionStamp.
     */
    fun parseEmotionName(mdContent: String): String? {
        return emotionRegex.find(frontmatterBlock(mdContent))
            ?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Reads the frontmatter `actions:` stamp names (e.g. `actions: [WORK, EXERCISE]`).
     * The list must sit on one line; wrapped lists only parse the first line.
     * Returns empty list when absent. Callers map names to ActionStamp.
     */
    fun parseActionNames(mdContent: String): List<String> {
        val raw = actionsRegex.find(frontmatterBlock(mdContent))
            ?.groupValues?.getOrNull(1) ?: return emptyList()
        return raw.split(",")
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'").trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Reads the frontmatter `tags:` list (e.g. `tags: [diary, journal]` or
     * `tags: diary`). Names are normalized to the `#tag` form used by
     * [extractTags] so body tags and frontmatter tags merge cleanly.
     * Returns empty list when absent. Body `#tags` are never included here.
     */
    fun parseFrontmatterTags(mdContent: String): List<String> {
        val raw = frontmatterTagsRegex.find(frontmatterBlock(mdContent))
            ?.groupValues?.getOrNull(1)?.trim() ?: return emptyList()
        val inner = if (raw.startsWith("[") && raw.endsWith("]")) {
            raw.substring(1, raw.length - 1)
        } else {
            raw
        }
        return inner.split(",")
            .map {
                it.trim().removeSurrounding("\"").removeSurrounding("'")
                    .trim().removePrefix("#").trim()
            }
            .filter { it.matches(tagNameRegex) }
            .map { "#$it" }
            .distinct()
    }

    /**
     * Text Stats Analytics.
     * Returns IntArray(4): [wordCount, charCount, lineCount, readingTimeMinutes]
     */
    fun calculateTextStats(mdContent: String): IntArray {
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
