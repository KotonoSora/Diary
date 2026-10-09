package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.MoodDao
import com.kotonosora.todolist.data.mapper.toDomainOrNull
import com.kotonosora.todolist.data.mapper.toEntity
import com.kotonosora.todolist.domain.model.MoodEntry
import com.kotonosora.todolist.domain.model.dayKeyOf
import com.kotonosora.todolist.domain.repository.DayMarkerRepository
import com.kotonosora.todolist.domain.repository.MoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MoodRepositoryImpl(
    private val moodDao: MoodDao,
    private val dayMarkers: DayMarkerRepository
) : MoodRepository {

    override fun observeMoods(): Flow<List<MoodEntry>> {
        return moodDao.observeMoods().map { entities ->
            entities.mapNotNull { it.toDomainOrNull() }
        }
    }

    override suspend fun getMoodById(id: String): MoodEntry? {
        return moodDao.getMoodById(id)?.toDomainOrNull()
    }

    override suspend fun insertMood(mood: MoodEntry) = withContext(Dispatchers.IO) {
        moodDao.insertMood(mood.toEntity())
        val day = dayKeyOf(mood.createdAt)
        if (day != null) dayMarkers.refreshDays(setOf(day))
    }

    override suspend fun updateMood(mood: MoodEntry) = withContext(Dispatchers.IO) {
        val oldDay = moodDao.getMoodById(mood.id)?.createdAt?.let(::dayKeyOf)
        moodDao.updateMood(mood.toEntity())
        val newDay = dayKeyOf(mood.createdAt)
        if (oldDay != newDay) dayMarkers.refreshDays(setOfNotNull(oldDay, newDay))
    }

    override suspend fun deleteMood(mood: MoodEntry) = withContext(Dispatchers.IO) {
        moodDao.deleteMood(mood.toEntity())
        val day = dayKeyOf(mood.createdAt)
        if (day != null) dayMarkers.refreshDays(setOf(day))
    }

    override suspend fun deleteMoodById(id: String) = withContext(Dispatchers.IO) {
        val oldDay = moodDao.getMoodById(id)?.createdAt?.let(::dayKeyOf)
        moodDao.deleteMoodById(id)
        if (oldDay != null) dayMarkers.refreshDays(setOf(oldDay))
    }
}
