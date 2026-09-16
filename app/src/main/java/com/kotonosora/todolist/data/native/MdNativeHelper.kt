package com.kotonosora.todolist.data.native

/**
 * Kotlin JNI wrapper for Markdown C++ parsing & text analytics functions.
 */
object MdNativeHelper {

    /**
     * Extracts WikiLink target titles (e.g. [[Target Note]]) from Markdown text using C++.
     */
    external fun extractWikiLinks(mdContent: String): Array<String>

    /**
     * Extracts tags (e.g. #tag) from Markdown text using C++.
     */
    external fun extractTags(mdContent: String): Array<String>

    /**
     * Extracts title from YAML frontmatter or first H1 header using C++.
     */
    external fun parseTitle(mdContent: String): String

    /**
     * High-performance Native C++ Text Stats Analytics.
     * Returns IntArray(4): [wordCount, charCount, lineCount, readingTimeMinutes]
     */
    external fun calculateTextStatsNative(mdContent: String): IntArray
}
