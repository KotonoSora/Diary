package com.kotonosora.todolist.domain.model

/**
 * Builds safe FTS4 MATCH queries + LIKE escapes for vault search.
 * Extracted from VaultRepositoryImpl so it is unit-testable.
 */
object FtsQueryBuilder {
    const val MAX_TOKENS = 12
    const val MAX_TOKEN_LEN = 40

    fun escapeLike(query: String): String {
        val sb = StringBuilder(query.length)
        for (c in query) {
            if (c == '\\' || c == '%' || c == '_') sb.append('\\')
            sb.append(c)
        }
        return sb.toString()
    }

    fun toFtsMatchQuery(query: String): String? {
        val tokens = query.split("[^\\p{L}\\p{Nd}]+".toRegex())
            .map { it.trim().take(MAX_TOKEN_LEN) }
            .filter { it.isNotBlank() }
            .take(MAX_TOKENS)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it*\"" }
    }
}
