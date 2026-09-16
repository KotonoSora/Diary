package com.kotonosora.todolist.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TodoEntity::class,
        MediaEntity::class,
        NoteEntity::class,
        LinkEntity::class,
        TagEntity::class,
        NoteFtsEntity::class,
        ZettelMetadataEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun mediaDao(): MediaDao
    abstract fun noteDao(): NoteDao
    abstract fun linkDao(): LinkDao
    abstract fun tagDao(): TagDao
    abstract fun zettelMetadataDao(): ZettelMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todolist_database"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA synchronous = NORMAL;")
                            db.execSQL("PRAGMA temp_store = MEMORY;")
                        }
                    })
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
