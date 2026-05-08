package com.anvil.bellows.presentation.ui.agents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import com.anvil.bellows.presentation.viewmodel.AgentPresetsViewModel
import com.anvil.bellows.presentation.viewmodel.ProvidersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPresetEditorScreen(
    presetId: String?,
    onBack: () -> Unit,
    viewModel: AgentPresetsViewModel = hiltViewModel(),
    providersViewModel: ProvidersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val providersState by providersViewModel.uiState.collectAsStateWithLifecycle()

    val preset = if (presetId != null)
        state.presets.firstOrNull { it.id == presetId }
    else null

    var name by remember(preset) { mutableStateOf(preset?.name ?: "") }
    var description by remember(preset) { mutableStateOf(preset?.description ?: "") }
    var systemPrompt by remember(preset) { mutableStateOf(preset?.systemPrompt ?: "") }
    var selectedProviderId by remember(preset) { mutableStateOf(preset?.defaultProviderId ?: "") }
    var functionSchemaJson by remember(preset) { mutableStateOf(preset?.functionSchemaJson ?: "") }
    var showProviderMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isBuiltIn = preset?.isBuiltIn == true
    val isNew = preset == null
    val canSave = name.isNotBlank() && !isBuiltIn
    val systemPromptLength = systemPrompt.length
    val maxSystemPrompt = 4000

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Agent" else if (isBuiltIn) preset.name else "Edit Agent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isBuiltIn && !isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isBuiltIn) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Built-in presets cannot be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isBuiltIn,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                enabled = !isBuiltIn
            )

            Column {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { if (it.length <= maxSystemPrompt) systemPrompt = it },
                    label = { Text("Instructions / System Prompt") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 12,
                    enabled = !isBuiltIn
                )
                Text(
                    "$systemPromptLength / $maxSystemPrompt",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (systemPromptLength > maxSystemPrompt * 0.9)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }

            ExposedDropdownMenuBox(
                expanded = showProviderMenu,
                onExpandedChange = { if (!isBuiltIn) showProviderMenu = it }
            ) {
                val selectedName = providersState.providers
                    .firstOrNull { it.entity.id == selectedProviderId }?.entity?.name
                    ?: "Auto (use best available)"
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Default Provider") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProviderMenu) },
                    enabled = !isBuiltIn
                )
                ExposedDropdownMenu(
                    expanded = showProviderMenu,
                    onDismissRequest = { showProviderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Auto (use best available)") },
                        onClick = { selectedProviderId = ""; showProviderMenu = false }
                    )
                    providersState.providers.forEach { pw ->
                        DropdownMenuItem(
                            text = { Text(pw.entity.name) },
                            onClick = { selectedProviderId = pw.entity.id; showProviderMenu = false }
                        )
                    }
                }
            }

            Column {
                OutlinedTextField(
                    value = functionSchemaJson,
                    onValueChange = { functionSchemaJson = it },
                    label = { Text("Function Schema JSON (optional)") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 8,
                    enabled = !isBuiltIn,
                    isError = state.jsonError != null
                )
                if (state.jsonError != null) {
                    Text(
                        state.jsonError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "Schema stored for future use — not yet executed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!isBuiltIn) {
                Button(
                    onClick = {
                        val updated = AgentPresetEntity(
                            id = preset?.id ?: "",
                            name = name.trim(),
                            description = description.trim(),
                            systemPrompt = systemPrompt.trim(),
                            defaultProviderId = selectedProviderId.ifEmpty { null },
                            defaultModelId = preset?.defaultModelId,
                            functionSchemaJson = functionSchemaJson.trim().ifEmpty { null },
                            isPinned = preset?.isPinned ?: false,
                            isBuiltIn = false,
                            createdAt = preset?.createdAt ?: 0L,
                            updatedAt = 0L
                        )
                        if (viewModel.createOrUpdate(updated)) onBack()
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isNew) "Create" else "Save") }
            }
        }
    }

    if (showDeleteConfirm && preset != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${preset.name}\"?") },
            text = { Text("This agent preset will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(preset)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
