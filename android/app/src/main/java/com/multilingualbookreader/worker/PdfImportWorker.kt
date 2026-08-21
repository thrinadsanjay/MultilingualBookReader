package com.multilingualbookreader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.multilingualbookreader.pdf.PdfImportProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class PdfImportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: PdfImportProcessor,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PATH) ?: return Result.failure()
        val name = inputData.getString(KEY_NAME) ?: "Imported book.pdf"
        val file = File(path)
        if (!file.exists()) return Result.failure()
        return runCatching {
            val bookId = processor.importPdf(name, file.readBytes())
            file.delete()
            Result.success(workDataOf(KEY_BOOK_ID to bookId))
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val KEY_PATH = "path"
        const val KEY_NAME = "name"
        const val KEY_BOOK_ID = "bookId"
        const val UNIQUE = "pdf-import"
    }
}
