package com.kotonosora.todolist.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeckProgressEntity::class,
        FlashcardDeckEntity::class,
        FlashcardCardEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DeckProgressDatabase : RoomDatabase() {

    abstract fun deckProgressDao(): DeckProgressDao
    abstract fun flashcardDeckDao(): FlashcardDeckDao
    abstract fun flashcardCardDao(): FlashcardCardDao

    companion object {
        @Volatile
        private var INSTANCE: DeckProgressDatabase? = null

        private const val DB_NAME = "deck_progress_database"

        fun getDatabase(context: Context): DeckProgressDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeckProgressDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
