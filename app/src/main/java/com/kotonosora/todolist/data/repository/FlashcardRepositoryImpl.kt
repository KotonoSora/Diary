package com.kotonosora.todolist.data.repository

import androidx.room.withTransaction
import com.kotonosora.todolist.data.database.DeckProgressDao
import com.kotonosora.todolist.data.database.DeckProgressDatabase
import com.kotonosora.todolist.data.database.DeckProgressEntity
import com.kotonosora.todolist.data.database.FlashcardCardDao
import com.kotonosora.todolist.data.database.FlashcardCardEntity
import com.kotonosora.todolist.data.database.FlashcardDeckDao
import com.kotonosora.todolist.data.database.FlashcardDeckEntity
import com.kotonosora.todolist.data.flashcard.FlashcardSeedData
import com.kotonosora.todolist.data.mapper.toDomain
import com.kotonosora.todolist.data.mapper.toInfo
import com.kotonosora.todolist.domain.model.DeckInfo
import com.kotonosora.todolist.domain.model.DeckWithMeta
import com.kotonosora.todolist.domain.model.FlashcardCard
import com.kotonosora.todolist.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class FlashcardRepositoryImpl(
    private val database: DeckProgressDatabase,
    private val deckDao: FlashcardDeckDao,
    private val cardDao: FlashcardCardDao,
    private val progressDao: DeckProgressDao
) : FlashcardRepository {

    override fun observeDecks(): Flow<List<DeckWithMeta>> = combine(
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

    override fun observeCards(deckId: String): Flow<List<FlashcardCard>> =
        cardDao.observeByDeck(deckId).map { rows -> rows.map { it.toDomain() } }

    override fun observeDeck(deckId: String): Flow<DeckWithMeta?> =
        observeDecks().map { decks -> decks.firstOrNull { it.id == deckId } }

    override suspend fun getCardsOnce(deckId: String): List<FlashcardCard> =
        cardDao.getByDeckOnce(deckId).map { it.toDomain() }

    override suspend fun getDeck(deckId: String): DeckInfo? =
        deckDao.getById(deckId)?.toInfo()

    /**
     * One-shot seeding of the built-in decks. Transactional so a failure partway
     * cannot leave half-seeded decks behind (the persisted seeded flag would then
     * suppress the retry). Built-ins are never deletable, so once seeded they
     * cannot return to an empty decks table.
     */
    override suspend fun ensureSeeded() {
        if (deckDao.getAllOnce().isNotEmpty()) return
        database.withTransaction {
            if (deckDao.getAllOnce().isNotEmpty()) return@withTransaction
            val now = System.currentTimeMillis()
            FlashcardSeedData.seedDecks.forEach { seed ->
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
                val (_, cards) = FlashcardSeedData.demoCards(seed.id)
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

    override suspend fun createDeck(name: String, description: String): String {
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

    override suspend fun renameDeck(deckId: String, name: String, description: String) {
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
    override suspend fun deleteDeck(deckId: String) = database.withTransaction {
        val deck = deckDao.getById(deckId) ?: return@withTransaction
        require(!deck.isBuiltIn) { "Built-in decks cannot be deleted" }
        cardDao.deleteByDeck(deckId)
        progressDao.deleteById(deckId)
        deckDao.deleteById(deckId)
    }

    override suspend fun addCard(
        deckId: String,
        word: String,
        definition: String,
        phonetic: String,
        example: String
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

    override suspend fun updateCard(
        cardId: String,
        deckId: String,
        word: String,
        definition: String,
        phonetic: String,
        example: String
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

    override suspend fun deleteCard(cardId: String) {
        cardDao.deleteById(cardId)
    }

    /**
     * Records a finished run. The stored value is the best score seen so far,
     * so a weaker re-run never regresses the deck's global progress shown
     * in the deck list.
     */
    override suspend fun saveFullRun(deckId: String, mastered: Int, total: Int) {
        if (total <= 0) return
        val clamped = mastered.coerceIn(0, total)
        val previous = try {
            progressDao.getById(deckId)
        } catch (_: Exception) {
            null
        }
        val best = maxOf(previous?.mastered ?: 0, clamped).coerceIn(0, total)
        progressDao.upsert(
            DeckProgressEntity(
                deckId = deckId,
                mastered = best,
                total = total
            )
        )
    }

    override fun progressFor(deckIds: List<String>): Flow<Map<String, Int>> =
        progressDao.getAll().map { rows ->
            val byId = rows.associateBy { it.deckId }
            deckIds.mapNotNull { id -> byId[id]?.let { id to it.mastered } }.toMap()
        }
}
