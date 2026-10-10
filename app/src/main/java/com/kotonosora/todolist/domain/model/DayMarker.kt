package com.kotonosora.todolist.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calendar day-marker types in dot priority order: task → note → mood.
 */
enum class DayMarkerType { TASK, NOTE, MOOD }

data class DayMarkers(
    val types: Set<DayMarkerType> = emptySet(),
    val moodEmotion: EmotionStamp? = null
)

/** Day key (`yyyy-MM-dd`, system zone) shared by writers and readers. */
fun dayKeyOf(epochMillis: Long?): String? {
    if (epochMillis == null) return null
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

fun dayKeyToDate(date: String): LocalDate = LocalDate.parse(date)
