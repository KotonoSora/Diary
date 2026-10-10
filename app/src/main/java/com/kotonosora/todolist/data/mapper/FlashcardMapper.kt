package com.kotonosora.todolist.data.mapper

import com.kotonosora.todolist.data.database.FlashcardCardEntity
import com.kotonosora.todolist.data.database.FlashcardDeckEntity
import com.kotonosora.todolist.domain.model.DeckInfo
import com.kotonosora.todolist.domain.model.FlashcardCard

fun FlashcardCardEntity.toDomain(): FlashcardCard {
    return FlashcardCard(
        id = id,
        deckId = deckId,
        word = word,
        definition = definition,
        phonetic = phonetic,
        example = example,
        position = position
    )
}

fun FlashcardDeckEntity.toInfo(): DeckInfo {
    return DeckInfo(
        id = id,
        name = name,
        description = description,
        isBuiltIn = isBuiltIn
    )
}
