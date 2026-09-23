@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.multilingualbookreader.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multilingualbookreader.presentation.components.BookReaderCard
import com.multilingualbookreader.presentation.components.PrimaryButton
import com.multilingualbookreader.presentation.components.ReaderTopBar
import com.multilingualbookreader.presentation.components.SecondaryButton
import com.multilingualbookreader.presentation.theme.LocalBrand

@Composable
fun ServerRoute(onBack: () -> Unit, viewModel: ServerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ServerScreen(
        state = state,
        onUrl = viewModel::setUrl,
        onApiKey = viewModel::setApiKey,
        onSave = viewModel::save,
        onTest = viewModel::test,
        onBack = onBack,
    )
}

@Composable
fun ServerScreen(
    state: ServerUiState,
    onUrl: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
) {
    val brand = LocalBrand.current
    Scaffold(
        containerColor = brand.background,
        topBar = { ReaderTopBar(title = "Reading server", onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookReaderCard {
                Text("Reading server", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.usesPackedServer) {
                        "This build already talks to your reading server. Leave the fields empty unless you want to point at a different host."
                    } else {
                        "English, Hindi, and Telugu are read on this phone. A server is optional: run the Docker backend from this project, then paste that HTTPS address here. Leave this empty to stay fully on-device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand.textSecondary,
                )
            }

            OutlinedTextField(
                value = state.url,
                onValueChange = onUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Server address") },
                placeholder = { Text("https://books.example.org") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = onApiKey,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API key") },
                placeholder = { Text("Optional, set API_KEY on the server") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            state.insecureWarning?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = brand.danger)
            }

            PrimaryButton(if (state.testing) "Testing…" else "Test connection", onTest, enabled = !state.testing)
            SecondaryButton("Save", onSave)

            if (state.saved && state.result == null) {
                Text("Saved.", style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
            }
            state.result?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.ok) brand.teal else brand.danger,
                )
            }
        }
    }
}
