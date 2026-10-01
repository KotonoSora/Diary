package com.kotonosora.todolist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MarkdownLinksTest {

    @Test
    fun `rewriteWikiLinks converts standard and aliased links`() {
        val out = MarkdownLinks.rewriteWikiLinks("See [[Roadmap]] and [[Roadmap|plan]]!")
        assertEquals(
            "See [Roadmap](wikilink://Roadmap) and [plan](wikilink://Roadmap)!",
            out
        )
    }

    @Test
    fun `rewriteWikiLinks leaves blank alias to target`() {
        val out = MarkdownLinks.rewriteWikiLinks("[[Roadmap| ]]")
        assertEquals("[Roadmap](wikilink://Roadmap)", out)
    }

    @Test
    fun `decodeWikiLinkTarget url-decodes`() {
        assertEquals("My Note", MarkdownLinks.decodeWikiLinkTarget("wikilink://My%20Note"))
        assertEquals("Plain", MarkdownLinks.decodeWikiLinkTarget("wikilink://Plain"))
    }

    @Test
    fun `resolveImageDestinations rewrites existing vault-relative images`() {
        val root = createTempDir("vault").apply {
            File(this, "_assets").mkdirs()
            File(this, "_assets/pic.jpg").writeText("img")
        }
        try {
            val out = MarkdownLinks.resolveImageDestinations(
                "![alt](_assets/pic.jpg)",
                noteDir = "",
                vaultRoot = root
            )
            assertTrue(out.startsWith("![alt](file://"))
            assertTrue(out.endsWith("/_assets/pic.jpg)"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `resolveImageDestinations prefers note folder then vault root`() {
        val root = createTempDir("vault").apply {
            File(this, "Work/_assets").mkdirs()
            File(this, "Work/_assets/a.jpg").writeText("img")
        }
        try {
            val out = MarkdownLinks.resolveImageDestinations(
                "![a](_assets/a.jpg)",
                noteDir = "Work",
                vaultRoot = root
            )
            assertTrue(out.contains("/Work/_assets/a.jpg)"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `resolveImageDestinations leaves remote absolute and missing files untouched`() {
        val root = createTempDir("vault")
        try {
            val text = "![a](https://x/y.png)\n![b](/abs/z.png)\n![c](missing.png)\n![d](#frag)"
            val out = MarkdownLinks.resolveImageDestinations(text, "Work", root)
            assertEquals(text, out)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `resolveImageDestinations blocks directory traversal outside vault`() {
        val root = createTempDir("vault")
        val outside = File(root.parentFile, "secret.jpg").apply { writeText("x") }
        try {
            val out = MarkdownLinks.resolveImageDestinations(
                "![s](../secret.jpg)",
                noteDir = "",
                vaultRoot = root
            )
            assertEquals("![s](../secret.jpg)", out)
        } finally {
            outside.delete()
            root.deleteRecursively()
        }
    }

    @Test
    fun `extractMermaidBlocks separates diagrams keeping other fences`() {
        val text = "# T\n\n```mermaid\ngraph TD\n```\n\n```kotlin\nval x = 1\n```"
        val (stripped, blocks) = MarkdownLinks.extractMermaidBlocks(text)
        assertEquals(listOf("graph TD"), blocks)
        assertTrue(stripped.contains("# T"))
        assertTrue(stripped.contains("```kotlin"))
        assertTrue(!stripped.contains("graph TD"))
    }

    @Test
    fun `extractMermaidBlocks is empty without mermaid`() {
        val text = "# T\n\n```kotlin\nval x = 1\n```"
        val (stripped, blocks) = MarkdownLinks.extractMermaidBlocks(text)
        assertTrue(blocks.isEmpty())
        assertEquals(text, stripped)
    }

    @Test
    fun `extractMermaidBlocks leaves unclosed fences inline`() {
        val text = "# T\n\n```mermaid\ngraph TD\n\nTrailing prose."
        val (stripped, blocks) = MarkdownLinks.extractMermaidBlocks(text)
        assertTrue(blocks.isEmpty())
        assertEquals(text, stripped)
    }

    private fun createTempDir(prefix: String): File {
        return File(System.getProperty("java.io.tmpdir"), "$prefix-${System.nanoTime()}").apply {
            mkdirs()
        }
    }
}
