package com.kotonosora.todolist.domain.model

/**
 * Domain model representing a Markdown or Plain Text Note file in the Vault.
 */
data class NoteItem(
    val id: String, // Relative path from Vault root (e.g., "Projects/Roadmap.md")
    val title: String,
    val relativePath: String,
    val content: String,
    val fileFormat: String = "md",
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList() // Target titles referenced by [[WikiLinks]]
)
