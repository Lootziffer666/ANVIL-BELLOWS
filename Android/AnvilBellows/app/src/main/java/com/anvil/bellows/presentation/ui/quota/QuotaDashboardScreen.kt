package com.anvil.bellows.presentation.ui.quota

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anvil.bellows.domain.model.QuotaStatus
import com.anvil.bellows.presentation.theme.*
import com.anvil.bellows.presentation.ui.providers.RateLimitGauge
import com.anvil.bellows.presentation.viewmodel.QuotaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotaDashboardScreen(
    paddingValues: PaddingValues,
    viewModel: QuotaViewModel = hiltViewModel()
) {
    val quotaList by viewModel.quotaList.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quota Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (quotaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading quota data…")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quotaList) { status ->
                    QuotaCard(status)
                }
            }
        }
    }
}

@Composable
private fun QuotaCard(status: QuotaStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isAvailable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (status.isAvailable) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (status.isAvailable) BabuTeal else RauschRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    status.provider.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Text(
                "T${status.provider.tier} • ${status.provider.selectedModel.take(18)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )

            Spacer(Modifier.height(8.dp))

            // ── Rate limit gauges ─────────────────────────────────────────────
            if (status.provider.rpmLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPM", status.rpmUsed, status.provider.rpmLimit)
                Spacer(Modifier.height(4.dp))
            }
            if (status.provider.rpdLimit < Int.MAX_VALUE) {
                RateLimitGauge("RPD", status.rpdUsed, status.provider.rpdLimit)
                Spacer(Modifier.height(4.dp))
            }

            // ── Daily activity summary ────────────────────────────────────────
            if (status.totalRequestsToday > 0 || status.totalTokensToday > 0L) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatPill(
                        label = "Reqs",
                        value = status.totalRequestsToday.toString()
                    )
                    StatPill(
                        label = "Tokens",
                        value = formatTokenCount(status.totalTokensToday)
                    )
                }
            } else {
                Text(
                    "Keine Requests heute",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BabuTeal
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun formatTokenCount(count: Long): String = when {
    count >= 1_000_000L -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000L     -> "%.1fK".format(count / 1_000.0)
    else                -> count.toString()
}
