package com.qubeguard.app.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        setContent {
            QubeGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen()
                }
            }
        }
    }
}

@Composable
fun BrowserScreen() {
    val viewModel: BrowserViewModel = hiltViewModel()
    val context = LocalContext.current
    var url by remember { mutableStateOf("https://www.example.com") }
    var webView: SecureWebView? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // URL Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webView?.goBack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { webView?.goForward() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Forward"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { webView?.reload() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f),
                label = { Text("URL") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { webView?.loadUrl(url) }
            ) {
                Text("Go")
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                SecureWebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            url?.let { this@apply.loadUrl(it) }
                            return true
                        }
                    }
                    webView = this
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
