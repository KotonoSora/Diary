package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC")
    fun observeMoods(): Flow<List<MoodEntity>>

    @Query("SELECT * FROM mood_entries WHERE id = :id")
    suspend fun getMoodById(id: String): MoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntity)

    @Update
    suspend fun updateMood(mood: MoodEntity)

    @Delete
    suspend fun deleteMood(mood: MoodEntity)

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteMoodById(id: String)
}
