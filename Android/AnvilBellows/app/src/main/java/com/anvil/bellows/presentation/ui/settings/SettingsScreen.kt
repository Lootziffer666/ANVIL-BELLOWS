package com.anvil.bellows.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anvil.bellows.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionTitle("Appearance") }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Dark Mode", modifier = Modifier.weight(1f))
                            Switch(state.darkMode, { viewModel.setDarkMode(it) })
                        }
                    }
                }
            }

            item { SectionTitle("Google Vertex AI (\$250 Credit)") }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Option A: AI Studio API Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        ApiKeyField(
                            label = "Google AI Studio API Key",
                            value = state.vertexAiStudioKey,
                            onValueChange = { viewModel.setVertexAiStudioKey(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Option B: Service Account JSON (Vertex AI proper)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        OutlinedTextField(
                            value = state.serviceAccountJson,
                            onValueChange = { viewModel.setServiceAccountJson(it) },
                            label = { Text("Paste Service Account JSON") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("{ \"type\": \"service_account\", ... }") }
                        )
                        OutlinedTextField(
                            value = state.vertexProjectId,
                            onValueChange = { viewModel.setVertexProjectId(it) },
                            label = { Text("GCP Project ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.vertexLocation,
                            onValueChange = { viewModel.setVertexLocation(it) },
                            label = { Text("Location (e.g. us-central1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(onClick = { viewModel.saveVertexSettings() }, modifier = Modifier.align(Alignment.End)) {
                            Text("Save Vertex Settings")
                        }
                    }
                }
            }

            item { SectionTitle("Wiki Export (memoriki)") }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.wikiEndpoint,
                            onValueChange = { viewModel.setWikiEndpoint(it) },
                            label = { Text("Wiki Endpoint URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("https://your-wiki.example.com/api/pages") }
                        )
                        ApiKeyField(
                            label = "Wiki Bearer Token",
                            value = state.wikiToken,
                            onValueChange = { viewModel.setWikiToken(it) }
                        )
                        Button(onClick = { viewModel.saveWikiSettings() }, modifier = Modifier.align(Alignment.End)) {
                            Text("Save")
                        }
                    }
                }
            }

            item { SectionTitle("Rate Limit Buffer") }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Switch provider at ${state.rpmBuffer} req before RPM limit", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = state.rpmBuffer.toFloat(),
                            onValueChange = { viewModel.setRpmBuffer(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }
            }

            item { SectionTitle("ANVIL API (Local Proxy)") }
            item {
                val clipboardManager = LocalClipboardManager.current
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Expose an OpenAI-compatible endpoint on this device. Use it in Cursor, Continue, or any LLM client — all providers are wrapped automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (state.isApiServerRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                                null,
                                tint = if (state.isApiServerRunning) MaterialTheme.colorScheme.secondary
                                       else MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Enable API Server (port 4141)", modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.isApiServerRunning,
                                onCheckedChange = { on ->
                                    if (on) viewModel.startApiServer() else viewModel.stopApiServer()
                                }
                            )
                        }
                        if (state.isApiServerRunning && state.localIpAddress.isNotEmpty()) {
                            val endpoint = "http://${state.localIpAddress}:4141"
                            OutlinedTextField(
                                value = endpoint,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Base URL — paste into your tool") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(endpoint)) }) {
                                        Icon(Icons.Default.ContentCopy, "Copy")
                                    }
                                }
                            )
                            Text(
                                "API key: any non-empty string\nModel: any string (Anvil Bellows routes automatically)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            item { SectionTitle("About") }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Anvil Bellows v1.0", fontWeight = FontWeight.Bold)
                        Text("Unified LLM Router with automatic handoff", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(4.dp))
                        Text("21 providers • Skill Rotation • ONNX Memory • Wiki Export • Local Proxy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    var showKey by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showKey = !showKey }) {
                Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}
