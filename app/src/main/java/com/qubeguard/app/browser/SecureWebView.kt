package com.qubeguard.app.browser

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewFeature
import com.qubeguard.app.data.blocklist.DeterministicBlocker
import com.qubeguard.app.ml.TfLiteClassifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A hardened WebView with privacy and security protections.
 * Blocks ads, trackers, and other unwanted content.
 */
@AndroidEntryPoint
class SecureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webView
) : WebView(context, attrs, defStyleAttr) {

    @Inject
    lateinit var deterministicBlocker: DeterministicBlocker

    @Inject
    lateinit var tfLiteClassifier: TfLiteClassifier

    private var qubeId: String = "default"

    init {
        initializeWebView()
    }

    /**
     * Initializes the WebView with security and privacy settings.
     */
    private fun initializeWebView() {
        // Enable JavaScript (can be toggled per-site)
        settings.javaScriptEnabled = true

        // Disable WebGL to prevent fingerprinting
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DISABLE_WEBGL)) {
            settings.setWebGlEnabled(false)
        }

        // Disable geolocation
        settings.setGeolocationEnabled(false)

        // Disable JavaScript popups
        settings.javaScriptCanOpenWindowsAutomatically = false

        // Disable DOM storage
        settings.domStorageEnabled = false

        // Disable database storage
        settings.databaseEnabled = false

        // Disable save form data
        settings.saveFormData = false

        // Disable text zoom
        settings.textZoom = 100

        // Set user agent to a generic one
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"

        // Set a custom WebViewClient for intercepting requests
        webViewClient = SecureWebViewClient(deterministicBlocker, tfLiteClassifier)

        // Disable third-party cookies
        if (WebViewFeature.isFeatureSupported(WebViewFeature.THIRD_PARTY_COOKIES)) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        }

        // Clear cache and cookies on initialization
        clearCache(true)
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }

    /**
     * Sets the Qube ID for this WebView (for isolation).
     */
    fun setQubeId(qubeId: String) {
        this.qubeId = qubeId
    }

    /**
     * Gets the Qube ID for this WebView.
     */
    fun getQubeId(): String {
        return qubeId
    }

    /**
     * Clears all data (cache, cookies, history) for this WebView.
     */
    fun clearAllData() {
        clearCache(true)
        clearHistory()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }

    /**
     * Custom WebViewClient for intercepting and blocking requests.
     */
    inner class SecureWebViewClient(
        private val deterministicBlocker: DeterministicBlocker,
        private val tfLiteClassifier: TfLiteClassifier
    ) : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
            val url = request?.url?.toString() ?: return null

            // Check if the URL is blocked by the deterministic blocker (Layer 1)
            if (deterministicBlocker.isBlocked(url)) {
                return createBlockedResponse()
            }

            // Check if the URL is blocked by the ML classifier (Layer 3)
            if (tfLiteClassifier.isBlocked(url)) {
                return createBlockedResponse()
            }

            return null // Allow the request
        }

        /**
         * Creates a blocked response (empty HTML page).
         */
        private fun createBlockedResponse(): android.webkit.WebResourceResponse {
            return android.webkit.WebResourceResponse(
                "text/html",
                "UTF-8",
                null
            )
        }
    }
}
