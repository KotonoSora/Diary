package com.kotonosora.todolist.ui.theme

import androidx.compose.ui.graphics.Color
import com.kotonosora.todolist.domain.model.NoteType

/**
 * Single source of truth for per-[NoteType] graph/dot colors.
 * Previously hardcoded in EditorMetadataBar + KnowledgeGraphScreen.
 */
object NoteTypeColors {
    fun forNoteType(noteType: NoteType): Color = when (noteType) {
        NoteType.FLEETING -> Color(0xFFFFC107)
        NoteType.LITERATURE -> Color(0xFF2196F3)
        NoteType.PERMANENT -> Color(0xFF4CAF50)
        NoteType.MOC -> Color(0xFF9C27B0)
        NoteType.DIARY -> Color(0xFFE91E63)
        NoteType.DAILY -> Color(0xFF00BCD4)
        NoteType.REPORT -> Color(0xFFFF5722)
        NoteType.TODO -> Color(0xFF8BC34A)
        NoteType.FLASHCARD -> Color(0xFF673AB7)
    }

    fun labelFor(noteType: NoteType): String = when (noteType) {
        NoteType.FLEETING -> "Quick Note"
        NoteType.LITERATURE -> "Reference Note"
        NoteType.PERMANENT -> "Core Note"
        NoteType.MOC -> "Topic Index"
        NoteType.DIARY -> "Diary"
        NoteType.DAILY -> "Daily Note"
        NoteType.REPORT -> "Report"
        NoteType.TODO -> "Task Note"
        NoteType.FLASHCARD -> "Flashcard"
    }
}
