package com.multilingualbookreader.database

import androidx.room.Database
import androidx.room.RoomDatabase

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
    version = 1,
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
}
