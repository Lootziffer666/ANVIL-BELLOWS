package com.anvil.bellows.presentation.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.domain.model.ChatMessage
import com.anvil.bellows.presentation.theme.*
import com.anvil.bellows.presentation.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    paddingValues: PaddingValues,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.messages.size, state.streamingText) {
        if (state.messages.isNotEmpty() || state.streamingText.isNotEmpty()) {
            listState.animateScrollToItem(
                (state.messages.size + if (state.isStreaming) 1 else 0).coerceAtLeast(0)
            )
        }
    }

    LaunchedEffect(state.handoffToast) {
        state.handoffToast?.let { event ->
            snackbarHostState.showSnackbar(
                message = "Switched: ${event.fromProviderName} → ${event.toProviderName}",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissHandoffToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Anvil Bellows", fontWeight = FontWeight.Bold) },
                actions = {
                    val badgeLabel = when {
                        state.selectedProviderName.isNotEmpty() -> state.selectedProviderName
                        state.availableProviders.isNotEmpty() -> state.availableProviders.first().name
                        else -> null
                    }
                    if (badgeLabel != null) {
                        Box {
                            ProviderBadge(
                                label = badgeLabel,
                                onClick = { viewModel.toggleProviderPicker() }
                            )
                            DropdownMenu(
                                expanded = state.showProviderPicker,
                                onDismissRequest = { viewModel.toggleProviderPicker() }
                            ) {
                                Text(
                                    "Provider wählen",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider()
                                state.availableProviders.forEach { provider ->
                                    val isSelected = provider.id == state.selectedProviderId
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(provider.name)
                                                if (isSelected) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.Check, null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { viewModel.selectProvider(provider) },
                                        leadingIcon = {
                                            Surface(
                                                color = tierColor(provider.tier).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "T${provider.tier}",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = tierColor(provider.tier),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.newSession() }) {
                        Icon(Icons.Default.Add, "Neuer Chat")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.messages.isEmpty() && !state.isStreaming) {
                    item { WelcomeCard(hasProviders = state.availableProviders.isNotEmpty()) }
                }
                items(state.messages) { msg ->
                    MessageBubble(msg)
                }
                if (state.isStreaming && state.streamingText.isNotEmpty()) {
                    item { StreamingBubble(state.streamingText) }
                }
                if (state.isStreaming && state.streamingText.isEmpty()) {
                    item { ThinkingIndicator() }
                }
            }

            state.error?.let { error ->
                ErrorBanner(error) { viewModel.dismissError() }
            }

            ChatInputBar(
                text = state.inputText,
                onTextChange = { viewModel.onInputChanged(it) },
                onSend = { viewModel.sendMessage() },
                isStreaming = state.isStreaming,
                enabled = state.availableProviders.isNotEmpty()
            )
        }
    }
}

@Composable
private fun ProviderBadge(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SmartToy, null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                Icons.Default.ArrowDropDown, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isUser && message.providerId != null) {
                Text(
                    text = message.providerId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = "$text▊",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            modifier = Modifier.padding(top = 12.dp).width(80.dp),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun WelcomeCard(hasProviders: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Willkommen bei Anvil Bellows",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Unified LLM-Router mit automatischem Failover über 14+ Provider.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            if (hasProviders) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = BabuTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Provider bereit – einfach losschreiben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BabuTeal
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Bitte API-Keys im Tab «Providers» hinterlegen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Schließen")
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    if (enabled) "Nachricht…"
                    else "Bitte zuerst API-Key in Providers hinterlegen"
                )
            },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
            enabled = enabled && !isStreaming
        )
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = { if (!isStreaming && enabled) onSend() },
            containerColor = when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant
                isStreaming -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                if (isStreaming) Icons.Default.HourglassEmpty else Icons.AutoMirrored.Filled.Send,
                "Senden",
                tint = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
            )
        }
    }
}
