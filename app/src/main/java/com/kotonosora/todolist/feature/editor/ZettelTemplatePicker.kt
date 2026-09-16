package com.kotonosora.todolist.feature.editor

import com.kotonosora.todolist.domain.model.ActionStamp
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.model.ZettelUidGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ZettelTemplatePicker {

    fun generateContentForTemplate(
        noteType: NoteType,
        title: String,
        author: String? = null,
        sourceUrl: String? = null,
        emotion: EmotionStamp? = null,
        actions: List<ActionStamp> = emptyList()
    ): String {
        val uid = ZettelUidGenerator.generateUid()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

        val emotionYaml = emotion?.let { "emotion: ${it.name}\n" } ?: ""
        val actionsYaml = if (actions.isNotEmpty()) "actions: [${actions.joinToString { it.name }}]\n" else ""
        val stampFrontmatter = "$emotionYaml$actionsYaml"

        return when (noteType) {
            NoteType.FLEETING -> {
                """
                ---
                uid: $uid
                type: fleeting
                date: $formattedDate
                ${stampFrontmatter}---
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
                date: $formattedDate
                ${stampFrontmatter}---
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
                date: $formattedDate
                ${stampFrontmatter}---
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
                date: $formattedDate
                ${stampFrontmatter}---
                # MOC: $title

                ## Overview
                Map of Content structural hub for $title.

                ## Core Topics
                - [[Atomic Note 1]]
                - [[Atomic Note 2]]
                """.trimIndent()
            }

            NoteType.DIARY -> {
                """
                ---
                uid: $uid
                type: diary
                date: $formattedDate
                tags: [diary, journal]
                ${stampFrontmatter}---
                # Diary: $title

                ## Mood & Energy
                - **Mood Stamp**: ${emotion?.label ?: "Not selected"}
                - **Activities**: ${if (actions.isNotEmpty()) actions.joinToString { it.label } else "None"}

                ## Highlights of the Day
                - What went well today?
                - Key accomplishments and memorable moments.

                ## Daily Reflection
                - What am I grateful for today?
                - What did I learn or want to improve tomorrow?
                """.trimIndent()
            }

            NoteType.DAILY -> {
                """
                ---
                uid: $uid
                type: daily
                date: $formattedDate
                tags: [daily, planner]
                ${stampFrontmatter}---
                # Daily Note: $title

                ## Top Priorities Today
                - [ ] Priority Task 1
                - [ ] Priority Task 2
                - [ ] Priority Task 3

                ## Schedule & Meetings
                - 09:00 AM - Morning Standup
                - 02:00 PM - Focus Time

                ## Quick Log
                - Note down thoughts, links, and ideas during the day.
                """.trimIndent()
            }

            NoteType.REPORT -> {
                """
                ---
                uid: $uid
                type: report
                date: $formattedDate
                author: ${author ?: "Author"}
                tags: [report, meeting]
                ${stampFrontmatter}---
                # Work Report: $title

                ## Executive Summary
                High-level overview of the meeting, project status, or research findings.

                ## Discussion Points & Notes
                - **Topic 1**: Key observations and data points.
                - **Topic 2**: Decisions made during discussion.

                ## Action Items & Next Steps
                - [ ] @assignee: Action item 1 (Due: YYYY-MM-DD)
                - [ ] @assignee: Action item 2
                """.trimIndent()
            }

            NoteType.TODO -> {
                """
                ---
                uid: $uid
                type: todo
                date: $formattedDate
                tags: [todo, tasks]
                ${stampFrontmatter}---
                # Todo List: $title

                ## High Priority
                - [ ] Important task 1
                - [ ] Important task 2

                ## Regular Tasks
                - [ ] Standard task 1
                - [ ] Standard task 2

                ## Completed
                - [x] Initial setup completed
                """.trimIndent()
            }

            NoteType.FLASHCARD -> {
                """
                ---
                uid: $uid
                type: flashcard
                date: $formattedDate
                tags: [flashcard, vocabulary]
                ${stampFrontmatter}---
                # Flashcards: $title

                ## Vocabulary List
                - Serendipity (/ˌser.ənˈdɪp.ə.ti/): Finding valuable things by chance (e.g. Finding the key was pure serendipity)
                - Ephemeral (/ɪˈfem.ər.əl/): Lasting for a very short time
                - Ubiquitous (/juːˈbɪk.wɪ.təs/): Present or appearing everywhere

                ## Terminology Table
                | Term | Definition | Example |
                | --- | --- | --- |
                | Algorithm | Step-by-step problem solving rule | Sorting algorithm |
                | Database | Organized collection of data | SQLite database |
                """.trimIndent()
            }
        }
    }
}
