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

    /**
     * Records a finished run. The stored value is the best score seen so far,
     * so a weaker re-run never regresses the deck's global progress.
     */
    suspend fun saveFullRun(deckId: String, mastered: Int, total: Int) {
        if (total <= 0) return
        val clamped = mastered.coerceIn(0, total)
        val previous = try {
            deckProgressDao.getById(deckId)
        } catch (_: Exception) {
            null
        }
        val best = maxOf(previous?.mastered ?: 0, clamped).coerceIn(0, total)
        deckProgressDao.upsert(
            DeckProgressEntity(
                deckId = deckId,
                mastered = best,
                total = total
            )
        )
    }
}
