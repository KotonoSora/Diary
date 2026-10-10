package com.kotonosora.todolist.domain.model

import java.util.UUID

/**
 * Standalone mood log entry. Owned by the `mood_entries` table — never stored
 * as note frontmatter and never routed through the vault/note index.
 */
data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val emotion: EmotionStamp,
    val note: String = "",
    val linkedNoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
