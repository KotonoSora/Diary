package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.DayMarkers
import kotlinx.coroutines.flow.Flow

interface DayMarkerRepository {
    fun observeMarkers(): Flow<Map<String, DayMarkers>>
    suspend fun refreshDays(dates: Set<String>)
    suspend fun rebuildAll()
    suspend fun rebuildIfEmpty()
}
