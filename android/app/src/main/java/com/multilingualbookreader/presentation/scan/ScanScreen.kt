package com.multilingualbookreader.presentation.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.presentation.components.LargeButton
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScanRoute(
    onOpenReader: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onGalleryPicked)
    }
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onGalleryPicked)
    }
    fun pickGallery() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContent.launch("image/*")
        }
    }
    LaunchedEffect(Unit) {
        if (!granted) permission.launch(Manifest.permission.CAMERA)
    }
    ScanScreen(
        state = state,
        cameraGranted = granted,
        onCapture = viewModel::onCaptured,
        onPickGallery = ::pickGallery,
        onRequestCamera = { permission.launch(Manifest.permission.CAMERA) },
        onTextChange = viewModel::updateText,
        onSave = viewModel::savePage,
        onRetake = viewModel::retake,
        onRead = { state.bookId?.let(onOpenReader) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onCapture: (ByteArray) -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onRead: () -> Unit,
    onBack: () -> Unit,
    cameraGranted: Boolean = true,
    onPickGallery: () -> Unit = {},
    onRequestCamera: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (state.preview == null) {
            CapturePane(
                modifier = Modifier.padding(padding),
                pageCount = state.pageCount,
                busy = state.busy,
                error = state.error,
                cameraGranted = cameraGranted,
                onCapture = onCapture,
                onPickGallery = onPickGallery,
                onRequestCamera = onRequestCamera,
                onRead = onRead.takeIf { state.pageCount > 0 },
            )
        } else {
            ReviewPane(
                modifier = Modifier.padding(padding),
                preview = state.preview,
                text = state.ocrText,
                language = state.language.displayName,
                blurry = state.blurry,
                error = state.error,
                busy = state.busy,
                onTextChange = onTextChange,
                onSave = onSave,
                onRetake = onRetake,
            )
        }
    }
}

@Composable
private fun CapturePane(
    modifier: Modifier,
    pageCount: Int,
    busy: Boolean,
    error: String?,
    cameraGranted: Boolean,
    onCapture: (ByteArray) -> Unit,
    onPickGallery: () -> Unit,
    onRequestCamera: () -> Unit,
    onRead: (() -> Unit)?,
) {
    if (cameraGranted) {
        CameraPane(
            modifier = modifier,
            pageCount = pageCount,
            busy = busy,
            error = error,
            onCapture = onCapture,
            onPickGallery = onPickGallery,
            onRead = onRead,
        )
    } else {
        GalleryFallbackPane(
            modifier = modifier,
            pageCount = pageCount,
            busy = busy,
            error = error,
            onPickGallery = onPickGallery,
            onRequestCamera = onRequestCamera,
            onRead = onRead,
        )
    }
}

@Composable
private fun GalleryFallbackPane(
    modifier: Modifier,
    pageCount: Int,
    busy: Boolean,
    error: String?,
    onPickGallery: () -> Unit,
    onRequestCamera: () -> Unit,
    onRead: (() -> Unit)?,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Scan a page with the camera, or pick a photo from your gallery.")
        if (pageCount > 0) Text("Pages saved: $pageCount")
        error?.let { Text(it) }
        if (busy) CircularProgressIndicator()
        LargeButton("Allow camera", onRequestCamera, enabled = !busy)
        LargeButton("Choose from gallery", onPickGallery, tonal = true, enabled = !busy, icon = Icons.Outlined.PhotoLibrary)
        onRead?.let { LargeButton("Read book", it, tonal = true) }
    }
}

@Composable
private fun CameraPane(
    modifier: Modifier,
    pageCount: Int,
    busy: Boolean,
    error: String?,
    onCapture: (ByteArray) -> Unit,
    onPickGallery: () -> Unit,
    onRead: (() -> Unit)?,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    Box(modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener(
                    {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    },
                    ContextCompat.getMainExecutor(ctx),
                )
                previewView
            },
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.84f
            val h = size.height * 0.72f
            val left = (size.width - w) / 2
            val top = (size.height - h) / 2
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 6f),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Align the page inside the frame", color = Color.White)
            if (pageCount > 0) Text("Pages saved: $pageCount", color = Color.White)
            error?.let { Text(it, color = Color.White) }
            if (busy) CircularProgressIndicator()
            LargeButton("Capture page", {
                val file = File(context.cacheDir, "capture.jpg")
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    output,
                    Executors.newSingleThreadExecutor(),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onCapture(file.readBytes())
                        }
                        override fun onError(exception: ImageCaptureException) = Unit
                    },
                )
            }, enabled = !busy)
            LargeButton("Choose from gallery", onPickGallery, tonal = true, enabled = !busy, icon = Icons.Outlined.PhotoLibrary)
            onRead?.let { LargeButton("Read book", it, tonal = true) }
        }
    }
}

@Composable
private fun ReviewPane(
    modifier: Modifier,
    preview: Bitmap,
    text: String,
    language: String,
    blurry: Boolean,
    error: String?,
    busy: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = "Captured page",
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentScale = ContentScale.Fit,
        )
        Text("Detected language: $language")
        if (blurry) Text("This photo looks a little blurry. You can retake it for better reading.")
        error?.let { Text(it) }
        if (busy) CircularProgressIndicator()
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().height(240.dp),
            enabled = !busy,
            label = { Text("Edit the text if something looks wrong") },
        )
        LargeButton("Use Page", onSave, enabled = !busy)
        LargeButton("Retake", onRetake, tonal = true, enabled = !busy)
    }
}
