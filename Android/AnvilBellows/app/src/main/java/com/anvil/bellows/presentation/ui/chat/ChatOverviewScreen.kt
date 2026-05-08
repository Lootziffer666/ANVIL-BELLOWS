package com.anvil.bellows.presentation.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.data.local.db.entity.AgentPresetEntity
import com.anvil.bellows.presentation.theme.BabuTeal
import com.anvil.bellows.presentation.viewmodel.AgentPresetsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatOverviewScreen(
    paddingValues: PaddingValues,
    onStartPreset: (AgentPresetEntity) -> Unit,
    onEditPreset: (String) -> Unit,
    onNewPreset: () -> Unit,
    viewModel: AgentPresetsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteCandidate by remember { mutableStateOf<AgentPresetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNewPreset) {
                        Icon(Icons.Default.Add, contentDescription = "Create Agent")
                    }
                }
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
            val pinned = state.presets.filter { it.isPinned }
            val rest = state.presets.filter { !it.isPinned }

            if (pinned.isNotEmpty()) {
                item {
                    Text(
                        "Start",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(pinned, key = { it.id }) { preset ->
                    AgentCard(
                        preset = preset,
                        onClick = { onStartPreset(preset) },
                        onEdit = { onEditPreset(preset.id) },
                        onLongPress = if (!preset.isBuiltIn) ({ deleteCandidate = preset }) else null
                    )
                }
            }

            if (rest.isNotEmpty()) {
                item {
                    Text(
                        "Presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(rest, key = { it.id }) { preset ->
                    AgentCard(
                        preset = preset,
                        onClick = { onStartPreset(preset) },
                        onEdit = { onEditPreset(preset.id) },
                        onLongPress = if (!preset.isBuiltIn) ({ deleteCandidate = preset }) else null
                    )
                }
            }
        }
    }

    deleteCandidate?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete \"${preset.name}\"?") },
            text = { Text("This agent preset will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(preset)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgentCard(
    preset: AgentPresetEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (preset.id) {
                    "builtin_general" -> Icons.AutoMirrored.Filled.Chat
                    "builtin_repo_auditor" -> Icons.Default.Code
                    "builtin_prompt_smith" -> Icons.Default.AutoFixHigh
                    "builtin_prd_sharpener" -> Icons.Default.Description
                    else -> Icons.Default.SmartToy
                },
                contentDescription = null,
                tint = if (preset.isPinned) BabuTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(preset.name, fontWeight = FontWeight.SemiBold)
                    if (preset.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = BabuTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = BabuTeal,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (preset.description.isNotBlank()) {
                    Text(
                        preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2
                    )
                }
            }
            if (!preset.isBuiltIn) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit, contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
