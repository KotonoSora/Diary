package com.kotonosora.todolist.domain.model

sealed class VaultNode {
    abstract val name: String
    abstract val relativePath: String

    data class FileNode(
        override val name: String,
        override val relativePath: String,
        val extension: String,
        val sizeBytes: Long,
        val updatedAt: Long
    ) : VaultNode()

    data class FolderNode(
        override val name: String,
        override val relativePath: String,
        val children: List<VaultNode> = emptyList(),
        val isExpanded: Boolean = false
    ) : VaultNode()
}
