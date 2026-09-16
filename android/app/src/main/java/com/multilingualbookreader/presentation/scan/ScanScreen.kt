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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.presentation.components.LargeButton
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.darkBrand
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.min

@Composable
fun ScanRoute(
    onOpenReader: (String) -> Unit,
    onImportPdf: () -> Unit,
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
    LaunchedEffect(state.openReaderId) {
        val id = state.openReaderId ?: return@LaunchedEffect
        viewModel.consumeOpenReader()
        onOpenReader(id)
    }
    ScanScreen(
        state = state,
        cameraGranted = granted,
        onCapture = viewModel::onCaptured,
        onPickGallery = ::pickGallery,
        onImportPdf = onImportPdf,
        onRequestCamera = { permission.launch(Manifest.permission.CAMERA) },
        onMode = viewModel::setMode,
        onCycleFlash = viewModel::cycleFlash,
        onToggleCrop = viewModel::toggleAutoCrop,
        onToggleQuality = viewModel::toggleHighQuality,
        onToggleTips = viewModel::toggleTips,
        onTextChange = viewModel::updateText,
        onSave = viewModel::savePage,
        onRetake = viewModel::retake,
        onRead = { state.bookId?.let(onOpenReader) },
        onBack = onBack,
    )
}

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
    onImportPdf: () -> Unit = {},
    onRequestCamera: () -> Unit = {},
    onMode: (ScanCaptureMode) -> Unit = {},
    onCycleFlash: () -> Unit = {},
    onToggleCrop: () -> Unit = {},
    onToggleQuality: () -> Unit = {},
    onToggleTips: () -> Unit = {},
) {
    val brand = LocalBrand.current
    val captureBrand = darkBrand()
    Box(Modifier.fillMaxSize().background(if (state.preview == null) captureBrand.background else brand.background)) {
        if (state.preview == null) {
            CompositionLocalProvider(LocalBrand provides captureBrand) {
                CaptureLayout(
                    state = state,
                    cameraGranted = cameraGranted,
                    onCapture = onCapture,
                    onPickGallery = onPickGallery,
                    onImportPdf = onImportPdf,
                    onRequestCamera = onRequestCamera,
                    onMode = onMode,
                    onCycleFlash = onCycleFlash,
                    onToggleCrop = onToggleCrop,
                    onToggleQuality = onToggleQuality,
                    onToggleTips = onToggleTips,
                    onRead = onRead.takeIf { state.pageCount > 0 },
                    onBack = onBack,
                )
            }
        } else {
            ReviewPane(
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                preview = state.preview,
                text = state.ocrText,
                language = state.language.displayName,
                blurry = state.blurry,
                error = state.error,
                busy = state.busy,
                onTextChange = onTextChange,
                onSave = onSave,
                onRetake = onRetake,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun CaptureLayout(
    state: ScanUiState,
    cameraGranted: Boolean,
    onCapture: (ByteArray) -> Unit,
    onPickGallery: () -> Unit,
    onImportPdf: () -> Unit,
    onRequestCamera: () -> Unit,
    onMode: (ScanCaptureMode) -> Unit,
    onCycleFlash: () -> Unit,
    onToggleCrop: () -> Unit,
    onToggleQuality: () -> Unit,
    onToggleTips: () -> Unit,
    onRead: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    val context = LocalContext.current
    val imageCapture = rememberImageCapture(highQuality = state.highQuality, flash = state.flash)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "Back", tint = brand.textPrimary)
            }
            Text("Scan Book", style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
            Spacer(Modifier.weight(1f))
            if (state.pageCount > 0) {
                Text("Pages ${state.pageCount}", color = brand.textSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
        TipBanner(showDetail = state.showTips, onTips = onToggleTips)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Color.Black),
        ) {
            if (cameraGranted) {
                CameraPreview(imageCapture = imageCapture)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Camera access is needed to scan a page.", color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Allow camera",
                            color = brand.onAccent,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(brand.accent)
                                .clickable(enabled = !state.busy, onClick = onRequestCamera)
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            ScanFrameOverlay(Modifier.fillMaxSize())
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RailButton(
                    icon = when (state.flash) {
                        ScanFlash.OFF -> Icons.Outlined.FlashOff
                        ScanFlash.AUTO -> Icons.Outlined.FlashAuto
                        ScanFlash.ON -> Icons.Outlined.FlashOn
                    },
                    label = when (state.flash) {
                        ScanFlash.OFF -> "Off"
                        ScanFlash.AUTO -> "Auto"
                        ScanFlash.ON -> "On"
                    },
                    selected = state.flash != ScanFlash.OFF,
                    onClick = onCycleFlash,
                )
                RailButton(Icons.Outlined.Crop, "Auto Crop", selected = state.autoCrop, onClick = onToggleCrop)
                RailButton(Icons.Outlined.Hd, "High Quality", selected = state.highQuality, onClick = onToggleQuality)
            }
            Text(
                "Align the page inside the frame",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            if (state.busy) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = brand.accent)
            }
        }
        state.error?.let {
            Text(it, color = brand.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ModeChip("Single Page", Icons.Outlined.Description, state.mode == ScanCaptureMode.SINGLE) {
                onMode(ScanCaptureMode.SINGLE)
            }
            ModeChip("Multiple Pages", Icons.Outlined.ContentCopy, state.mode == ScanCaptureMode.MULTIPLE) {
                onMode(ScanCaptureMode.MULTIPLE)
            }
            ModeChip(
                "Book Mode",
                Icons.AutoMirrored.Outlined.MenuBook,
                state.mode == ScanCaptureMode.BOOK,
            ) { onMode(ScanCaptureMode.BOOK) }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleAction(Icons.Outlined.PhotoLibrary, "Choose from gallery", enabled = !state.busy, onClick = onPickGallery)
            ShutterButton(
                enabled = cameraGranted && !state.busy,
                onClick = { takePicture(context.cacheDir, imageCapture, onCapture) },
            )
            CircleAction(Icons.Outlined.PictureAsPdf, "Import PDF instead", enabled = !state.busy, onClick = onImportPdf)
        }
        if (onRead != null) {
            Spacer(Modifier.height(12.dp))
            LargeButton("Read book", onRead, tonal = true)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TipBanner(showDetail: Boolean, onTips: () -> Unit) {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(brand.elevated)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(brand.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = brand.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Position the page inside the frame", color = brand.textPrimary, style = MaterialTheme.typography.titleSmall)
                Text("Keep the page flat, well lit, and avoid shadows.", color = brand.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(brand.accent.copy(alpha = 0.18f))
                    .clickable(onClick = onTips)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = brand.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tips", color = brand.accent, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (showDetail) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Single Page saves and opens the book. Multiple Pages keeps adding. Book Mode skips the camera crop for an open spread. Telugu needs the reading server.",
                color = brand.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val brand = LocalBrand.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) brand.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) brand.onAccent else brand.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) brand.onAccent else brand.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun RailButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val brand = LocalBrand.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, if (selected) brand.accent else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) brand.accent else Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CircleAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val brand = LocalBrand.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(brand.elevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = brand.textPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = brand.textSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val brand = LocalBrand.current
    Box(
        Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(brand.accent.copy(alpha = if (enabled) 1f else 0.38f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "Capture page" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(70.dp).clip(CircleShape).background(brand.accent.copy(alpha = if (enabled) 1f else 0.38f)))
    }
}

@Composable
private fun rememberImageCapture(highQuality: Boolean, flash: ScanFlash): ImageCapture {
    val capture = remember(highQuality) {
        ImageCapture.Builder()
            .setCaptureMode(
                if (highQuality) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            )
            .build()
    }
    LaunchedEffect(capture, flash) {
        capture.flashMode = when (flash) {
            ScanFlash.OFF -> ImageCapture.FLASH_MODE_OFF
            ScanFlash.AUTO -> ImageCapture.FLASH_MODE_AUTO
            ScanFlash.ON -> ImageCapture.FLASH_MODE_ON
        }
    }
    return capture
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture) {
    val lifecycle = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.tag = imageCapture
                bindCamera(ctx, previewView, lifecycle, imageCapture)
            }
        },
        update = { previewView ->
            if (previewView.tag !== imageCapture) {
                previewView.tag = imageCapture
                bindCamera(previewView.context, previewView, lifecycle, imageCapture)
            }
        },
    )
}

private val captureIo = Executors.newSingleThreadExecutor()

private fun takePicture(cacheDir: File, imageCapture: ImageCapture, onCapture: (ByteArray) -> Unit) {
    val file = File(cacheDir, "capture.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        output,
        captureIo,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onCapture(file.readBytes())
            }
            override fun onError(exception: ImageCaptureException) = Unit
        },
    )
}

private fun bindCamera(
    ctx: android.content.Context,
    previewView: PreviewView,
    lifecycle: androidx.lifecycle.LifecycleOwner,
    imageCapture: ImageCapture,
) {
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
}

@Composable
private fun ScanFrameOverlay(modifier: Modifier) {
    val accent = LocalBrand.current.accent
    Canvas(modifier) {
        val side = min(size.width, size.height) * 0.72f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val corner = side * 0.16f
        val stroke = 6.dp.toPx()
        drawRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(left, top),
            size = Size(side, side),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        fun cornerAt(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(accent, Offset(x, y), Offset(x + dx * corner, y), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(accent, Offset(x, y), Offset(x, y + dy * corner), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        cornerAt(left, top, 1f, 1f)
        cornerAt(left + side, top, -1f, 1f)
        cornerAt(left, top + side, 1f, -1f)
        cornerAt(left + side, top + side, -1f, -1f)
        val cx = left + side / 2f
        val cy = top + side / 2f
        val cross = 16.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - cross, cy), Offset(cx + cross, cy), strokeWidth = 2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.85f), Offset(cx, cy - cross), Offset(cx, cy + cross), strokeWidth = 2.dp.toPx())
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
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.Close, contentDescription = "Back", tint = brand.textPrimary)
            }
            Text("Scan Book", style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
        }
        Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = "Captured page",
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )
        Text("Detected language: $language", color = brand.textPrimary)
        if (blurry) Text("This photo looks a little blurry. You can retake it for better reading.", color = brand.textSecondary)
        error?.let { Text(it, color = brand.danger) }
        if (busy) CircularProgressIndicator(color = brand.accent)
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
