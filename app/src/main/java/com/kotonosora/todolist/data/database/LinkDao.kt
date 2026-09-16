package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links WHERE sourceNoteId = :sourceNoteId")
    fun getOutgoingLinksForNote(sourceNoteId: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE targetTitle = :targetTitle")
    fun getIncomingLinksForNoteTitle(targetTitle: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<LinkEntity>)

    @Query("DELETE FROM links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteLinksForSource(sourceNoteId: String)

    @Query("DELETE FROM links")
    suspend fun deleteAllLinks()
}
