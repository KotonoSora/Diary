package com.kotonosora.todolist.domain.model

import com.kotonosora.todolist.data.native.MdNativeHelper
import java.util.Locale

/**
 * Single owner for mood-stamp parsing (frontmatter `emotion:` / `actions:`).
 * Previously duplicated in VaultRepositoryImpl.entityToDomain + EditorScreen.
 */
object StampParser {
    fun parseEmotion(content: String): EmotionStamp? {
        return try {
            MdNativeHelper.parseEmotionName(content)
                ?.let { EmotionStamp.valueOf(it.uppercase(Locale.ROOT)) }
        } catch (_: Exception) {
            null
        }
    }

    fun parseActions(content: String): List<ActionStamp> {
        return try {
            MdNativeHelper.parseActionNames(content).mapNotNull {
                try {
                    ActionStamp.valueOf(it.uppercase(Locale.ROOT))
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val emotionLineRegex = Regex("^\\s*emotion\\s*:.*$", RegexOption.IGNORE_CASE)
    private val actionsLineRegex = Regex("^\\s*actions\\s*:.*$", RegexOption.IGNORE_CASE)

    private fun isStampLine(line: String): Boolean =
        emotionLineRegex.matches(line) || actionsLineRegex.matches(line)

    /**
     * Returns [content] with its frontmatter `emotion:` / `actions:` stamp
     * lines replaced by the given stamps (removed when null/empty). The
     * parser is order-insensitive, so stamps are appended just before the
     * closing `---`; documents without frontmatter get a new block prepended.
     * Format matches what [MdNativeHelper] parses (`emotion: NAME`,
     * `actions: [A, B]`).
     */
    fun withStamps(
        content: String,
        emotion: EmotionStamp?,
        actions: List<ActionStamp>
    ): String {
        val stampLines = buildList {
            if (emotion != null) add("emotion: ${emotion.name}")
            if (actions.isNotEmpty()) add("actions: [${actions.joinToString(", ") { it.name }}]")
        }
        val lines = content.lines()
        if (lines.firstOrNull()?.trim() != "---") {
            if (stampLines.isEmpty()) return content
            return (listOf("---") + stampLines + listOf("---", "") + lines).joinToString("\n")
        }
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end == -1) {
            if (stampLines.isEmpty()) return content
            return (listOf("---") + stampLines + listOf("---", "") + lines).joinToString("\n")
        }
        val closeIndex = end + 1
        val body = lines.drop(1).take(end).filterNot(::isStampLine).toMutableList()
        body.addAll(stampLines)
        return (listOf(lines[0]) + body + listOf(lines[closeIndex]) + lines.drop(closeIndex + 1))
            .joinToString("\n")
    }
}
