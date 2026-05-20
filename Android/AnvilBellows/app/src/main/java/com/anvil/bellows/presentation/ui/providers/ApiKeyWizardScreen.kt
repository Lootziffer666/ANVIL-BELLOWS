package com.anvil.bellows.presentation.ui.providers

import android.content.ClipboardManager
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.presentation.viewmodel.ApiKeyWizardViewModel

/**
 * Step-through wizard that opens each provider's API key console in a
 * Custom Tab, waits for the user to copy their key, detects it on
 * [Lifecycle.Event.ON_RESUME] via clipboard pattern matching, and
 * auto-saves before advancing to the next provider.
 *
 * INVARIANT: Only providers with noAuth=false are shown. Vertex SA JSON
 * providers show a paste area instead of a clipboard auto-detect flow.
 *
 * Note on Android 12+ clipboard access: reading the clipboard is only
 * permitted while the app is in the foreground.  ON_RESUME fires exactly
 * at that moment, making it the correct and reliable hook.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyWizardScreen(
    onBack: () -> Unit,
    viewModel: ApiKeyWizardViewModel = hiltViewModel()
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state         by viewModel.uiState.collectAsState()

    val currentProvider = state.providers.getOrNull(state.currentIndex)

    // ── Clipboard auto-detect on resume ───────────────────────────────────────
    DisposableEffect(lifecycleOwner, state.currentIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentProvider != null) {
                val clip = readClipboard(context)
                if (clip != null && currentProvider.clipboardPattern.isNotEmpty()) {
                    runCatching {
                        val regex = Regex(currentProvider.clipboardPattern)
                        val match = regex.find(clip)?.value
                        if (!match.isNullOrEmpty()) {
                            viewModel.onKeyDetected(match)
                        }
                    }
                } else if (clip != null && currentProvider.authType == "VERTEX" &&
                    clip.trimStart().startsWith("{")) {
                    // SA JSON: just populate the paste field
                    viewModel.onKeyDetected(clip)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                title = { Text("API Key Wizard") }
            )
        }
    ) { innerPadding ->

        if (state.providers.isEmpty()) {
            // All providers already have keys
            WizardDoneContent(onBack)
            return@Scaffold
        }

        if (state.finished) {
            WizardDoneContent(onBack)
            return@Scaffold
        }

        currentProvider ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Progress ──────────────────────────────────────────────────────
            val progress = (state.currentIndex).toFloat() / state.providers.size.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${state.currentIndex + 1} / ${state.providers.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(Modifier.height(8.dp))

            // ── Provider name + tier ──────────────────────────────────────────
            Text(
                currentProvider.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                currentProvider.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider()

            if (currentProvider.authType == "VERTEX") {
                // ── Vertex SA JSON flow ───────────────────────────────────────
                Text(
                    "Öffne die GCP Console, erstelle ein Service Account und lade die JSON-Datei herunter. " +
                    "Kopiere den gesamten JSON-Inhalt und füge ihn hier ein.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        openCustomTab(context, currentProvider.consoleUrl.ifEmpty {
                            currentProvider.registrationUrl
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("GCP Console öffnen")
                }
                OutlinedTextField(
                    value = state.detectedKey,
                    onValueChange = { viewModel.onKeyDetected(it) },
                    label = { Text("Service Account JSON") },
                    placeholder = { Text("{ \"type\": \"service_account\", … }") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                    maxLines = 10
                )
            } else {
                // ── Standard API key flow ─────────────────────────────────────
                Text(
                    "Tippe auf „Console öffnen", melde dich an und kopiere deinen API-Key. " +
                    "Bellows erkennt den Key automatisch und speichert ihn, sobald du zurückkehrst.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        val url = currentProvider.consoleUrl.ifEmpty { currentProvider.registrationUrl }
                        openCustomTab(context, url)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Console öffnen – Key kopieren")
                }

                // Auto-detect indicator
                if (state.detectedKey.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Key erkannt: ${state.detectedKey.take(12)}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Manual fallback
                var showManual by remember(state.currentIndex) { mutableStateOf(false) }
                var showKeyText by remember { mutableStateOf(false) }

                TextButton(onClick = { showManual = !showManual }) {
                    Text(if (showManual) "Manuell ausblenden" else "Key manuell eingeben")
                }
                if (showManual) {
                    OutlinedTextField(
                        value = state.detectedKey,
                        onValueChange = { viewModel.onKeyDetected(it) },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKeyText) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKeyText = !showKeyText }) {
                                Icon(
                                    if (showKeyText) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility, null
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Action row ────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.skipCurrent() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Überspringen")
                }
                Button(
                    onClick = { viewModel.saveCurrentAndAdvance() },
                    enabled = state.detectedKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.currentIndex < state.providers.size - 1) "Speichern & Weiter" else "Fertig")
                }
            }
        }
    }
}

@Composable
private fun WizardDoneContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("Alle Keys gesetzt!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Du kannst jederzeit über den ⊕-Button weitere Provider hinzufügen " +
            "oder Keys direkt auf der Provider-Karte ändern.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Zurück") }
    }
}

private fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    return cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
}

private fun openCustomTab(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    }
}
