package com.anvil.bellows.presentation.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anvil.bellows.presentation.theme.BabuTeal
import com.anvil.bellows.presentation.viewmodel.LocalServerViewModel

/**
 * Collapsible info-banner displayed at the top of the Chat/Home screen
 * showing the BELLOWS local server endpoint and API token.
 *
 * Users can:
 *  • Toggle key visibility
 *  • Copy the key with a single tap
 *  • Regenerate the key (new random token)
 *
 * The banner is collapsed by default after the key has been set at
 * least once, so it stays out of the way for returning users.
 *
 * Embed in ChatOverviewScreen above the agent/preset list:
 *
 *   LocalServerCard()
 *   AgentPresetList(…)
 */
@Composable
fun LocalServerCard(
    modifier: Modifier = Modifier,
    viewModel: LocalServerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state   by viewModel.uiState.collectAsState()

    var expanded   by remember { mutableStateOf(!state.hasKey) }
    var showToken  by remember { mutableStateOf(false) }
    var showRegen  by remember { mutableStateOf(false) }

    // Collapse automatically once a key exists
    LaunchedEffect(state.hasKey) {
        if (state.hasKey) expanded = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Always-visible header row ─────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Router,
                    contentDescription = null,
                    tint = BabuTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "BELLOWS Local API",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Running indicator
                if (state.serverRunning) {
                    Surface(
                        color = BabuTeal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "● Port ${state.port}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BabuTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Einklappen" else "Ausklappen"
                    )
                }
            }

            // ── Expandable section ────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Endpoint label
                    Text(
                        "Endpunkt: http://localhost:${state.port}/v1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Token row
                    if (state.hasKey) {
                        OutlinedTextField(
                            value = state.token,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bearer Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showToken) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    // Visibility toggle
                                    IconButton(onClick = { showToken = !showToken }) {
                                        Icon(
                                            if (showToken) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            contentDescription = "Sichtbarkeit"
                                        )
                                    }
                                    // Copy
                                    IconButton(onClick = {
                                        copyToClipboard(context, state.token)
                                    }) {
                                        Icon(Icons.Default.ContentCopy, "Kopieren")
                                    }
                                }
                            }
                        )

                        // Regenerate button
                        if (!showRegen) {
                            TextButton(
                                onClick = { showRegen = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Key regenerieren")
                            }
                        } else {
                            // Confirmation step
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "⚠ Alle Clients brauchen danach den neuen Key.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showRegen = false },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Abbrechen") }
                                        Button(
                                            onClick = {
                                                viewModel.regenerateToken()
                                                showRegen = false
                                                showToken = true // auto-reveal new key
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Jetzt regenerieren") }
                                    }
                                }
                            }
                        }
                    } else {
                        // No key set yet — generate one
                        Button(
                            onClick = { viewModel.generateToken() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bearer Token generieren")
                        }
                    }

                    // Usage hint
                    Text(
                        "Authorization: Bearer ${if (state.hasKey) "<dein-token>" else "(noch kein Token)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Bellows Token", text))
}
