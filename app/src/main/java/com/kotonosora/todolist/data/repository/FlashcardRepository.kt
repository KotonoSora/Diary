package com.kotonosora.todolist.data.repository

import com.kotonosora.todolist.data.database.DeckProgressDao
import com.kotonosora.todolist.data.database.DeckProgressEntity
import com.kotonosora.todolist.data.database.FlashcardCardDao
import com.kotonosora.todolist.data.database.FlashcardCardEntity
import com.kotonosora.todolist.data.database.FlashcardDeckDao
import com.kotonosora.todolist.data.database.FlashcardDeckEntity
import androidx.room.withTransaction
import com.kotonosora.todolist.data.database.DeckProgressDatabase
import com.kotonosora.todolist.feature.flashcard.DemoFlashcardData
import com.kotonosora.todolist.feature.flashcard.Flashcard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

data class DeckWithMeta(
    val id: String,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean,
    val cardCount: Int,
    val mastered: Int
)

class FlashcardRepository(
    private val database: DeckProgressDatabase,
    private val deckDao: FlashcardDeckDao,
    private val cardDao: FlashcardCardDao,
    private val progressDao: DeckProgressDao
) {

    fun observeDecks(): Flow<List<DeckWithMeta>> = combine(
        deckDao.observeAll(),
        cardDao.observeCounts(),
        progressDao.getAll()
    ) { decks, counts, progress ->
        val countById = counts.associate { it.deckId to it.count }
        val masteredById = progress.associate { it.deckId to it.mastered }
        decks.map { d ->
            DeckWithMeta(
                id = d.id,
                name = d.name,
                description = d.description,
                isBuiltIn = d.isBuiltIn,
                cardCount = countById[d.id] ?: 0,
                mastered = (masteredById[d.id] ?: 0).coerceIn(0, (countById[d.id] ?: 0))
            )
        }
    }

    fun observeCards(deckId: String): Flow<List<Flashcard>> =
        cardDao.observeByDeck(deckId).map { rows -> rows.map { it.toDomain() } }

    fun observeDeck(deckId: String): Flow<DeckWithMeta?> =
        observeDecks().map { decks -> decks.firstOrNull { it.id == deckId } }

    suspend fun getCardsOnce(deckId: String): List<Flashcard> =
        cardDao.getByDeckOnce(deckId).map { it.toDomain() }

    suspend fun getDeckById(deckId: String): FlashcardDeckEntity? =
        deckDao.getById(deckId)

    suspend fun getDeckName(deckId: String): String? =
        deckDao.getById(deckId)?.name

    /**
     * One-shot seeding of the built-in decks. Transactional so a failure partway
     * cannot leave half-seeded decks behind (the persisted seeded flag would then
     * suppress the retry). Built-ins are never deletable, so once seeded they
     * cannot return to an empty decks table.
     */
    suspend fun ensureSeeded() {
        if (deckDao.getAllOnce().isNotEmpty()) return
        database.withTransaction {
            if (deckDao.getAllOnce().isNotEmpty()) return@withTransaction
            val now = System.currentTimeMillis()
            DemoFlashcardData.seedDecks.forEach { seed ->
                deckDao.upsert(
                    FlashcardDeckEntity(
                        id = seed.id,
                        name = seed.name,
                        description = seed.description,
                        isBuiltIn = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                val (_, cards) = DemoFlashcardData.demoCards(seed.id)
                cardDao.upsertAll(
                    cards.mapIndexed { index, c ->
                        FlashcardCardEntity(
                            id = UUID.randomUUID().toString(),
                            deckId = seed.id,
                            word = c.word,
                            definition = c.definition,
                            phonetic = c.phonetic,
                            example = c.example,
                            position = index,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                )
            }
        }
    }

    suspend fun createDeck(name: String, description: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Deck name must not be blank" }
        val id = "deck_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        deckDao.upsert(
            FlashcardDeckEntity(
                id = id,
                name = trimmed,
                description = description.trim(),
                isBuiltIn = false,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun renameDeck(deckId: String, name: String, description: String) {
        val existing = deckDao.getById(deckId) ?: return
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Deck name must not be blank" }
        deckDao.upsert(
            existing.copy(
                name = trimmed,
                description = description.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Removes a user-created deck. Built-ins are refused: they are re-seeded on
     * next launch, so deleting one would make it silently reappear. Runs in a
     * single transaction so a deck is never left with orphan cards/progress, and
     * deletes the deck row last so the FK cascade stays authoritative.
     */
    suspend fun deleteDeck(deckId: String) = database.withTransaction {
        val deck = deckDao.getById(deckId) ?: return@withTransaction
        require(!deck.isBuiltIn) { "Built-in decks cannot be deleted" }
        cardDao.deleteByDeck(deckId)
        progressDao.deleteById(deckId)
        deckDao.deleteById(deckId)
    }

    suspend fun addCard(
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    ): String {
        require(deckDao.getById(deckId) != null) { "Deck not found: $deckId" }
        val trimmedWord = word.trim()
        require(trimmedWord.isNotEmpty()) { "Card word must not be blank" }
        val position = (cardDao.maxPosition(deckId) + 1).coerceAtLeast(0)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        cardDao.upsert(
            FlashcardCardEntity(
                id = id,
                deckId = deckId,
                word = trimmedWord,
                definition = definition.trim(),
                phonetic = phonetic.trim(),
                example = example.trim(),
                position = position,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun updateCard(
        cardId: String,
        deckId: String,
        word: String,
        definition: String = "",
        phonetic: String = "",
        example: String = ""
    ) {
        require(deckDao.getById(deckId) != null) { "Deck not found: $deckId" }
        val trimmedWord = word.trim()
        require(trimmedWord.isNotEmpty()) { "Card word must not be blank" }
        val existing = cardDao.getByDeckOnce(deckId).firstOrNull { it.id == cardId } ?: return
        cardDao.upsert(
            existing.copy(
                word = trimmedWord,
                definition = definition.trim(),
                phonetic = phonetic.trim(),
                example = example.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCard(cardId: String) {
        cardDao.deleteById(cardId)
    }

    suspend fun saveFullRun(deckId: String, mastered: Int, total: Int) {
        if (total <= 0) return
        progressDao.upsert(
            DeckProgressEntity(
                deckId = deckId,
                mastered = mastered.coerceIn(0, total),
                total = total
            )
        )
    }

    fun progressFor(deckIds: List<String>): Flow<Map<String, Int>> =
        progressDao.getAll().map { rows ->
            val byId = rows.associateBy { it.deckId }
            deckIds.mapNotNull { id -> byId[id]?.let { id to it.mastered } }.toMap()
        }

    private fun FlashcardCardEntity.toDomain(): Flashcard = Flashcard(
        id = id,
        word = word,
        definition = definition,
        phonetic = phonetic,
        example = example
    )
}
