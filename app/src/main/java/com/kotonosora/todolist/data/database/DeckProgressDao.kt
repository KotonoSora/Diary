package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckProgressDao {

    @Query("SELECT * FROM deck_progress")
    fun getAll(): Flow<List<DeckProgressEntity>>

    @Query("SELECT * FROM deck_progress WHERE deckId = :deckId")
    suspend fun getById(deckId: String): DeckProgressEntity?

    @Upsert
    suspend fun upsert(progress: DeckProgressEntity)

    @Query("DELETE FROM deck_progress WHERE deckId = :deckId")
    suspend fun deleteById(deckId: String)
}
