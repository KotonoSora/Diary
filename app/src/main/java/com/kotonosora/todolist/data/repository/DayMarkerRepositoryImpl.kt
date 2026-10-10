package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.DayMarkerDao
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.domain.model.DayMarkers
import com.kotonosora.todolist.domain.repository.DayMarkerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DayMarkerRepositoryImpl(
    private val dayMarkerDao: DayMarkerDao
) : DayMarkerRepository {

    override fun observeMarkers(): Flow<Map<String, DayMarkers>> {
        return dayMarkerDao.observeMarkers().map { entities ->
            entities.associate { it.date to it.toDomain() }
        }
    }

    override suspend fun refreshDays(dates: Set<String>) = withContext(Dispatchers.IO) {
        if (dates.isNotEmpty()) dayMarkerDao.refreshDays(dates)
    }

    override suspend fun rebuildAll() = withContext(Dispatchers.IO) {
        val dates = dayMarkerDao.distinctTaskDays() +
                dayMarkerDao.distinctNoteDays() +
                dayMarkerDao.distinctMoodDays()
        dayMarkerDao.clearAll()
        dayMarkerDao.refreshDays(dates.toSet())
    }

    override suspend fun rebuildIfEmpty() = withContext(Dispatchers.IO) {
        // Self-healing: a wiped or pre-marker database rebuilds on next read
        // instead of needing a versioned flag.
        if (dayMarkerDao.markerRowCount() == 0 &&
            (dayMarkerDao.hasAnyTasks() > 0 ||
                    dayMarkerDao.anyNote() != null ||
                    dayMarkerDao.hasAnyMoods() > 0)
        ) {
            rebuildAll()
        }
    }
}
