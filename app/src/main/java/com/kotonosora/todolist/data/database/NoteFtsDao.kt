package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface NoteFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFts(entry: NoteFtsEntity)

    @Query("DELETE FROM notes_fts WHERE noteId = :noteId")
    suspend fun deleteFtsForNote(noteId: String)

    @Transaction
    suspend fun replaceFtsForNote(noteId: String, entry: NoteFtsEntity) {
        deleteFtsForNote(noteId)
        upsertFts(entry)
    }

    @Query("DELETE FROM notes_fts")
    suspend fun deleteAllFts()
}
