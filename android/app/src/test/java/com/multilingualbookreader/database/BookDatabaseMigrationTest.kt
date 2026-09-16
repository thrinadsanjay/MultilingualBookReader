package com.multilingualbookreader.database

import android.app.Application
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDatabaseMigrationTest {
    @Test
    fun migration1to2KeepsScannedBooksAndAddsLibraryColumns() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migrate-1-2.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `books` (
                                    `id` TEXT NOT NULL,
                                    `title` TEXT NOT NULL,
                                    `author` TEXT,
                                    `coverPath` TEXT,
                                    `sourceType` TEXT NOT NULL,
                                    `language` TEXT NOT NULL,
                                    `totalPages` INTEGER NOT NULL,
                                    `createdAt` INTEGER NOT NULL,
                                    `updatedAt` INTEGER NOT NULL,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                INSERT INTO books (id, title, author, coverPath, sourceType, language, totalPages, createdAt, updatedAt)
                                VALUES ('keep', 'Scanned book', NULL, NULL, 'CAMERA_SCAN', 'te', 1, 10, 10)
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        val db = helper.writableDatabase
        BookReaderDatabase.MIGRATION_1_2.migrate(db)
        db.query("SELECT title, tags, priority, color, genre, favorite FROM books WHERE id = 'keep'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Scanned book")
            assertThat(cursor.getString(1)).isEqualTo("")
            assertThat(cursor.getString(2)).isEqualTo("NORMAL")
            assertThat(cursor.getString(3)).isEqualTo("")
            assertThat(cursor.getString(4)).isEqualTo("")
            assertThat(cursor.getInt(5)).isEqualTo(0)
        }
        helper.close()
    }
}
