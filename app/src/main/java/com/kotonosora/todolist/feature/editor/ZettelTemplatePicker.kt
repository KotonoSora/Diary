package com.kotonosora.todolist.feature.editor

import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.ZettelUidGenerator

object ZettelTemplatePicker {

    fun generateContentForTemplate(
        noteType: NoteType,
        title: String,
        author: String? = null,
        sourceUrl: String? = null
    ): String {
        val uid = ZettelUidGenerator.generateUid()
        return when (noteType) {
            NoteType.FLEETING -> {
                """
                ---
                uid: $uid
                type: fleeting
                date: ${System.currentTimeMillis()}
                ---
                # $title

                - Quick idea captured on mobile.
                """.trimIndent()
            }

            NoteType.LITERATURE -> {
                """
                ---
                uid: $uid
                type: literature
                author: ${author ?: "Unknown"}
                url: ${sourceUrl ?: ""}
                date: ${System.currentTimeMillis()}
                ---
                # $title

                ## Source Reference
                - **Author**: ${author ?: "N/A"}
                - **URL**: ${sourceUrl ?: "N/A"}

                ## Key Quotes & Highlights
                > "Insert key quotation or highlight here."

                ## Summary
                - Summary of the reading in my own words.
                """.trimIndent()
            }

            NoteType.PERMANENT -> {
                """
                ---
                uid: $uid
                type: permanent
                date: ${System.currentTimeMillis()}
                ---
                # $title

                ## Statement (1 Idea Rule)
                State the atomic concept clearly in 1-2 sentences.

                ## Elaboration
                Detailed reasoning and context.

                ## Connected Notes
                - [[Linked Zettel Note]]
                """.trimIndent()
            }

            NoteType.MOC -> {
                """
                ---
                uid: $uid
                type: moc
                date: ${System.currentTimeMillis()}
                ---
                # MOC: $title

                ## Overview
                Map of Content structural hub for $title.

                ## Core Topics
                - [[Atomic Note 1]]
                - [[Atomic Note 2]]
                """.trimIndent()
            }
        }
    }
}
