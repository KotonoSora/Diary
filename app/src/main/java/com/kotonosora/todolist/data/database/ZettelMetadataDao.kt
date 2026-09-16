package com.kotonosora.todolist.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZettelMetadataDao {
    @Query("SELECT * FROM zettel_metadata WHERE noteId = :noteId")
    suspend fun getMetadataForNote(noteId: String): ZettelMetadataEntity?

    @Query("SELECT * FROM zettel_metadata WHERE uid = :uid LIMIT 1")
    suspend fun getMetadataByUid(uid: String): ZettelMetadataEntity?

    @Query("SELECT noteId FROM zettel_metadata WHERE noteType = :noteType")
    fun getNoteIdsByType(noteType: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: ZettelMetadataEntity)

    @Query("DELETE FROM zettel_metadata WHERE noteId = :noteId")
    suspend fun deleteMetadataForNote(noteId: String)
}
