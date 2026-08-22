package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.data.blocklist.DnsLogEntity
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Pi-hole style Query Log & Analytics Screen.
 */
@Composable
fun DnsLogScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val dnsLogs by viewModel.dnsLogs.observeAsState(emptyList())

    var filterType by remember { mutableStateOf("ALL") } // ALL, BLOCKED, ALLOWED
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadDnsLogs()
    }

    val totalQueries = dnsLogs.size
    val blockedQueries = dnsLogs.count { it.isBlocked }
    val blockPercentage = if (totalQueries > 0) (blockedQueries * 100) / totalQueries else 0

    val topBlocked = remember(dnsLogs) {
        dnsLogs.filter { it.isBlocked }
            .groupingBy { it.domain }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    val topPermitted = remember(dnsLogs) {
        dnsLogs.filter { !it.isBlocked }
            .groupingBy { it.domain }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    val filteredLogs = remember(dnsLogs, filterType, searchQuery) {
        dnsLogs.filter { log ->
            val matchesFilter = when (filterType) {
                "BLOCKED" -> log.isBlocked
                "ALLOWED" -> !log.isBlocked
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || log.domain.contains(searchQuery.trim(), ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Pi-hole Query Monitor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Total: $totalQueries • Blocked: $blockedQueries ($blockPercentage%)", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = { viewModel.loadDnsLogs() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { viewModel.clearDnsLogs() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs")
                }
            }
        }

        // Top Blocked / Permitted Analytics
        if (dnsLogs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Top Blocked", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (topBlocked.isEmpty()) {
                            Text("None", style = MaterialTheme.typography.bodySmall)
                        } else {
                            topBlocked.forEach { (domain, count) ->
                                Text("$domain ($count)", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Top Permitted", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (topPermitted.isEmpty()) {
                            Text("None", style = MaterialTheme.typography.bodySmall)
                        } else {
                            topPermitted.forEach { (domain, count) ->
                                Text("$domain ($count)", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by domain...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filterType == "ALL", onClick = { filterType = "ALL" }, label = { Text("All ($totalQueries)") })
            FilterChip(selected = filterType == "BLOCKED", onClick = { filterType = "BLOCKED" }, label = { Text("Blocked ($blockedQueries)") })
            FilterChip(selected = filterType == "ALLOWED", onClick = { filterType = "ALLOWED" }, label = { Text("Allowed (${totalQueries - blockedQueries})") })
        }

        if (filteredLogs.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No matching DNS logs found.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    DnsLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun DnsLogItem(log: DnsLogEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.domain,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = log.reason,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (log.isBlocked) "BLOCKED" else "ALLOWED",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (log.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DnsLogScreenPreview() {
    QubeGuardTheme {
        Surface {
            DnsLogScreen()
        }
    }
}
