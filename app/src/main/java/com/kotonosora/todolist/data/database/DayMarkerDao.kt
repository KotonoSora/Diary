package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Persisted calendar day-markers. Writers recompute whole days (never
 * increment/decrement counters) so date-moves and purges can't drift.
 * Days whose counts all reach zero are deleted — readers ignore them.
 */
@Dao
interface DayMarkerDao {
    @Query("SELECT * FROM day_markers")
    fun observeMarkers(): Flow<List<DayMarkerEntity>>

    @Query("SELECT COUNT(*) FROM day_markers")
    suspend fun markerRowCount(): Int

    @Query("SELECT COUNT(*) FROM todo_items")
    suspend fun hasAnyTasks(): Int

    @Query("SELECT * FROM notes LIMIT 1")
    suspend fun anyNote(): NoteEntity?

    @Query("SELECT COUNT(*) FROM mood_entries")
    suspend fun hasAnyMoods(): Int

    @Query(
        "SELECT COUNT(*) FROM todo_items " +
                "WHERE date(dueDate / 1000, 'unixepoch', 'localtime') = :date"
    )
    suspend fun countTasksOn(date: String): Int

    @Query(
        "SELECT COUNT(*) FROM notes " +
                "WHERE date(updatedAt / 1000, 'unixepoch', 'localtime') = :date"
    )
    suspend fun countNotesOn(date: String): Int

    @Query(
        "SELECT COUNT(*) FROM mood_entries " +
                "WHERE date(createdAt / 1000, 'unixepoch', 'localtime') = :date"
    )
    suspend fun countMoodsOn(date: String): Int

    @Query(
        "SELECT emotion FROM mood_entries " +
                "WHERE date(createdAt / 1000, 'unixepoch', 'localtime') = :date " +
                "GROUP BY emotion ORDER BY COUNT(*) DESC, MAX(createdAt) DESC LIMIT 1"
    )
    suspend fun dominantEmotionOn(date: String): String?

    @Query(
        "SELECT DISTINCT date(dueDate / 1000, 'unixepoch', 'localtime') FROM todo_items " +
                "WHERE dueDate IS NOT NULL"
    )
    suspend fun distinctTaskDays(): List<String>

    @Query(
        "SELECT DISTINCT date(updatedAt / 1000, 'unixepoch', 'localtime') FROM notes"
    )
    suspend fun distinctNoteDays(): List<String>

    @Query(
        "SELECT DISTINCT date(createdAt / 1000, 'unixepoch', 'localtime') FROM mood_entries"
    )
    suspend fun distinctMoodDays(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMarker(marker: DayMarkerEntity)

    @Query("DELETE FROM day_markers WHERE date = :date")
    suspend fun deleteMarker(date: String)

    @Query("DELETE FROM day_markers")
    suspend fun clearAll()

    @Transaction
    suspend fun refreshDays(dates: Set<String>) {
        dates.forEach { date ->
            val moodCount = countMoodsOn(date)
            val marker = DayMarkerEntity(
                date = date,
                taskCount = countTasksOn(date),
                noteCount = countNotesOn(date),
                moodCount = moodCount,
                moodEmotion = if (moodCount > 0) dominantEmotionOn(date) else null
            )
            if (marker.taskCount + marker.noteCount + marker.moodCount == 0) {
                deleteMarker(date)
            } else {
                upsertMarker(marker)
            }
        }
    }
}
