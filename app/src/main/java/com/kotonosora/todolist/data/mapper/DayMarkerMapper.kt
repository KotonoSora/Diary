package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.DayMarkerEntity
import com.kotonosora.todolist.domain.model.DayMarkerType
import com.kotonosora.todolist.domain.model.DayMarkers
import com.kotonosora.todolist.domain.model.EmotionStamp

fun DayMarkerEntity.toDomain(): DayMarkers {
    val types = buildSet {
        if (taskCount > 0) add(DayMarkerType.TASK)
        if (noteCount > 0) add(DayMarkerType.NOTE)
        if (moodCount > 0) add(DayMarkerType.MOOD)
    }
    // Unknown emotion strings drop the mood dot but keep the counts.
    val emotion = moodEmotion?.let { runCatching { EmotionStamp.valueOf(it) }.getOrNull() }
    return DayMarkers(types = types, moodEmotion = emotion)
}
