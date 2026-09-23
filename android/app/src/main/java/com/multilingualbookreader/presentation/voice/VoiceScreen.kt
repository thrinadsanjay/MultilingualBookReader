package com.multilingualbookreader.presentation.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.FilterChipItem
import com.multilingualbookreader.presentation.components.PrimaryButton
import com.multilingualbookreader.presentation.components.ScreenHeader
import com.multilingualbookreader.presentation.components.SecondaryButton
import com.multilingualbookreader.presentation.reader.defaultStandardVoice
import com.multilingualbookreader.presentation.theme.LocalBrand
import com.multilingualbookreader.presentation.theme.LocalDimens
import kotlinx.coroutines.delay

@Composable
fun VoiceRoute(
    onBack: () -> Unit,
    onTest: () -> Unit,
    showBack: Boolean = true,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    VoiceScreen(
        state = state,
        profiles = profiles,
        micGranted = granted,
        onRequestMic = { permission.launch(Manifest.permission.RECORD_AUDIO) },
        onName = viewModel::setName,
        onConsent = viewModel::setConsent,
        onStart = viewModel::startRecording,
        onStop = viewModel::stopRecording,
        onPause = viewModel::pauseRecording,
        onResume = viewModel::resumeRecording,
        onRetake = viewModel::retakeLast,
        onPlayLast = viewModel::playLastSample,
        onCreate = viewModel::create,
        onDelete = viewModel::delete,
        onSelect = viewModel::select,
        onTest = onTest,
        onBack = onBack,
        showBack = showBack,
    )
}

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    profiles: List<VoiceProfile>,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onName: (String) -> Unit,
    onConsent: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onRetake: () -> Unit = {},
    onPlayLast: () -> Unit = {},
    onCreate: () -> Unit,
    onDelete: (String) -> Unit,
    onSelect: (VoiceProfile) -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = true,
) {
    val brand = LocalBrand.current
    val dimens = LocalDimens.current
    var showHelp by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.recording, state.paused) {
        if (state.recording && !state.paused) {
            while (true) {
                delay(1000)
                seconds += 1
            }
        } else if (!state.recording) {
            seconds = 0
        }
    }
    val step = when {
        profiles.any { it.status.name == "READY" } -> 4
        state.samples >= 3 -> 3
        state.samples > 0 -> 2
        else -> 1
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = dimens.screen, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brand.textPrimary) }
                }
                Text("My Voice", style = MaterialTheme.typography.displaySmall, color = brand.textPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { showHelp = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "Voice help", tint = brand.textPrimary)
                }
            }
            BookReaderCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Create your reading voice", style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Clone your own voice to hear books in your speech.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = brand.textSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Record in a quiet room. Speak naturally and avoid music and clipping.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.textSecondary,
                        )
                    }
                    Waveform(active = state.recording && !state.paused, modifier = Modifier.width(56.dp).height(96.dp))
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = state.name, onValueChange = onName, label = { Text("Voice name") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = state.consent, onCheckedChange = onConsent)
                    Text(stringResource(R.string.voice_consent), style = MaterialTheme.typography.bodySmall, color = brand.textPrimary)
                }
            }
            BookReaderCard {
                Text("Samples recorded", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${state.samples}", style = MaterialTheme.typography.displaySmall, color = brand.textPrimary)
                    SampleMeter(recorded = state.samples, modifier = Modifier.weight(1f).height(28.dp))
                }
                if (state.recording) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (state.paused) "Paused ${formatTime(seconds)}" else "Recording ${formatTime(seconds)}",
                        color = brand.danger,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val steps = listOf(
                        "Record samples" to Icons.Outlined.Mic,
                        "Review samples" to Icons.Outlined.Headphones,
                        "Create voice" to Icons.Outlined.AutoAwesome,
                        "Ready to use" to Icons.Outlined.VerifiedUser,
                    )
                    steps.forEachIndexed { index, (label, icon) ->
                        val active = step >= index + 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (active) brand.accent.copy(alpha = 0.18f) else brand.surfaceSecondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    icon,
                                    null,
                                    tint = if (active) brand.accent else brand.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) brand.textPrimary else brand.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            when {
                !micGranted -> PrimaryButton("Allow microphone", onRequestMic)
                state.recording && state.paused -> {
                    PrimaryButton("Resume", onResume)
                    SecondaryButton("Stop", onStop)
                }
                state.recording -> {
                    PrimaryButton("Stop", onStop)
                    SecondaryButton("Pause", onPause)
                }
                else -> PrimaryButton("Start recording samples", onStart, icon = Icons.Outlined.Mic)
            }
            if (state.samples > 0 && !state.recording) {
                SecondaryButton("Play last sample", onPlayLast)
                SecondaryButton("Retake last sample", onRetake)
            }
            PrimaryButton(
                if (state.creating) "Creating voice…" else "Create voice",
                onCreate,
                enabled = !state.creating && !state.recording,
            )
            if (!state.consent || state.samples == 0) {
                Text(
                    "Tick the consent box and record at least one sample, then tap Create voice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textSecondary,
                )
            }
            SecondaryButton("Compare voices", onTest)
            state.message?.let { Text(it, color = brand.textSecondary) }
            state.error?.let { Text(it, color = brand.danger) }
            profiles.forEach { profile ->
                BookReaderCard {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge, color = brand.textPrimary)
                    Text("Status: ${profile.status}", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                    val supported = profile.supportedLanguages.joinToString { it.displayName }
                    Text(
                        if (supported.isBlank()) "Supported languages: not verified yet" else "Supported: $supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = brand.textSecondary,
                    )
                    if (profile.experimentalLanguages.isNotEmpty()) {
                        Text("Experimental: ${profile.experimentalLanguages.joinToString { it.displayName }}", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
                    }
                    profile.qualityNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = brand.textSecondary) }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Use this voice", { onSelect(profile) })
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Delete voice", { onDelete(profile.id) })
                }
            }
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Voice cloning") },
            text = { Text("Only clone a voice you are authorized to use. Test English, Hindi, and Telugu before relying on a custom voice. The app never claims a language is supported unless the provider lists it.") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val bars = remember { listOf(0.30f, 0.62f, 0.42f, 0.86f, 0.54f, 0.95f, 0.48f, 0.72f, 0.36f) }
    val accent = brand.accent
    val teal = brand.teal
    Canvas(modifier) {
        val slot = size.width / bars.size
        val barWidth = slot * 0.52f
        bars.forEachIndexed { index, raw ->
            val fraction = if (active) raw else 0.22f + raw * 0.22f
            val h = size.height * fraction
            val x = index * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (index % 2 == 0) accent else teal,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun SampleMeter(recorded: Int, modifier: Modifier = Modifier) {
    val brand = LocalBrand.current
    val slots = 12
    val accent = brand.accent
    val idle = brand.border
    Canvas(modifier) {
        val slot = size.width / slots
        val barWidth = slot * 0.45f
        repeat(slots) { index ->
            val filled = index < recorded
            val h = if (filled) size.height else size.height * 0.35f
            val x = index * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (filled) accent else idle,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
fun VoiceTestRoute(
    onBack: () -> Unit,
    viewModel: VoiceQualityTestViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val english by viewModel.english.collectAsStateWithLifecycle()
    val hindi by viewModel.hindi.collectAsStateWithLifecycle()
    val telugu by viewModel.telugu.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    VoiceTestScreen(
        message = ui.message,
        english = english,
        hindi = hindi,
        telugu = telugu,
        voices = listOf(defaultStandardVoice()) + profiles,
        selectedVoiceId = ui.selectedVoiceId,
        playingLanguage = ui.playingLanguage,
        onEnglish = { viewModel.english.value = it },
        onHindi = { viewModel.hindi.value = it },
        onTelugu = { viewModel.telugu.value = it },
        onSelectVoice = viewModel::selectVoice,
        onPlay = { text, language -> viewModel.play(text, language) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTestScreen(
    message: String,
    english: String,
    hindi: String,
    telugu: String,
    onEnglish: (String) -> Unit,
    onHindi: (String) -> Unit,
    onTelugu: (String) -> Unit,
    onPlay: (String, SupportedLanguage) -> Unit,
    onBack: () -> Unit,
    voices: List<VoiceProfile> = emptyList(),
    selectedVoiceId: String = defaultStandardVoice().id,
    playingLanguage: SupportedLanguage? = null,
    onSelectVoice: (String) -> Unit = {},
) {
    val brand = LocalBrand.current
    Scaffold(
        containerColor = brand.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Voice test") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("Listen before you choose", "Do not lock in a custom voice until Telugu, Hindi, and English all sound acceptable.")
            Text(message, color = brand.textSecondary)
            if (voices.isNotEmpty()) {
                Text("Voice", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    voices.forEach { voice ->
                        FilterChipItem(voice.name, selectedVoiceId == voice.id) { onSelectVoice(voice.id) }
                    }
                }
            }
            OutlinedTextField(english, onEnglish, label = { Text("English") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                if (playingLanguage == SupportedLanguage.ENGLISH) "Playing English…" else "Play English",
                onClick = { onPlay(english, SupportedLanguage.ENGLISH) },
            )
            OutlinedTextField(hindi, onHindi, label = { Text("Hindi") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                if (playingLanguage == SupportedLanguage.HINDI) "Playing Hindi…" else "Play Hindi",
                onClick = { onPlay(hindi, SupportedLanguage.HINDI) },
            )
            OutlinedTextField(telugu, onTelugu, label = { Text("Telugu") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(
                if (playingLanguage == SupportedLanguage.TELUGU) "Playing Telugu…" else "Play Telugu",
                onClick = { onPlay(telugu, SupportedLanguage.TELUGU) },
            )
        }
    }
}
