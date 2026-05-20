package com.anvil.bellows.presentation.ui.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.domain.model.MemoryChunk
import com.anvil.bellows.presentation.theme.AmberDark
import com.anvil.bellows.presentation.theme.IigSuccessDark
import com.anvil.bellows.presentation.theme.OxidRedHover
import com.anvil.bellows.presentation.viewmodel.MemoryViewModel
import com.anvil.bellows.presentation.viewmodel.SessionMemoryGroup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val groups     by viewModel.groups.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val isLoading  by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // Per-group expand state (all collapsed by default)
    val expandedSessions = remember { mutableStateSetOf<String>() }

    // Clear-session confirmation
    var clearTarget by remember { mutableStateOf<SessionMemoryGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Memory", fontWeight = FontWeight.Bold)
                        if (totalCount > 0) {
                            Text(
                                "$totalCount Chunk${if (totalCount == 1) "" else "s"} • ${groups.size} Session${if (groups.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Aktualisieren")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            groups.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Noch keine Memories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Memories werden automatisch aus Chat-Sessions extrahiert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.sessionId }) { group ->
                        val isExpanded = group.sessionId in expandedSessions
                        SessionGroupCard(
                            group      = group,
                            isExpanded = isExpanded,
                            onToggle   = {
                                if (isExpanded) expandedSessions.remove(group.sessionId)
                                else expandedSessions.add(group.sessionId)
                            },
                            onDeleteChunk   = { viewModel.deleteChunk(it) },
                            onClearSession  = { clearTarget = group }
                        )
                    }
                }
            }
        }
    }

    // ── Clear-session confirmation dialog ──────────────────────────────────────
    clearTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            icon = {
                Icon(Icons.Default.DeleteSweep, null, tint = OxidRedHover)
            },
            title = { Text("Session-Memory löschen?") },
            text = {
                Text(
                    "Alle ${target.chunks.size} Chunk${if (target.chunks.size == 1) "" else "s"} " +
                            "der Session „${target.sessionTitle}" werden unwiderruflich gelöscht."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSession(target.sessionId)
                        clearTarget = null
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearTarget = null }) { Text("Abbrechen") }
            }
        )
    }
}

// ── SessionGroupCard ───────────────────────────────────────────────────────────

@Composable
private fun SessionGroupCard(
    group: SessionMemoryGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDeleteChunk: (String) -> Unit,
    onClearSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // ── Session header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AmberDark
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.sessionTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${group.chunks.size} Chunk${if (group.chunks.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                // Clear session button
                IconButton(onClick = onClearSession, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteSweep, "Session löschen",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                // Expand/collapse
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (isExpanded) "Einklappen" else "Ausklappen",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Chunks (animated) ──────────────────────────────────────────────
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp, end = 12.dp, bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()
                    group.chunks.forEach { chunk ->
                        MemoryChunkItem(chunk = chunk, onDelete = { onDeleteChunk(chunk.id) })
                    }
                }
            }
        }
    }
}

// ── MemoryChunkItem ────────────────────────────────────────────────────────────

@Composable
private fun MemoryChunkItem(
    chunk: MemoryChunk,
    onDelete: () -> Unit
) {
    val dateStr = remember(chunk.createdAt) {
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY).format(Date(chunk.createdAt))
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Content preview
            Text(
                chunk.content.take(200).let {
                    if (chunk.content.length > 200) "$it…" else it
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (chunk.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chunk.tags.take(4).forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(tag, style = MaterialTheme.typography.labelSmall)
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Label, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = IigSuccessDark
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline, "Löschen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Helper ─────────────────────────────────────────────────────────────────────

private fun mutableStateSetOf(vararg elements: String): MutableSet<String> =
    mutableSetOf(*elements)
