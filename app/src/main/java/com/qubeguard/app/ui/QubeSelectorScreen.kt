package com.qubeguard.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
 * Qube Selector Screen for choosing a Qube profile.
 */
@Composable
fun QubeSelectorScreen(
    onQubeSelected: (QubeProfile) -> Unit,
    onCreateNewQube: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val qubes by viewModel.qubeProfiles.observeAsState(emptyList())
    var selectedQube by remember { mutableStateOf<QubeProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select a Qube",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(qubes) { qube ->
                QubeItem(
                    qube = qube,
                    isSelected = qube == selectedQube,
                    onClick = { selectedQube = qube }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { selectedQube?.let { onQubeSelected(it) } },
                enabled = selectedQube != null
            ) {
                Text("Select")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onCreateNewQube
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Qube"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Qube")
            }
        }
    }
}

@Composable
fun QubeItem(
    qube: QubeProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = qube.name,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1f))

        if (qube.isDefault) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Default",
                tint = Color.Green
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QubeSelectorScreenPreview() {
    QubeGuardTheme {
        Surface {
            QubeSelectorScreen(
                onQubeSelected = {},
                onCreateNewQube = {}
            )
        }
    }
}
