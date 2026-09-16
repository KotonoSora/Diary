package com.kotonosora.todolist.feature.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parseMarkdownFlashcards_bulletPointsWithDelimiter_parsesWordAndDefinition() {
        val markdown = """
            - Serendipity: Finding valuable things by chance
            - Ephemeral - Lasting for a short time
            - Ubiquitous :: Present everywhere
        """.trimIndent()

        val cards = MarkdownParser.parseMarkdownFlashcards(markdown)

        assertEquals(3, cards.size)
        assertEquals("Serendipity", cards[0].word)
        assertEquals("Finding valuable things by chance", cards[0].definition)

        assertEquals("Ephemeral", cards[1].word)
        assertEquals("Lasting for a short time", cards[1].definition)

        assertEquals("Ubiquitous", cards[2].word)
        assertEquals("Present everywhere", cards[2].definition)
    }

    @Test
    fun parseMarkdownFlashcards_markdownTable_parsesTableRows() {
        val tableMarkdown = """
            | Term | Definition | Example |
            | --- | --- | --- |
            | Algorithm | Set of rules to solve a problem | Sorting numbers |
            | Encryption | Coding data for security | SSL TLS |
        """.trimIndent()

        val cards = MarkdownParser.parseMarkdownFlashcards(tableMarkdown)

        assertEquals(2, cards.size)
        assertEquals("Algorithm", cards[0].word)
        assertEquals("Set of rules to solve a problem", cards[0].definition)
        assertEquals("Sorting numbers", cards[0].example)

        assertEquals("Encryption", cards[1].word)
        assertEquals("Coding data for security", cards[1].definition)
    }

    @Test
    fun parseMarkdownFlashcards_phoneticInParentheses_extractsPhonetic() {
        val markdown = "- Student (/ˈstjuː.dənt/): A person studying at school"

        val cards = MarkdownParser.parseMarkdownFlashcards(markdown)

        assertEquals(1, cards.size)
        assertEquals("Student", cards[0].word)
        assertEquals("/ˈstjuː.dənt/", cards[0].phonetic)
        assertEquals("A person studying at school", cards[0].definition)
    }

    @Test
    fun parseMarkdownFlashcards_emptyString_returnsEmptyList() {
        val cards = MarkdownParser.parseMarkdownFlashcards("")
        assertTrue(cards.isEmpty())
    }
}
