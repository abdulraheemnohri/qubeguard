package com.qubeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.data.blocklist.BlocklistSource
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Blocklist Settings Screen for enabling/disabling blocklist sources.
 */
@Composable
fun BlocklistSettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val blocklistSources by viewModel.blocklistSources.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Blocklist Sources",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enable or disable blocklist sources to customize your protection.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(blocklistSources) { source ->
                BlocklistSourceItem(
                    source = source,
                    onToggle = { isEnabled ->
                        viewModel.setBlocklistSourceEnabled(source.id, isEnabled)
                    }
                )
            }
        }
    }
}

@Composable
fun BlocklistSourceItem(
    source: BlocklistSource,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Category: ${source.category}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "License: ${source.license}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = source.enabled,
            onCheckedChange = onToggle
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BlocklistSettingsScreenPreview() {
    QubeGuardTheme {
        Surface {
            BlocklistSettingsScreen()
        }
    }
}
