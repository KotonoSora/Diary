package com.kotonosora.todolist.ui.components

import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Pure Markdown link preprocessing for the reading views (unit-testable).
 * - WikiLinks `[[Target]]` / `[[Target|Alias]]` become standard links the
 *   mikepenz renderer turns into clickable annotations.
 * - Relative image destinations (`_assets/pic.jpg`) resolve to `file://` URLs
 *   against the note's folder so Coil can load vault-local media. Absolute,
 *   `file://`, `content://` and `http(s)` destinations pass through untouched.
 * - `mermaid` fences are extracted for [MermaidDiagramView]; everything else
 *   stays inline for mikepenz (which now highlights code natively).
 */
object MarkdownLinks {

    const val WIKILINK_SCHEME = "wikilink://"

    private val wikiLinkRegex = Regex("""\[\[([^|\]]+)(?:\|([^\]]+))?\]\]""")
    private val imageRegex = Regex("""!\[([^\]]*)]\(\s*(\S+?)(\s+"[^"]*")?\s*\)""")
    private val fenceRegex = Regex(
        "^```(\\w*)\\s*\n([\\s\\S]*?)^```",
        setOf(RegexOption.MULTILINE)
    )

    fun rewriteWikiLinks(text: String): String {
        return text.replace(wikiLinkRegex) { match ->
            val target = match.groupValues[1].trim()
            val alias = match.groupValues.getOrNull(2)?.trim()?.ifBlank { null } ?: target
            val encoded = try {
                URLEncoder.encode(target, Charsets.UTF_8.name()).replace("+", "%20")
            } catch (e: Exception) {
                target
            }
            "[$alias]($WIKILINK_SCHEME$encoded)"
        }
    }

    fun decodeWikiLinkTarget(uri: String): String {
        val raw = uri.removePrefix(WIKILINK_SCHEME)
        return try {
            URLDecoder.decode(raw, Charsets.UTF_8.name())
        } catch (e: Exception) {
            raw
        }
    }

    fun resolveImageDestinations(
        text: String,
        noteDir: String,
        vaultRoot: File
    ): String {
        // Blank noteDir = vault root itself. Falls back to vault root for
        // shared attachment folders (e.g. root `_assets/`).
        return text.replace(imageRegex) { match ->
            val alt = match.groupValues[1]
            val dest = match.groupValues[2]
            val title = match.groupValues.getOrNull(3).orEmpty()
            if (isResolvableRelative(dest)) {
                val root = vaultRoot.canonicalFile
                val file = sequenceOf(
                    File(File(vaultRoot, noteDir), dest),
                    File(vaultRoot, dest)
                ).map { it.normalize() }.firstOrNull { candidate ->
                    // Contain traversal (`../`) inside the vault.
                    candidate.canonicalPath.startsWith(root.path + File.separator) &&
                            candidate.exists()
                }
                if (file != null) {
                    return@replace "![$alt](${fileToEncodedUri(file)}$title)"
                }
            }
            match.value
        }
    }

    private fun isResolvableRelative(dest: String): Boolean {
        if (dest.isBlank()) return false
        val lower = dest.lowercase()
        return !lower.startsWith("http://") &&
                !lower.startsWith("https://") &&
                !lower.startsWith("file://") &&
                !lower.startsWith("content://") &&
                !lower.startsWith("data:") &&
                !dest.startsWith("#") &&
                !dest.startsWith("/")
    }

    /**
     * Encodes an absolute file path as a `file://` URI (`file:///tmp/my%20pic.jpg`).
     * Segments are encoded individually so `/` separators survive while spaces
     * and `#`/`?`/`%` don't break Coil.
     */
    internal fun fileToEncodedUri(file: File): String {
        val encoded =
            file.absolutePath.split(File.separator).joinToString(File.separator) { segment ->
                try {
                    URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
                } catch (e: Exception) {
                    segment
                }
            }
        return "file://$encoded"
    }

    /**
     * Returns `(strippedText, mermaidBlocks)`: fenced `mermaid` blocks are cut
     * out of the prose (diagrams render separately) while all other fences stay
     * inline for native highlighted rendering.
     */
    fun extractMermaidBlocks(text: String): Pair<String, List<String>> {
        val blocks = mutableListOf<String>()
        val stripped = fenceRegex.replace(text) { match ->
            val language = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (language.equals("mermaid", ignoreCase = true)) {
                blocks.add(match.groupValues.getOrNull(2)?.trimEnd().orEmpty())
                ""
            } else {
                match.value
            }
        }
        return stripped to blocks
    }
}
