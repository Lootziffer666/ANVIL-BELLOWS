package com.anvil.bellows.presentation.ui.providers

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.presentation.theme.*
import com.anvil.bellows.presentation.viewmodel.ProvidersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    paddingValues: PaddingValues,
    viewModel: ProvidersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Providers", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Provider hinzufügen") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val byok = state.providers.filter { it.entity.isByok }
            val free = state.providers.filter { !it.entity.isByok }

            if (free.isNotEmpty()) {
                item { SectionHeader("Free Providers") }
                items(free, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }

            if (byok.isNotEmpty()) {
                item { SectionHeader("BYOK (Paid)") }
                items(byok, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddProviderDialog(
            onConfirm = { name, url, key, rpm, rpd, ctx, out, model, tier, isByok ->
                viewModel.addCustomProvider(name, url, key, rpm, rpd, ctx, out, model, tier, isByok)
                viewModel.hideAddDialog()
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ProviderCard(
    pw: ProvidersViewModel.ProviderWithUsage,
    viewModel: ProvidersViewModel
) {
    var showKeyInput by remember { mutableStateOf(false) }
    var apiKeyText by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    val entity = pw.entity
    val uriHandler = LocalUriHandler.current

    // For BYOK providers without a key, visually communicate they're not usable yet
    val needsKey = entity.isByok && !pw.hasApiKey

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (needsKey && entity.enabled)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: name + chips + toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entity.name, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        TierChip(entity.tier)
                        if (entity.isByok) {
                            Spacer(Modifier.width(4.dp))
                            ByokChip()
                        }
                        if (pw.hasApiKey) {
                            Spacer(Modifier.width(4.dp))
                            KeySetChip()
                        }
                    }
                    // Clickable base URL
                    TextButton(
                        onClick = {
                            if (entity.baseUrl.startsWith("http")) uriHandler.openUri(entity.baseUrl)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            entity.baseUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = entity.enabled,
                    onCheckedChange = { viewModel.toggleProvider(entity.id, it) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Rate limit gauges
            if (entity.rpmLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPM", pw.rpmUsed, entity.rpmLimit)
                Spacer(Modifier.height(4.dp))
            }
            if (entity.rpdLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPD", pw.rpdUsed, entity.rpdLimit)
                Spacer(Modifier.height(4.dp))
            }

            // Key row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (pw.hasApiKey) Icons.Default.Key else Icons.Default.KeyOff,
                    contentDescription = null,
                    tint = if (pw.hasApiKey) BabuTeal else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (pw.hasApiKey) "API-Key hinterlegt" else "Kein API-Key",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pw.hasApiKey) BabuTeal else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.weight(1f))

                // "Get Key" button: show when URL is known
                if (entity.registrationUrl.isNotEmpty()) {
                    TextButton(onClick = { uriHandler.openUri(entity.registrationUrl) }) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Key holen", style = MaterialTheme.typography.labelSmall)
                    }
                }
                TextButton(onClick = { showKeyInput = !showKeyInput }) {
                    Text(if (showKeyInput) "Abbrechen" else if (pw.hasApiKey) "Ändern" else "Key setzen")
                }
            }

            // Key input field
            if (showKeyInput) {
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (pw.hasApiKey) {
                        OutlinedButton(
                            onClick = {
                                viewModel.removeApiKey(entity.id)
                                showKeyInput = false
                                apiKeyText = ""
                            }
                        ) { Text("Entfernen") }
                    }
                    Button(
                        onClick = {
                            viewModel.saveApiKey(entity.id, apiKeyText)
                            showKeyInput = false
                            apiKeyText = ""
                        },
                        enabled = apiKeyText.isNotBlank()
                    ) { Text("Speichern") }
                }
            }
        }
    }
}

@Composable
private fun TierChip(tier: Int) {
    Surface(
        color = tierColor(tier).copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            "T$tier",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tierColor(tier),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ByokChip() {
    Surface(color = ByokColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Text(
            "BYOK",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = ByokColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KeySetChip() {
    Surface(color = BabuTeal.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, null, tint = BabuTeal, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                "Key",
                style = MaterialTheme.typography.labelSmall,
                color = BabuTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RateLimitGauge(label: String, used: Int, limit: Int) {
    val fraction = if (limit > 0 && limit < Int.MAX_VALUE) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gauge_$label"
    )
    val color = when {
        fraction < 0.6f -> BabuTeal
        fraction < 0.85f -> AccentAmber
        else -> RauschRed
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                if (limit < Int.MAX_VALUE) "$used / $limit" else "$used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun AddProviderDialog(
    onConfirm: (String, String, String, Int, Int, Int, Int, String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://") }
    var apiKey by remember { mutableStateOf("") }
    var rpmLimit by remember { mutableStateOf("60") }
    var rpdLimit by remember { mutableStateOf("") }
    var contextWindow by remember { mutableStateOf("128000") }
    var maxOutput by remember { mutableStateOf("4096") }
    var modelId by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf(3) }
    var isByok by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Provider hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name*") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL*") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(modelId, { modelId = it }, label = { Text("Model ID*") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    apiKey, { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(rpmLimit, { rpmLimit = it }, label = { Text("RPM") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(rpdLimit, { rpdLimit = it }, label = { Text("RPD") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), placeholder = { Text("∞") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(contextWindow, { contextWindow = it }, label = { Text("Context") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(maxOutput, { maxOutput = it }, label = { Text("Max Out") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BYOK (paid)")
                    Spacer(Modifier.weight(1f))
                    Switch(isByok, { isByok = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name, baseUrl, apiKey,
                        rpmLimit.toIntOrNull() ?: 60,
                        rpdLimit.toIntOrNull() ?: Int.MAX_VALUE,
                        contextWindow.toIntOrNull() ?: 128_000,
                        maxOutput.toIntOrNull() ?: 4_096,
                        modelId, tier, isByok
                    )
                },
                enabled = name.isNotBlank() && baseUrl.length > 10 && modelId.isNotBlank()
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Abbrechen") } }
    )
}
