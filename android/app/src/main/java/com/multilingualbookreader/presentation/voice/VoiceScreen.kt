package com.multilingualbookreader.presentation.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.R
import com.multilingualbookreader.domain.model.SupportedLanguage
import com.multilingualbookreader.domain.model.VoiceProfile
import com.multilingualbookreader.presentation.components.LargeButton
import com.multilingualbookreader.presentation.components.ScreenHeader
import com.multilingualbookreader.presentation.reader.defaultStandardVoice

@Composable
fun VoiceRoute(
    onBack: () -> Unit,
    onTest: () -> Unit,
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
        onCreate = viewModel::create,
        onDelete = viewModel::delete,
        onSelect = viewModel::select,
        onTest = onTest,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onCreate: () -> Unit,
    onDelete: (String) -> Unit,
    onSelect: (VoiceProfile) -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Voice") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("Create your reading voice", "Only clone a voice you are authorized to use. Never impersonate someone without consent.")
            Text("Record in a quiet room. Hold the phone 15–20 cm away. Speak naturally. Avoid music and clipping. Record several samples.")
            OutlinedTextField(value = state.name, onValueChange = onName, label = { Text("Voice name") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.consent, onCheckedChange = onConsent)
                Text(stringResource(R.string.voice_consent), modifier = Modifier.padding(start = 8.dp))
            }
            Text("Samples recorded: ${state.samples}")
            if (!micGranted) {
                LargeButton("Allow microphone", onRequestMic)
            } else if (state.recording) {
                LargeButton("Stop recording", onStop)
            } else {
                LargeButton("Record sample", onStart)
            }
            LargeButton("Create voice", onCreate, enabled = state.consent && state.samples >= 3)
            state.message?.let { Text(it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LargeButton("Compare voices", onTest, tonal = true)
            profiles.forEach { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge)
                        Text("Status: ${profile.status}")
                        Text("Supported: ${profile.supportedLanguages.joinToString { it.displayName }.ifBlank { "Not verified yet" }}")
                        if (profile.experimentalLanguages.isNotEmpty()) {
                            Text("Experimental: ${profile.experimentalLanguages.joinToString { it.displayName }}")
                        }
                        profile.qualityNote?.let { Text(it) }
                        LargeButton("Use this voice", { onSelect(profile) })
                        LargeButton("Delete voice", { onDelete(profile.id) }, tonal = true)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceTestRoute(
    onBack: () -> Unit,
    viewModel: VoiceQualityTestViewModel = hiltViewModel(),
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val english by viewModel.english.collectAsStateWithLifecycle()
    val hindi by viewModel.hindi.collectAsStateWithLifecycle()
    val telugu by viewModel.telugu.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    VoiceTestScreen(
        message = message,
        english = english,
        hindi = hindi,
        telugu = telugu,
        onEnglish = { viewModel.english.value = it },
        onHindi = { viewModel.hindi.value = it },
        onTelugu = { viewModel.telugu.value = it },
        onPlay = { text, language -> viewModel.play(text, language, profiles.firstOrNull() ?: defaultStandardVoice()) },
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice test") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("Listen before you choose", "Do not lock in a custom voice until Telugu, Hindi, and English all sound acceptable.")
            Text(message)
            OutlinedTextField(english, onEnglish, label = { Text("English") }, modifier = Modifier.fillMaxWidth())
            LargeButton("Play English", onClick = { onPlay(english, SupportedLanguage.ENGLISH) })
            OutlinedTextField(hindi, onHindi, label = { Text("Hindi") }, modifier = Modifier.fillMaxWidth())
            LargeButton("Play Hindi", onClick = { onPlay(hindi, SupportedLanguage.HINDI) })
            OutlinedTextField(telugu, onTelugu, label = { Text("Telugu") }, modifier = Modifier.fillMaxWidth())
            LargeButton("Play Telugu", onClick = { onPlay(telugu, SupportedLanguage.TELUGU) })
        }
    }
}
