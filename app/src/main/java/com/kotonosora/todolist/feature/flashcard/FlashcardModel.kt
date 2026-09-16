package com.kotonosora.todolist.feature.flashcard

import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val definition: String = "",
    val phonetic: String = "",
    val example: String = "",
    val isMastered: Boolean = false
)
