package com.multilingualbookreader.presentation.pdf

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.multilingualbookreader.pdf.PdfImportProcessor
import com.multilingualbookreader.worker.PdfImportWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PdfImportUiState(
    val busy: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val message: String? = null,
    val bookId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class PdfImportViewModel @Inject constructor(
    application: Application,
    private val processor: PdfImportProcessor,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(PdfImportUiState())
    val state: StateFlow<PdfImportUiState> = _state

    init {
        viewModelScope.launch {
            processor.progress.collect { progress ->
                if (progress != null) {
                    _state.value = _state.value.copy(
                        busy = true,
                        currentPage = progress.currentPage,
                        totalPages = progress.totalPages,
                        message = "Processing book...\nPage ${progress.currentPage} / ${progress.totalPages}",
                    )
                }
            }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _state.value = PdfImportUiState(busy = true, message = "Preparing PDF…")
            runCatching {
                val context = getApplication<Application>()
                val temp = File(context.cacheDir, "import-${UUID.randomUUID()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open that file.")
                val name = displayName(uri)
                val request = OneTimeWorkRequestBuilder<PdfImportWorker>()
                    .setInputData(workDataOf(PdfImportWorker.KEY_PATH to temp.absolutePath, PdfImportWorker.KEY_NAME to name))
                    .build()
                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniqueWork(PdfImportWorker.UNIQUE, ExistingWorkPolicy.REPLACE, request)
                workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                    when (info?.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val bookId = info.outputData.getString(PdfImportWorker.KEY_BOOK_ID)
                            _state.value = PdfImportUiState(bookId = bookId, message = "Ready to read.")
                        }
                        WorkInfo.State.FAILED -> _state.value = PdfImportUiState(error = "This PDF could not be imported.")
                        WorkInfo.State.RUNNING -> Unit
                        else -> Unit
                    }
                }
            }.onFailure {
                _state.value = PdfImportUiState(error = "This file is not a readable PDF.")
            }
        }
    }

    /**
     * Storage URIs end in an opaque document id, so the last path segment produced titles like
     * "document:108102". The provider knows the real file name.
     */
    private fun displayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val fromProvider = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        }.getOrNull()
        val candidate = fromProvider?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
        return candidate
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
            ?: "Imported book.pdf"
    }
}
