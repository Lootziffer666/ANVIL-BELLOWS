package com.anvil.bellows.presentation.ui.providers

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bottom sheet for per-provider advanced configuration.
 *
 * Features
 * ─────────
 * • Alias override (display name)
 * • Base URL override
 * • Default model override
 * • RPM / RPD limit override
 * • JSON export (ShareIntent)
 * • JSON import (file picker)
 *
 * The sheet does NOT write to the database directly; it calls [onSave]
 * which is handled by [ProvidersViewModel.updateProviderConfig].
 */
@Serializable
data class ProviderConfigExport(
    val id: String,
    val name: String,
    val baseUrl: String,
    val selectedModel: String,
    val rpmLimit: Int,
    val rpdLimit: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderApiConfigSheet(
    entity: ProviderConfigEntity,
    onDismiss: () -> Unit,
    onSave: (alias: String, baseUrl: String, model: String, rpm: Int, rpd: Int) -> Unit
) {
    val context = LocalContext.current

    var alias    by remember(entity.id) { mutableStateOf(entity.name) }
    var baseUrl  by remember(entity.id) { mutableStateOf(entity.baseUrl) }
    var model    by remember(entity.id) { mutableStateOf(entity.selectedModel) }
    var rpmLimit by remember(entity.id) { mutableStateOf(
        if (entity.rpmLimit == Int.MAX_VALUE) "" else entity.rpmLimit.toString()
    )}
    var rpdLimit by remember(entity.id) { mutableStateOf(
        if (entity.rpdLimit == Int.MAX_VALUE) "" else entity.rpdLimit.toString()
    )}

    // ── JSON import launcher ──────────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: return@runCatching
            val cfg = Json.decodeFromString<ProviderConfigExport>(json)
            alias    = cfg.name
            baseUrl  = cfg.baseUrl
            model    = cfg.selectedModel
            rpmLimit = if (cfg.rpmLimit == Int.MAX_VALUE) "" else cfg.rpmLimit.toString()
            rpdLimit = if (cfg.rpdLimit == Int.MAX_VALUE) "" else cfg.rpdLimit.toString()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Konfiguration: ${entity.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Export
                IconButton(onClick = {
                    val cfg = ProviderConfigExport(
                        id            = entity.id,
                        name          = alias,
                        baseUrl       = baseUrl,
                        selectedModel = model,
                        rpmLimit      = rpmLimit.toIntOrNull() ?: Int.MAX_VALUE,
                        rpdLimit      = rpdLimit.toIntOrNull() ?: Int.MAX_VALUE
                    )
                    val jsonStr = Json.encodeToString(cfg)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, jsonStr)
                        putExtra(Intent.EXTRA_SUBJECT, "Bellows Provider Config – ${entity.name}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Config exportieren"))
                }) {
                    Icon(Icons.Default.Share, "Exportieren")
                }
                // Import
                IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                    Icon(Icons.Default.FileOpen, "Importieren")
                }
            }

            HorizontalDivider()

            // ── Fields ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias (Anzeigename)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Label, null) }
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL Override") },
                placeholder = { Text(entity.baseUrl) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Link, null) }
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Standard-Modell") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.SmartToy, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rpmLimit,
                    onValueChange = { rpmLimit = it },
                    label = { Text("RPM Limit") },
                    placeholder = { Text("∞") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = rpdLimit,
                    onValueChange = { rpdLimit = it },
                    label = { Text("RPD Limit") },
                    placeholder = { Text("∞") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ── Auth info (read-only) ─────────────────────────────────────────
            if (entity.authHeaderName != "Authorization") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Auth-Header: ${entity.authHeaderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Buttons ───────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = {
                        onSave(
                            alias,
                            baseUrl,
                            model,
                            rpmLimit.toIntOrNull() ?: Int.MAX_VALUE,
                            rpdLimit.toIntOrNull() ?: Int.MAX_VALUE
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}
