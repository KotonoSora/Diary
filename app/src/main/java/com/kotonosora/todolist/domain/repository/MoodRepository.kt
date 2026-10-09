package com.kotonosora.todolist.domain.repository

import com.kotonosora.todolist.domain.model.MoodEntry
import kotlinx.coroutines.flow.Flow

interface MoodRepository {
    fun observeMoods(): Flow<List<MoodEntry>>
    suspend fun getMoodById(id: String): MoodEntry?
    suspend fun insertMood(mood: MoodEntry)
    suspend fun updateMood(mood: MoodEntry)
    suspend fun deleteMood(mood: MoodEntry)
    suspend fun deleteMoodById(id: String)
}
