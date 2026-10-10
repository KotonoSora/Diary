package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT DISTINCT tagName FROM tags ORDER BY tagName ASC")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT * FROM tags WHERE noteId = :noteId")
    fun getTagsForNote(noteId: String): Flow<List<TagEntity>>

    @Query("SELECT noteId FROM tags WHERE tagName = :tagName")
    fun getNoteIdsForTag(tagName: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: String)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
