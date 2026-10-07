package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.DeckProgressDao
import com.kotonosora.todolist.data.database.DeckProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeckProgressRepository(
    private val deckProgressDao: DeckProgressDao
) {

    fun progressFor(deckIds: List<String>): Flow<Map<String, Int>> =
        deckProgressDao.getAll().map { rows ->
            val byId = rows.associateBy { it.deckId }
            deckIds.mapNotNull { id ->
                byId[id]?.let { id to it.mastered }
            }.toMap()
        }

    suspend fun saveFullRun(deckId: String, mastered: Int, total: Int) {
        if (total <= 0) return
        deckProgressDao.upsert(
            DeckProgressEntity(
                deckId = deckId,
                mastered = mastered.coerceIn(0, total),
                total = total
            )
        )
    }
}
