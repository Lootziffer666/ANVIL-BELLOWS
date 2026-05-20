package com.anvil.bellows.presentation.ui.providers

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.presentation.theme.*
import com.anvil.bellows.presentation.viewmodel.ProvidersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    paddingValues: PaddingValues,
    onNavigateToWizard: () -> Unit,
    viewModel: ProvidersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Providers", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToWizard) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Provider hinzufügen / Wizard",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        // ── Group providers by section ────────────────────────────────────────
        val freeTier   = state.providers.filter { !it.entity.isByok && !it.entity.noAuth && !it.entity.isCustom }
        val noAuth     = state.providers.filter { it.entity.noAuth && !it.entity.isCustom }
        val byok       = state.providers.filter { it.entity.isByok && !it.entity.isCustom }
        val custom     = state.providers.filter { it.entity.isCustom }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (freeTier.isNotEmpty()) {
                item { SectionHeader("⚡ Free Tier") }
                items(freeTier, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }
            if (noAuth.isNotEmpty()) {
                item { SectionHeader("🆓 Ohne API-Key") }
                items(noAuth, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }
            if (byok.isNotEmpty()) {
                item { SectionHeader("💳 BYOK (Paid)") }
                items(byok, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }
            if (custom.isNotEmpty()) {
                item { SectionHeader("⚙️ Eigene Provider") }
                items(custom, key = { it.entity.id }) { pw ->
                    ProviderCard(pw, viewModel)
                }
            }
        }
    }

    // ── Add custom provider dialog (for manual entry, not wizard) ─────────────
    if (state.showAddDialog) {
        AddProviderDialog(
            onConfirm = { name, url, key, rpm, rpd, ctx, out, model, tier, isByok ->
                viewModel.addCustomProvider(name, url, key, rpm, rpd, ctx, out, model, tier, isByok)
                viewModel.hideAddDialog()
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    // ── Per-provider API config sheet ─────────────────────────────────────────
    state.configSheetProvider?.let { entity ->
        ProviderApiConfigSheet(
            entity = entity,
            onDismiss = { viewModel.hideConfigSheet() },
            onSave = { alias, baseUrl, model, rpm, rpd ->
                viewModel.updateProviderConfig(entity.id, alias, baseUrl, model, rpm, rpd)
                viewModel.hideConfigSheet()
            }
        )
    }
}

// ── Section header ─────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

// ── Provider card ──────────────────────────────────────────────────────────────
@Composable
private fun ProviderCard(
    pw: ProvidersViewModel.ProviderWithUsage,
    viewModel: ProvidersViewModel
) {
    val context = LocalContext.current
    val entity  = pw.entity

    var showKeyInput    by remember { mutableStateOf(false) }
    var apiKeyText      by remember { mutableStateOf("") }
    var showKey         by remember { mutableStateOf(false) }
    var showSaJsonInput by remember { mutableStateOf(false) }
    var saJsonText      by remember { mutableStateOf("") }
    var modelsExpanded  by remember { mutableStateOf(false) }

    val needsKey = !entity.noAuth && !pw.hasApiKey

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (needsKey && entity.enabled)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entity.name, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        TierChip(entity.tier)
                        if (entity.isByok) {
                            Spacer(Modifier.width(4.dp))
                            ByokChip()
                        }
                        if (pw.hasApiKey || entity.noAuth) {
                            Spacer(Modifier.width(4.dp))
                            KeySetChip(entity.noAuth)
                        }
                        if (entity.deprecated) {
                            Spacer(Modifier.width(4.dp))
                            DeprecatedChip()
                        }
                    }
                    Text(
                        text = entity.baseUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Config sheet button
                IconButton(onClick = { viewModel.showConfigSheet(entity) }) {
                    Icon(Icons.Default.Tune, contentDescription = "Konfigurieren",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline)
                }
                Switch(
                    checked = entity.enabled,
                    onCheckedChange = { viewModel.toggleProvider(entity.id, it) }
                )
            }

            // ── Deprecation notice ────────────────────────────────────────────
            if (entity.deprecated && entity.deprecationNotice.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚠ ${entity.deprecationNotice}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Rate limit gauges ─────────────────────────────────────────────
            if (entity.rpmLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPM", pw.rpmUsed, entity.rpmLimit)
                Spacer(Modifier.height(4.dp))
            }
            if (entity.rpdLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPD", pw.rpdUsed, entity.rpdLimit)
                Spacer(Modifier.height(4.dp))
            }

            // ── Model list (collapsible) ───────────────────────────────────────
            if (pw.models.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { modelsExpanded = !modelsExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pw.models.size} Modell${if (pw.models.size != 1) "e" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    val chevronAngle by animateFloatAsState(
                        targetValue = if (modelsExpanded) 180f else 0f,
                        label = "chevron_${entity.id}"
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).rotate(chevronAngle),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                AnimatedVisibility(visible = modelsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        pw.models.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${model.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${model.contextWindow / 1_000}k",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Key / Auth row ────────────────────────────────────────────────
            if (!entity.noAuth) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (pw.hasApiKey) Icons.Default.Key else Icons.Default.KeyOff,
                        contentDescription = null,
                        tint = if (pw.hasApiKey) BabuTeal else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (pw.hasApiKey) {
                            if (entity.authType == "VERTEX") "SA JSON hinterlegt" else "API-Key hinterlegt"
                        } else {
                            if (entity.authType == "VERTEX") "Kein SA JSON" else "Kein API-Key"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pw.hasApiKey) BabuTeal else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.weight(1f))

                    // Open console URL
                    val consoleUrl = entity.consoleUrl.ifEmpty { entity.registrationUrl }
                    if (consoleUrl.isNotEmpty()) {
                        TextButton(onClick = { openCustomTab(context, consoleUrl) }) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Key holen", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Toggle key input
                    if (entity.authType == "VERTEX") {
                        TextButton(onClick = { showSaJsonInput = !showSaJsonInput }) {
                            Text(if (showSaJsonInput) "Abbrechen" else if (pw.hasApiKey) "SA JSON ändern" else "SA JSON laden")
                        }
                    } else {
                        TextButton(onClick = { showKeyInput = !showKeyInput }) {
                            Text(if (showKeyInput) "Abbrechen" else if (pw.hasApiKey) "Ändern" else "Key setzen")
                        }
                    }
                }

                // Standard API key input
                if (showKeyInput && entity.authType != "VERTEX") {
                    Spacer(Modifier.height(4.dp))
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
                            OutlinedButton(onClick = {
                                viewModel.removeApiKey(entity.id)
                                showKeyInput = false
                                apiKeyText = ""
                            }) { Text("Entfernen") }
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

                // Vertex SA JSON input
                if (showSaJsonInput && entity.authType == "VERTEX") {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = saJsonText,
                        onValueChange = { saJsonText = it },
                        label = { Text("Service Account JSON") },
                        placeholder = { Text("{ \"type\": \"service_account\", … }") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 160.dp),
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (pw.hasApiKey) {
                            OutlinedButton(onClick = {
                                viewModel.removeApiKey(entity.id)
                                showSaJsonInput = false
                                saJsonText = ""
                            }) { Text("Entfernen") }
                        }
                        Button(
                            onClick = {
                                viewModel.saveVertexSaJson(entity.id, saJsonText)
                                showSaJsonInput = false
                                saJsonText = ""
                            },
                            enabled = saJsonText.isNotBlank()
                        ) { Text("Speichern") }
                    }
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            if (entity.notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entity.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ── Chips ──────────────────────────────────────────────────────────────────────

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
private fun KeySetChip(noAuth: Boolean = false) {
    Surface(color = BabuTeal.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (noAuth) Icons.Default.LockOpen else Icons.Default.Check,
                null,
                tint = BabuTeal,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                if (noAuth) "Kein Key nötig" else "Key ✓",
                style = MaterialTheme.typography.labelSmall,
                color = BabuTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeprecatedChip() {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
        Text(
            "deprecated",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Rate limit gauge ───────────────────────────────────────────────────────────
@Composable
fun RateLimitGauge(label: String, used: Int, limit: Int) {
    val fraction = if (limit > 0 && limit < Int.MAX_VALUE)
        (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "gauge_$label"
    )
    val color = when {
        fraction < 0.6f  -> BabuTeal
        fraction < 0.85f -> AccentAmber
        else             -> RauschRed
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
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

// ── Add custom provider dialog ─────────────────────────────────────────────────
@Composable
private fun AddProviderDialog(
    onConfirm: (String, String, String, Int, Int, Int, Int, String, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name          by remember { mutableStateOf("") }
    var baseUrl       by remember { mutableStateOf("https://") }
    var apiKey        by remember { mutableStateOf("") }
    var rpmLimit      by remember { mutableStateOf("60") }
    var rpdLimit      by remember { mutableStateOf("") }
    var contextWindow by remember { mutableStateOf("128000") }
    var maxOutput     by remember { mutableStateOf("4096") }
    var modelId       by remember { mutableStateOf("") }
    var tier          by remember { mutableStateOf(3) }
    var isByok        by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Provider hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name*") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL*") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(modelId, { modelId = it }, label = { Text("Model ID*") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    apiKey, { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(rpmLimit, { rpmLimit = it }, label = { Text("RPM") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(rpdLimit, { rpdLimit = it }, label = { Text("RPD") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        placeholder = { Text("∞") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(contextWindow, { contextWindow = it }, label = { Text("Context") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(maxOutput, { maxOutput = it }, label = { Text("Max Out") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun openCustomTab(context: Context, url: String) {
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    }
}
