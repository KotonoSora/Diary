package com.kotonosora.todolist.domain.model

/**
 * Parses GFM task list items (`- [ ]`, `- [x]`, `* [ ]`, `* [x]`) for the
 * reading view. Previously toggle logic lived only in LivePreviewEditor and
 * was unreachable because MarkdownView never forwarded clicks.
 */
data class TaskListEntry(
    val lineIndex: Int,
    val checked: Boolean,
    val text: String
)

object TaskToggleHelper {
    private val taskRegex = Regex("""^(\s*[-*]\s*\[)([ xX])(\]\s*)(.*)$""")

    /**
     * Parses tasks, skipping lines inside fenced code blocks (``` / ~~~,
     * including ```mermaid diagrams) so checklist rows never point into a
     * fence. Indices stay relative to [content], matching [toggleTaskAtLine].
     */
    fun parseTasks(content: String): List<TaskListEntry> {
        val entries = mutableListOf<TaskListEntry>()
        var inFence = false
        content.lines().forEachIndexed { index, line ->
            if (isFenceMarker(line)) {
                inFence = !inFence
                return@forEachIndexed
            }
            if (inFence) return@forEachIndexed
            val m = taskRegex.find(line) ?: return@forEachIndexed
            entries.add(
                TaskListEntry(
                    lineIndex = index,
                    checked = m.groupValues[2].lowercase() == "x",
                    text = m.groupValues[4].ifBlank { "(empty task)" }
                )
            )
        }
        return entries
    }

    private fun isFenceMarker(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("```") || trimmed.startsWith("~~~")
    }

    fun toggleTaskAtLine(text: String, lineIndex: Int): String {
        // Preserve the file's newline style so \r\n vaults aren't normalized.
        val delimiter = if (text.contains("\r\n")) "\r\n" else "\n"
        // Split keeping trailing empty segment so a trailing newline survives the join.
        val lines = text.split(delimiter).toMutableList()
        if (lineIndex !in lines.indices) return text
        val line = lines[lineIndex]
        // Single regex path: anchored, so at most one replacement per line.
        // Handles `- [ ]`, `* [ ]` (any case) with or without trailing text —
        // the old contains()+replace() chain required a trailing space and
        // replaced every occurrence on the line.
        lines[lineIndex] = taskRegex.replace(line) { mr ->
            val open = mr.groupValues[1]
            val mark = mr.groupValues[2]
            val close = mr.groupValues[3]
            val rest = mr.groupValues[4]
            val flipped = if (mark.lowercase() == "x") " " else "x"
            "$open$flipped$close$rest"
        }
        return lines.joinToString(delimiter)
    }
}
