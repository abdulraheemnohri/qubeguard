package com.qubeguard.app.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.qubeguard.app.browser.SecureWebView
import com.qubeguard.app.ui.theme.QubeGuardTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Browser Activity for QubeGuard.
 * Displays a private browser with URL bar, navigation buttons, and Qube selection.
 */
@AndroidEntryPoint
class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent?.dataString ?: intent?.getStringExtra("url") ?: "https://www.example.com"
        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(initialUrl = initialUrl)
                }
            }
        }
    }
}

@Composable
fun BrowserScreen(initialUrl: String = "https://www.example.com") {
    val viewModel: BrowserViewModel = hiltViewModel()
    var url by remember { mutableStateOf(initialUrl) }
    var webView: SecureWebView? by remember { mutableStateOf(null) }
    val selectedQube by viewModel.selectedQube.observeAsState()
    val qubes by viewModel.qubes.observeAsState(emptyList())
    var showQubeDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(selectedQube) {
        selectedQube?.let { qube ->
            webView?.setQubeId(qube.id)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation & Qube selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webView?.goBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            IconButton(onClick = { webView?.goForward() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward"
                )
            }

            IconButton(onClick = { webView?.reload() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            Box {
                OutlinedButton(onClick = { showQubeDropdown = true }) {
                    Text(selectedQube?.name ?: "Qube")
                }
                DropdownMenu(
                    expanded = showQubeDropdown,
                    onDismissRequest = { showQubeDropdown = false }
                ) {
                    qubes.forEach { qube ->
                        DropdownMenuItem(
                            text = { Text(qube.name + if (qube.isIncognito) " (Incognito)" else "") },
                            onClick = {
                                viewModel.selectQube(qube)
                                showQubeDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // URL input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f),
                label = { Text("URL") },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val formattedUrl = when {
                        url.startsWith("http://") || url.startsWith("https://") -> url
                        else -> "https://$url"
                    }
                    url = formattedUrl
                    viewModel.setUrl(formattedUrl)
                    webView?.loadUrl(formattedUrl)
                }
            ) {
                Text("Go")
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                SecureWebView(ctx).apply {
                    selectedQube?.let { setQubeId(it.id) }
                    webViewClient = object : WebViewClient() {
                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            url?.let {
                                this@apply.loadUrl(it)
                                viewModel.setUrl(it)
                            }
                            return true
                        }
                    }
                    webView = this
                    loadUrl(initialUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserScreenPreview() {
    QubeGuardTheme {
        BrowserScreen()
    }
}
