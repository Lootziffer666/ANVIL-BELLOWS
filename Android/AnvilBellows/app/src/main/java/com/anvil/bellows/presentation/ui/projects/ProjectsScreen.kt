package com.anvil.bellows.presentation.ui.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.domain.model.ConversationSession
import com.anvil.bellows.domain.model.Project
import com.anvil.bellows.presentation.viewmodel.ProjectsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    paddingValues: PaddingValues,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Projects", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.CreateNewFolder, "New Project")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.projects) { project ->
                ProjectNode(
                    project = project,
                    isExpanded = project.id in state.expandedIds,
                    onToggle = { viewModel.toggleExpanded(project.id) },
                    onExportSession = { viewModel.exportSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    expandedIds = state.expandedIds,
                    onToggleChild = { viewModel.toggleExpanded(it) }
                )
            }

            if (state.unorganizedSessions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Unorganized", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                }
                items(state.unorganizedSessions) { session ->
                    SessionItem(
                        session = session,
                        onExport = { viewModel.exportSession(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    if (state.showCreateProjectDialog) {
        CreateProjectDialog(
            onConfirm = { name, color ->
                viewModel.createProject(name, color, null)
                viewModel.hideCreateDialog()
            },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }

    state.exportMarkdown?.let { markdown ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            title = { Text("Export Conversation") },
            text = {
                Text(
                    "Share as Markdown or copy to wiki?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.shareExport(markdown, "conversation") }) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExport() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProjectNode(
    project: Project,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onExportSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    expandedIds: Set<String>,
    onToggleChild: (String) -> Unit,
    depth: Int = 0
) {
    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, modifier = Modifier.size(18.dp)
                )
            }
            Icon(Icons.Default.Folder, null, tint = Color(android.graphics.Color.parseColor(project.color)), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(project.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                "${project.sessions.size} sessions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                project.sessions.forEach { session ->
                    SessionItem(
                        session = session,
                        onExport = { onExportSession(session.id) },
                        onDelete = { onDeleteSession(session.id) },
                        depth = depth + 1
                    )
                }
                project.children.forEach { child ->
                    ProjectNode(
                        project = child,
                        isExpanded = child.id in expandedIds,
                        onToggle = { onToggleChild(child.id) },
                        onExportSession = onExportSession,
                        onDeleteSession = onDeleteSession,
                        expandedIds = expandedIds,
                        onToggleChild = onToggleChild,
                        depth = depth + 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: ConversationSession,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    depth: Int = 0
) {
    val dateStr = remember(session.updatedAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(session.updatedAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16 + 32).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "$dateStr • ${session.currentProviderId}${if (session.handoffCount > 0) " • ${session.handoffCount} handoffs" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.FileUpload, "Export", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF5A5F") }
    val colors = listOf("#FF5A5F", "#00A699", "#F7B731", "#2196F3", "#9C27B0", "#4CAF50")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Surface(
                            onClick = { selectedColor = color },
                            color = Color(android.graphics.Color.parseColor(color)),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.size(32.dp),
                            shadowElevation = if (color == selectedColor) 4.dp else 0.dp
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
