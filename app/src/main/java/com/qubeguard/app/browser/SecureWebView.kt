package com.qubeguard.app.browser

import android.content.Context
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SecureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr) {

    @Inject lateinit var deterministicBlocker: DeterministicBlocker
    @Inject lateinit var mlClassifier: MLClassifier

    private var qubeId: String = "default"
    var onProgressChanged: ((Int) -> Unit)? = null
    var onTitleReceived: ((String) -> Unit)? = null

    companion object {
        const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    init {
        initializeWebView()
    }

    private fun initializeWebView() {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.domStorageEnabled = false
        settings.databaseEnabled = false
        @Suppress("DEPRECATION")
        settings.saveFormData = false
        settings.textZoom = 100
        settings.userAgentString = MOBILE_USER_AGENT

        webViewClient = SecureWebViewClient(deterministicBlocker, mlClassifier)
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged?.invoke(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onTitleReceived?.invoke(it) }
            }
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
    }

    fun setDesktopMode(enabled: Boolean) {
        settings.userAgentString = if (enabled) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
        settings.useWideViewPort = enabled
        settings.loadWithOverviewMode = enabled
    }

    fun setQubeId(qubeId: String) {
        this.qubeId = qubeId
    }

    fun getQubeId(): String = qubeId

    fun clearAllData() {
        clearCache(true)
        clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
    }

    class SecureWebViewClient(
        private val deterministicBlocker: DeterministicBlocker,
        private val mlClassifier: MLClassifier
    ) : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            if (runBlocking { deterministicBlocker.isBlocked(url) }) {
                return createBlockedResponse()
            }

            if (mlClassifier.isModelLoaded() && runBlocking { mlClassifier.isBlocked(url) }) {
                return createBlockedResponse()
            }

            return null
        }

        private fun createBlockedResponse(): WebResourceResponse =
            WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), null)
    }
}
