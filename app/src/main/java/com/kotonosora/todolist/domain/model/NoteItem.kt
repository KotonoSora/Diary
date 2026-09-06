package com.kotonosora.todolist.domain.model

/**
 * Domain model representing a Markdown or Plain Text Note file in the Vault.
 */
data class NoteItem(
    val id: String, // Relative path from Vault root (e.g., "Projects/202603011200-Roadmap.md")
    val title: String,
    val relativePath: String,
    val content: String,
    val uid: String = ZettelUidGenerator.generateUid(),
    val noteType: NoteType = NoteType.PERMANENT,
    val fileFormat: String = "md",
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val author: String? = null,
    val sourceUrl: String? = null,
    val paraCategory: ParaCategory? = null,
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList() // Target titles referenced by [[WikiLinks]]
)
