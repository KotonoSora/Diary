package com.kotonosora.todolist.feature.flashcard

object MarkdownParser {

    fun parseMarkdownFlashcards(text: String): List<Flashcard> {
        if (text.isBlank()) return emptyList()

        val cards = mutableListOf<Flashcard>()

        val tableCards = parseTableFormat(text)
        if (tableCards.isNotEmpty()) {
            return tableCards
        }

        val lines = text.lines()
        val listRegex = Regex("""^(?:-|\*|\d+\.)\s+(.+)$""")

        for (line in lines) {
            val trimmedLine = line.trim()
            val match = listRegex.matchEntire(trimmedLine)
            if (match != null) {
                val content = match.groupValues[1].trim()
                val card = parseLineToFlashcard(content)
                if (card.word.isNotBlank()) {
                    cards.add(card)
                }
            }
        }

        if (cards.isEmpty()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    val card = parseLineToFlashcard(trimmed)
                    if (card.word.isNotBlank()) {
                        cards.add(card)
                    }
                }
            }
        }

        return cards
    }

    private fun parseLineToFlashcard(rawContent: String): Flashcard {
        val content = rawContent.replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
            .replace(Regex("""\*([^*]+)\*"""), "$1")

        val delimiters = listOf(" :: ", " : ", " - ", " — ", " = ", ": ")
        for (delim in delimiters) {
            if (content.contains(delim)) {
                val parts = content.split(delim, limit = 2)
                var wordPart = parts[0].trim()
                val defPart = parts.getOrNull(1)?.trim() ?: ""

                var phonetic = ""
                val phoneticSlashMatch = Regex("""^(.+?)\s*/([^/]+)/$""").find(wordPart)
                if (phoneticSlashMatch != null) {
                    wordPart = phoneticSlashMatch.groupValues[1].trim()
                    phonetic = "/" + phoneticSlashMatch.groupValues[2].trim() + "/"
                } else {
                    val phoneticParenMatch = Regex("""^(.+?)\s*\(([^)]+)\)$""").find(wordPart)
                    if (phoneticParenMatch != null) {
                        wordPart = phoneticParenMatch.groupValues[1].trim()
                        val rawPhonetic = phoneticParenMatch.groupValues[2].trim().removePrefix("/")
                            .removeSuffix("/")
                        phonetic = "/$rawPhonetic/"
                    }
                }

                var definition = defPart
                var example = ""
                val exampleMatch = Regex(
                    """^(.+?)\s*(?:\(e\.g\.|e\.g\.|Example:)\s*(.+?)\)?$""",
                    RegexOption.IGNORE_CASE
                ).find(defPart)
                if (exampleMatch != null) {
                    definition = exampleMatch.groupValues[1].trim().removeSuffix("(")
                    example = exampleMatch.groupValues[2].trim().removeSuffix(")")
                }

                return Flashcard(
                    word = wordPart,
                    definition = definition,
                    phonetic = phonetic,
                    example = example
                )
            }
        }

        return Flashcard(word = content)
    }

    private fun parseTableFormat(text: String): List<Flashcard> {
        val tableLines =
            text.lines().map { it.trim() }.filter { it.startsWith("|") && it.endsWith("|") }
        if (tableLines.size < 2) return emptyList()

        val cards = mutableListOf<Flashcard>()
        for (line in tableLines) {
            if (line.contains("---")) continue
            val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.isNotEmpty() && !cells[0].equals(
                    "Term",
                    ignoreCase = true
                ) && !cells[0].equals("Word", ignoreCase = true)
            ) {
                val word = cells[0]
                val def = cells.getOrNull(1) ?: ""
                val example = cells.getOrNull(2) ?: ""
                cards.add(Flashcard(word = word, definition = def, example = example))
            }
        }
        return cards
    }

    fun parseMarkdownList(text: String): List<String> {
        val regex = Regex("^(?:-|\\*)\\s+(.+)$", RegexOption.MULTILINE)
        return regex.findAll(text).map { it.groupValues[1].trim() }.toList()
    }
}
