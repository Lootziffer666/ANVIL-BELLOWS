package com.anvil.bellows.presentation.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.presentation.theme.IigSuccessDark
import com.anvil.bellows.presentation.theme.OxidRedHover
import com.anvil.bellows.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isServerRunning   by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val currentToken      by viewModel.currentToken.collectAsStateWithLifecycle()
    val tokenVisible      by viewModel.tokenVisible.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val context           = LocalContext.current

    var showRotateDialog by remember { mutableStateOf(false) }

    // Collect snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // App version from PackageManager (no BuildConfig needed)
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
    val versionCode = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        }.getOrDefault(1L)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Local API Server ───────────────────────────────────────────────
            item {
                SectionHeader("Local API Server")
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "NanoHTTPD Server",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Port 4141 · OpenAI-kompatible API",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = { viewModel.toggleServer() }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))

                    // Status dot + label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServerRunning) IigSuccessDark
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isServerRunning) "Läuft · http://127.0.0.1:4141"
                            else "Gestoppt",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isServerRunning) IigSuccessDark
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Bearer Token ───────────────────────────────────────────────────
            item {
                SectionHeader("Bearer Token")
            }
            item {
                SettingsCard {
                    Text(
                        "Authentifizierungs-Token für den lokalen API-Server. " +
                                "Nur mit vertrauenswürdigen Clients teilen.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(12.dp))

                    // Token display field
                    val displayToken = if (tokenVisible) currentToken
                    else currentToken.take(8) + "••••••••••••••••"
                    OutlinedTextField(
                        value = displayToken,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                            .copy(fontFamily = FontFamily.Monospace),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleTokenVisibility() }) {
                                Icon(
                                    imageVector = if (tokenVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = "Sichtbarkeit umschalten"
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy
                        OutlinedButton(
                            onClick = {
                                val token = viewModel.currentTokenValue()
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Bearer Token", token)
                                )
                                viewModel.emitCopiedMessage()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Kopieren")
                        }

                        // Rotate
                        Button(
                            onClick = { showRotateDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rotieren")
                        }
                    }
                }
            }

            // ── App Info ───────────────────────────────────────────────────────
            item {
                SectionHeader("App-Info")
            }
            item {
                SettingsCard {
                    InfoRow("App", "Anvil Bellows")
                    RowDivider()
                    InfoRow("Version", "$versionName (Build $versionCode)")
                    RowDivider()
                    InfoRow("Paketname", "com.anvil.bellows")
                    RowDivider()
                    InfoRow("Design-System", "Ink & Iron Glow")
                    RowDivider()
                    InfoRow("API-Port", "4141")
                }
            }
        }
    }

    // ── Rotate token confirmation dialog ───────────────────────────────────────
    if (showRotateDialog) {
        AlertDialog(
            onDismissRequest = { showRotateDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = OxidRedHover
                )
            },
            title = { Text("Token rotieren?") },
            text = {
                Text(
                    "Alle laufenden Clients verlieren ihre Session und müssen sich " +
                            "mit dem neuen Token neu authentifizieren. " +
                            "Das kann nicht rückgängig gemacht werden."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.rotateToken()
                        showRotateDialog = false
                    }
                ) {
                    Text("Rotieren", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRotateDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// ── Private helper composables ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
}
