package com.kotonosora.todolist.domain.model

import com.kotonosora.todolist.common.AppConstants
import java.text.SimpleDateFormat
import java.util.Date

enum class NoteType {
    FLEETING,    // Ephemeral scratchpad thoughts / voice notes
    LITERATURE,  // Reading notes, highlights, quotes with citations
    PERMANENT,   // Atomic, self-contained thoughts (1 Idea Rule)
    MOC,         // Map of Content (Structural Index Note)
    DIARY,       // Personal diary / journal entries
    DAILY,       // Daily planner / log note
    REPORT,      // Work / Meeting report
    TODO,        // Task & Todo list note
    FLASHCARD    // Flashcard vocabulary deck note
}

val NoteType.displayName: String
    get() = when (this) {
        NoteType.FLEETING -> "Quick Note"
        NoteType.LITERATURE -> "Reference"
        NoteType.PERMANENT -> "Core Note"
        NoteType.MOC -> "Topic Index"
        NoteType.DIARY -> "Diary Journal"
        NoteType.DAILY -> "Daily Note"
        NoteType.REPORT -> "Work Report"
        NoteType.TODO -> "Todo List"
        NoteType.FLASHCARD -> "Flashcard Deck"
    }

val NoteType.description: String
    get() = when (this) {
        NoteType.FLEETING -> "Quick thoughts, scratchpad, and fleeting ideas"
        NoteType.LITERATURE -> "Reading notes, citations, and key quotes"
        NoteType.PERMANENT -> "Atomic concepts following 1-Idea rule"
        NoteType.MOC -> "Map of Content structural topic hub"
        NoteType.DIARY -> "Personal reflections, mood, and daily journal"
        NoteType.DAILY -> "Daily schedule, priority tasks, and quick logs"
        NoteType.REPORT -> "Meeting minutes, status reports, and key takeaways"
        NoteType.TODO -> "Structured task list with checkboxes and backlog"
        NoteType.FLASHCARD -> "Vocabulary deck with terms, phonetics & definitions"
    }

val NoteType.defaultTitlePrefix: String
    get() {
        val today = SimpleDateFormat("yyyy-MM-dd", AppConstants.APP_LOCALE).format(Date())
        return when (this) {
            NoteType.DIARY -> "$today Diary"
            NoteType.DAILY -> "$today Daily Note"
            NoteType.REPORT -> "Meeting Report"
            NoteType.TODO -> "Task List"
            NoteType.FLASHCARD -> "Vocabulary Deck"
            NoteType.FLEETING -> "Quick Note"
            NoteType.LITERATURE -> "Reference Note"
            NoteType.PERMANENT -> "Atomic Concept"
            NoteType.MOC -> "MOC Topic"
        }
    }

enum class ParaCategory {
    PROJECT,     // Active goals with deadlines
    AREA,        // Long-term areas of responsibility
    RESOURCE,    // Topics of interest & reference material
    ARCHIVE      // Inactive or completed items
}

object ZettelUidGenerator {
    /**
     * Generates a 12-digit Timestamp UID in format YYYYMMDDHHMM.
     */
    fun generateUid(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmm", AppConstants.APP_LOCALE)
        return sdf.format(Date(timestamp))
    }
}
