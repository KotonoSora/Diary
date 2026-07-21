package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_attachments WHERE todoId = :todoId ORDER BY createdAt ASC")
    fun getMediaForTodo(todoId: String): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Delete
    suspend fun deleteMedia(media: MediaEntity)

    @Query("DELETE FROM media_attachments WHERE todoId = :todoId")
    suspend fun deleteAllMediaForTodo(todoId: String)

    @Query("SELECT * FROM media_attachments ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>
}

