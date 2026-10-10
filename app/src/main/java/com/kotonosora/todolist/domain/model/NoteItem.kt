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
    val links: List<String> = emptyList(), // Target titles referenced by [[WikiLinks]]
    val emotion: EmotionStamp? = null,
    val actions: List<ActionStamp> = emptyList()
)

/**
 * Builds the vault-relative id a note gets after being renamed to [newTitle].
 * Single source of truth for the rename filename policy (previously
 * duplicated in the repository and the editor ViewModel, which could drift).
 */
fun NoteItem.renamedId(newTitle: String): String {
    val newFilename = if (id.contains("-")) {
        val prefix = id.substringAfterLast("/").substringBefore("-")
        "$prefix-$newTitle.$fileFormat"
    } else {
        "$newTitle.$fileFormat"
    }
    return if (relativePath.isBlank()) newFilename else "$relativePath/$newFilename"
}
