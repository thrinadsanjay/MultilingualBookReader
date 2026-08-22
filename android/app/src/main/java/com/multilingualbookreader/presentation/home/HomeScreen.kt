package com.multilingualbookreader.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.BrandLockup
import com.multilingualbookreader.presentation.components.PillButton
import com.multilingualbookreader.presentation.components.ReaderProgressBar
import com.multilingualbookreader.presentation.components.SectionHeader
import com.multilingualbookreader.presentation.components.StatusPill
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens
import java.util.Calendar

@Composable
fun HomeRoute(
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onLibrary: () -> Unit,
    onVoice: () -> Unit,
    onContinue: (String) -> Unit,
    onUpdates: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onScan = onScan,
        onImportPdf = onImportPdf,
        onLibrary = onLibrary,
        onVoice = onVoice,
        onContinue = onContinue,
        onUpdates = onUpdates,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onScan: () -> Unit,
    onImportPdf: () -> Unit,
    onLibrary: () -> Unit,
    onVoice: () -> Unit,
    onContinue: (String) -> Unit,
    onUpdates: () -> Unit = {},
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screen)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.gap),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandLockup()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUpdates) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = "What's new", tint = brand.textPrimary)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(greeting(), style = MaterialTheme.typography.displaySmall, color = brand.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("Open a book and listen.", style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
                }
                StatusPill(state.online)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroCard(
                    title = stringResource(R.string.scan_book),
                    subtitle = "Use camera to scan book pages",
                    icon = Icons.Outlined.DocumentScanner,
                    container = brand.scanSurface,
                    content = brand.onScanSurface,
                    iconTint = brand.scanIcon,
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                )
                HeroCard(
                    title = stringResource(R.string.import_pdf),
                    subtitle = "Import books from your files",
                    icon = Icons.Outlined.PictureAsPdf,
                    container = brand.importSurface,
                    content = brand.onImportSurface,
                    iconTint = brand.importIcon,
                    onClick = onImportPdf,
                    modifier = Modifier.weight(1f),
                    bordered = true,
                )
            }

            SectionHeader("Continue Listening", "View all", onLibrary, brand.accent)
            state.continueReading?.let { item ->
                val page = (item.progressPercent * item.book.totalPages / 100).coerceAtLeast(1)
                BookReaderCard(onClick = { onContinue(item.book.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BookCover()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.book.title, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                            Text(
                                "Page $page of ${item.book.totalPages} · ${item.progressPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = brand.textSecondary,
                            )
                            ReaderProgressBar(item.progressPercent)
                        }
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(brand.accent)
                                .clickable { onContinue(item.book.id) }
                                .semantics { contentDescription = "Play" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = brand.onAccent, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            } ?: BookReaderCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(brand.surfaceSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = brand.accent, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("No books in progress", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                        Text(
                            "Scan a page or import a PDF to start listening.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.textSecondary,
                        )
                    }
                }
            }

            SectionHeader("My Voice", if (state.voice != null) "Manage Voice" else null, if (state.voice != null) onVoice else null, brand.accent)
            BookReaderCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(brand.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.GraphicEq, null, tint = brand.accent, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.voice?.name ?: "No voice yet", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                        Text(
                            if (state.voice == null) {
                                "Create your voice to hear books in your speech."
                            } else {
                                buildString {
                                    append(state.voice.status.name.lowercase().replaceFirstChar { it.titlecase() })
                                    val langs = state.voice.supportedLanguages.joinToString { it.displayName }
                                    if (langs.isNotBlank()) append(" · ").append(langs)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.textSecondary,
                        )
                    }
                    if (state.voice == null) {
                        PillButton("Create Voice", onVoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCover() {
    val brand = LocalBrand.current
    Box(
        Modifier
            .size(width = 52.dp, height = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(brand.accent, brand.teal))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    val shape = RoundedCornerShape(dimens.cardRadius)
    Column(
        modifier
            .height(dimens.heroCardHeight)
            .clip(shape)
            .background(container)
            .then(if (bordered) Modifier.border(1.dp, brand.border, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = content, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = content.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(brand.background.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = content, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning! 👋"
        in 12..16 -> "Good afternoon! 👋"
        else -> "Good evening! 👋"
    }
}
