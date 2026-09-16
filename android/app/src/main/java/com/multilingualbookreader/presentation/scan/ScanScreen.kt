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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
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
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris -> viewModel.onGalleryPicked(uris) }
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.onGalleryPicked(uris)
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
        onSelectDraft = viewModel::selectDraft,
        onRotate = viewModel::rotateSelected,
        onCyclePageCrop = viewModel::cycleCropSelected,
        onToggleEnhance = viewModel::toggleEnhanceSelected,
        onRemoveDraft = viewModel::removeSelected,
        onCancelPrepare = viewModel::cancelPrepare,
        onDetect = viewModel::detectSelected,
        onDetectAll = viewModel::detectAll,
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
    onSelectDraft: (Int) -> Unit = {},
    onRotate: () -> Unit = {},
    onCyclePageCrop: () -> Unit = {},
    onToggleEnhance: () -> Unit = {},
    onRemoveDraft: () -> Unit = {},
    onCancelPrepare: () -> Unit = {},
    onDetect: () -> Unit = {},
    onDetectAll: () -> Unit = {},
) {
    val brand = LocalBrand.current
    val captureBrand = darkBrand()
    val captureChrome = state.preview == null
    Box(Modifier.fillMaxSize().background(if (captureChrome) captureBrand.background else brand.background)) {
        CompositionLocalProvider(LocalBrand provides if (captureChrome) captureBrand else brand) {
            when {
                state.preview != null -> ReviewPane(
                    preview = state.preview,
                    text = state.ocrText,
                    language = state.language.displayName,
                    blurry = state.blurry,
                    error = state.error,
                    busy = state.busy,
                    remaining = state.drafts.size,
                    onTextChange = onTextChange,
                    onSave = onSave,
                    onRetake = onRetake,
                    onBack = onBack,
                )
                state.drafts.isNotEmpty() -> PreparePane(
                    state = state,
                    onSelect = onSelectDraft,
                    onRotate = onRotate,
                    onCycleCrop = onCyclePageCrop,
                    onToggleEnhance = onToggleEnhance,
                    onRemove = onRemoveDraft,
                    onAdd = onPickGallery,
                    onDetect = onDetect,
                    onDetectAll = onDetectAll,
                    onBack = onCancelPrepare,
                )
                else -> CaptureLayout(
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
        Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = 2.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Back", tint = brand.textPrimary)
            }
            Text("Scan Book", style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
            Spacer(Modifier.weight(1f))
            if (state.pageCount > 0) {
                Text("Pages ${state.pageCount}", color = brand.textSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
        TipBanner(showDetail = state.showTips, onTips = onToggleTips)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black),
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
            Text(
                "Align the page inside the frame",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            if (state.busy) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = brand.accent)
            }
        }
        state.error?.let {
            Text(it, color = brand.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTool(
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
            BottomTool(Icons.Outlined.Crop, "Auto Crop", selected = state.autoCrop, onClick = onToggleCrop)
            BottomTool(Icons.Outlined.Hd, "High Quality", selected = state.highQuality, onClick = onToggleQuality)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CircleAction(Icons.Outlined.PhotoLibrary, "Choose from gallery", enabled = !state.busy, onClick = onPickGallery)
            ShutterButton(
                enabled = cameraGranted && !state.busy,
                onClick = { takePicture(context.cacheDir, imageCapture, onCapture) },
            )
            CircleAction(Icons.Outlined.PictureAsPdf, "Import PDF instead", enabled = !state.busy, onClick = onImportPdf)
        }
        if (onRead != null) {
            Spacer(Modifier.height(8.dp))
            LargeButton("Read book", onRead, tonal = true)
        }
    }
}

@Composable
private fun PreparePane(
    state: ScanUiState,
    onSelect: (Int) -> Unit,
    onRotate: () -> Unit,
    onCycleCrop: () -> Unit,
    onToggleEnhance: () -> Unit,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    onDetect: () -> Unit,
    onDetectAll: () -> Unit,
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    val selected = state.drafts.getOrNull(state.selectedDraftIndex)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = 2.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Back", tint = brand.textPrimary)
            }
            Text("Prepare pages", style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
            Spacer(Modifier.weight(1f))
            Text("${state.drafts.size} photo${if (state.drafts.size == 1) "" else "s"}", color = brand.textSecondary)
        }
        Text(
            "Rotate until the writing reads left to right. Crop and enhance if you need to, then detect text.",
            color = brand.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.drafts, key = { _, draft -> draft.id }) { index, draft ->
                val selectedPage = index == state.selectedDraftIndex
                Image(
                    bitmap = draft.preview.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, if (selectedPage) brand.accent else brand.border, RoundedCornerShape(14.dp))
                        .clickable { onSelect(index) },
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(brand.elevated),
            contentAlignment = Alignment.Center,
        ) {
            if (selected != null) {
                Image(
                    bitmap = selected.preview.asImageBitmap(),
                    contentDescription = "Selected page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            }
            if (state.busy) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = brand.accent)
                    state.busyMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = brand.textPrimary)
                    }
                }
            }
        }
        state.error?.let {
            Text(it, color = brand.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTool(Icons.AutoMirrored.Outlined.RotateRight, "Rotate", selected = false, onClick = onRotate, enabled = !state.busy)
            BottomTool(
                Icons.Outlined.Crop,
                if (selected != null && selected.cropInset > 0f) "Crop ${ (selected.cropInset * 100).toInt() }%" else "Crop",
                selected = selected?.cropInset?.let { it > 0f } == true,
                onClick = onCycleCrop,
                enabled = !state.busy,
            )
            BottomTool(
                Icons.Outlined.AutoFixHigh,
                "Enhance",
                selected = selected?.enhance == true,
                onClick = onToggleEnhance,
                enabled = !state.busy,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CircleAction(Icons.Outlined.PhotoLibrary, "Add photos", enabled = !state.busy, onClick = onAdd)
            CircleAction(Icons.Outlined.Delete, "Remove page", enabled = !state.busy && state.drafts.isNotEmpty(), onClick = onRemove)
        }
        Spacer(Modifier.height(10.dp))
        LargeButton("Detect text", onDetect, enabled = !state.busy && selected != null)
        if (state.drafts.size > 1) {
            Spacer(Modifier.height(8.dp))
            LargeButton("Detect all pages", onDetectAll, tonal = true, enabled = !state.busy)
        }
    }
}

@Composable
private fun TipBanner(showDetail: Boolean, onTips: () -> Unit) {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brand.elevated)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(brand.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = brand.accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Position the page inside the frame", color = brand.textPrimary, style = MaterialTheme.typography.titleSmall)
                Text("Keep the page flat, well lit, and avoid shadows.", color = brand.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(brand.accent.copy(alpha = 0.18f))
                    .clickable(onClick = onTips)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = brand.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tips", color = brand.accent, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (showDetail) {
            Spacer(Modifier.height(6.dp))
            Text(
                "After you capture or pick photos, rotate them until the lines read left to right, then detect text. Telugu needs the reading server.",
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) brand.onAccent else brand.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
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
private fun BottomTool(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val brand = LocalBrand.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(brand.elevated)
                .border(1.dp, if (selected) brand.accent else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) brand.accent else brand.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = brand.textSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp),
    ) {
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
        val side = min(size.width, size.height) * 0.90f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val corner = side * 0.14f
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
    preview: Bitmap,
    text: String,
    language: String,
    blurry: Boolean,
    error: String?,
    busy: Boolean,
    remaining: Int,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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
        LargeButton(if (remaining > 0) "Adjust photo" else "Retake", onRetake, tonal = true, enabled = !busy)
    }
}
