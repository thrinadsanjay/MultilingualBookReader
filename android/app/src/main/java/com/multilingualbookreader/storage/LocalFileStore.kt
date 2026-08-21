package com.multilingualbookreader.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val booksDir: File get() = dir("books")
    val audioDir: File get() = dir("audio")
    val voiceDir: File get() = dir("voice")
    val pagesDir: File get() = dir("pages")

    fun savePageImage(bookId: String, pageNumber: Int, bytes: ByteArray): String {
        val file = File(pagesDir, "$bookId-${pageNumber.toString().padStart(4, '0')}.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun savePdfCopy(originalName: String, bytes: ByteArray): String {
        val safe = originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(booksDir, "${UUID.randomUUID()}-$safe")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun saveAudio(cacheKey: String, bytes: ByteArray, extension: String = "mp3"): String {
        val file = File(audioDir, "$cacheKey.$extension")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun audioFile(cacheKey: String, extension: String = "mp3"): File = File(audioDir, "$cacheKey.$extension")

    fun saveVoiceSample(profileId: String, index: Int, bytes: ByteArray): String {
        val dir = File(voiceDir, profileId).apply { mkdirs() }
        val file = File(dir, "sample-$index.wav")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun deleteVoiceSamples(profileId: String) {
        File(voiceDir, profileId).deleteRecursively()
    }

    fun deleteBookFiles(bookId: String) {
        pagesDir.listFiles()?.filter { it.name.startsWith(bookId) }?.forEach { it.delete() }
    }

    fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun dir(name: String): File = File(context.filesDir, name).apply { mkdirs() }
}
