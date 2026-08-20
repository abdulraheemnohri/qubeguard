package com.qubeguard.app.browser

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewFeature
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.MLClassifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SecureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webView
) : WebView(context, attrs, defStyleAttr) {

    @Inject lateinit var deterministicBlocker: DeterministicBlocker
    @Inject lateinit var mlClassifier: MLClassifier

    private var qubeId: String = "default"

    init { initializeWebView() }

    private fun initializeWebView() {
        settings.javaScriptEnabled = true

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLE_WEBGL)) {
            settings.setWebGlEnabled(false)
        }
        settings.setGeolocationEnabled(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.domStorageEnabled = false
        settings.databaseEnabled = false
        settings.saveFormData = false
        settings.textZoom = 100
        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"

        webViewClient = SecureWebViewClient(deterministicBlocker, mlClassifier)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.THIRD_PARTY_COOKIES)) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        }

        clearCache(true)
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }

    fun setQubeId(qubeId: String) { this.qubeId = qubeId }
    fun getQubeId(): String = qubeId

    fun clearAllData() {
        clearCache(true)
        clearHistory()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }

    inner class SecureWebViewClient(
        private val deterministicBlocker: DeterministicBlocker,
        private val mlClassifier: MLClassifier
    ) : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: android.webkit.WebResourceRequest?
        ): android.webkit.WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            if (deterministicBlocker.isBlocked(url)) return createBlockedResponse()

            // Layer 3: local Transformer inference only.
            if (mlClassifier.isModelLoaded() && mlClassifier.isBlocked(url)) {
                return createBlockedResponse()
            }

            return null
        }

        private fun createBlockedResponse(): android.webkit.WebResourceResponse =
            android.webkit.WebResourceResponse("text/html", "UTF-8", null)
    }
}
