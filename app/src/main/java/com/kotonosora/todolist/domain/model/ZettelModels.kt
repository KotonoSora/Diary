package com.kotonosora.todolist.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NoteType {
    FLEETING,    // Ephemeral scratchpad thoughts / voice notes
    LITERATURE,  // Reading notes, highlights, quotes with citations
    PERMANENT,   // Atomic, self-contained thoughts (1 Idea Rule)
    MOC          // Map of Content (Structural Index Note)
}

enum class ParaCategory {
    PROJECT,     // Active goals with deadlines
    AREA,        // Long-term areas of responsibility
    RESOURCE,    // Topics of interest & reference material
    ARCHIVE      // Inactive or completed items
}

object ZettelUidGenerator {
    /**
     * Generates a 12-digit Zettelkasten Timestamp UID in format YYYYMMDDHHMM.
     */
    fun generateUid(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
