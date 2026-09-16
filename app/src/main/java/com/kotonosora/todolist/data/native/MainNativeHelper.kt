package com.kotonosora.todolist.data.native

/**
 * Pure Kotlin helper replacing former native C++ performance helpers.
 */
object MainNativeHelper {

    /**
     * Sorts an array of strings alphabetically.
     */
    fun sortStrings(arr: Array<String>): Array<String> {
        return arr.sortedArray()
    }

    /**
     * Parses the title (first "# " heading) from a Markdown string.
     */
    fun parseMdTitle(mdContent: String): String {
        return mdContent.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            .orEmpty()
    }

    /**
     * Filters an array of strings by a case-insensitive prefix.
     */
    fun filterByPrefix(arr: Array<String>, prefix: String): Array<String> {
        if (prefix.isEmpty()) return arr
        return arr.filter { it.startsWith(prefix, ignoreCase = true) }.toTypedArray()
    }
}
