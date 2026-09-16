package com.multilingualbookreader.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        BookPageEntity::class,
        BookTextBlockEntity::class,
        ReadingProgressEntity::class,
        AudioSegmentEntity::class,
        VoiceProfileEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BookReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun pageDao(): BookPageDao
    abstract fun textBlockDao(): TextBlockDao
    abstract fun progressDao(): ProgressDao
    abstract fun audioDao(): AudioSegmentDao
    abstract fun voiceDao(): VoiceProfileDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE books ADD COLUMN color TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
