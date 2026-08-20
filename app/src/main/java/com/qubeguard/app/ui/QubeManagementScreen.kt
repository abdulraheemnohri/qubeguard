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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.browser.QubeProfile
import com.qubeguard.app.ui.theme.QubeGuardTheme

/**
 * Qube Management Screen for creating, deleting, and managing Qube profiles.
 */
@Composable
fun QubeManagementScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val qubes by viewModel.qubeProfiles.observeAsState(emptyList())
    var newQubeName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Qube Management",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create, delete, or manage your Qube profiles.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // New Qube Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newQubeName,
                onValueChange = { newQubeName = it },
                modifier = Modifier.weight(1f),
                label = { Text("New Qube Name") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (newQubeName.isNotBlank()) {
                        viewModel.createQube(newQubeName)
                        newQubeName = ""
                    }
                },
                enabled = newQubeName.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Qube"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Qube List
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(qubes) { qube ->
                QubeManagementItem(
                    qube = qube,
                    onDelete = { viewModel.deleteQube(qube.id) },
                    onSetDefault = { viewModel.setDefaultQube(qube.id) }
                )
            }
        }
    }
}

@Composable
fun QubeManagementItem(
    qube: QubeProfile,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
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
                text = qube.name,
                style = MaterialTheme.typography.bodyLarge
            )
            if (qube.isIncognito) {
                Text(
                    text = "Incognito",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (qube.isDefault) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Default",
                tint = Color.Yellow
            )
        } else {
            IconButton(
                onClick = onSetDefault
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Set as Default",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onDelete
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QubeManagementScreenPreview() {
    QubeGuardTheme {
        Surface {
            QubeManagementScreen()
        }
    }
}
