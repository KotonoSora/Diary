package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.MoodEntity
import com.kotonosora.todolist.domain.model.EmotionStamp
import com.kotonosora.todolist.domain.model.MoodEntry

/**
 * Returns null when the stored emotion name no longer maps to [EmotionStamp]
 * (e.g. hand-edited DB) so callers can drop the corrupt row instead of
 * mislabeling the user's history.
 */
fun MoodEntity.toDomainOrNull(): MoodEntry? {
    val emotion = runCatching { EmotionStamp.valueOf(emotion) }.getOrNull()
        ?: return null
    return MoodEntry(
        id = id,
        emotion = emotion,
        note = note,
        linkedNoteId = linkedNoteId,
        createdAt = createdAt
    )
}

fun MoodEntry.toEntity(): MoodEntity {
    return MoodEntity(
        id = id,
        emotion = emotion.name,
        note = note,
        linkedNoteId = linkedNoteId,
        createdAt = createdAt
    )
}
