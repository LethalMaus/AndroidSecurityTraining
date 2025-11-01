package dev.jamescullimore.android_security_training.web

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri

class SecureWebViewHelper : WebViewHelper {

    override fun configure(context: Context, webView: WebView): String {
        val s = webView.settings
        s.javaScriptEnabled = true // enable only if needed for the demo
        s.allowFileAccess = false
        s.allowContentAccess = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webView.settings.safeBrowsingEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url ?: return true
                return !isAllowed(u)
            }
            @Deprecated("Deprecated in API 24")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return !isAllowed(url?.toUri())
            }
            override fun onSafeBrowsingHit(
                view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?
            ) {
                // Show interstitial and block
                callback?.backToSafety(true)
            }
        }
        return "[SECURE] WebView configured: JS=on, fileAccess=off, mixedContent=never, SafeBrowsing=on, host allowlist enforced"
    }

    private fun isAllowed(uri: Uri?): Boolean {
        if (uri == null) return false
        if (uri.scheme != "https") return false
        val allowedHosts = setOf("jamescullimore.dev", "lethalmaus.github.io", "github.com")
        val host = uri.host ?: return false
        return allowedHosts.contains(host)
    }

    override fun loadTrusted(context: Context, webView: WebView): String {
        // Primary trusted URL migrated to GitHub Pages as jamescullimore.dev may be unavailable.
        val url = "https://lethalmaus.github.io/AndroidSecurityTraining/README.md"
        webView.loadUrl(url)
        return "Loaded trusted $url"
    }

    override fun loadUntrusted(context: Context, webView: WebView): String {
        // In secure builds, demonstrate blocking by attempting to load http URL which should be blocked by client.
        val url = "http://neverssl.com/"
        webView.loadUrl(url)
        return "Attempted to load untrusted $url (should be blocked by shouldOverrideUrlLoading)"
    }

    override fun runDemoJs(context: Context, webView: WebView): String {
        // Demonstrate safe JS call to display message, no native bridge exposed.
        Log.i("WebDemo", "Secure runDemoJs() invoked: no bridge exposed, executing harmless JS")
        webView.evaluateJavascript("alert('Hello from safe JS (no native bridge)')") { value ->
            Log.i("WebDemo", "evaluateJavascript (secure) result: $value")
        }
        return "Executed harmless JS (no addJavascriptInterface)"
    }

    override fun sendInternalBroadcast(context: Context): String {
        val intent = Intent(ACTION_DEMO).apply {
            setPackage(context.packageName)
            putExtra("msg", "hello-internal")
        }
        context.sendBroadcast(intent)
        return "Sent internal broadcast (restricted by setPackage; receiver not exported in secure builds)"
    }

    override fun exposePendingIntent(context: Context): String {
        // Secure: use immutable and do not expose outside
        PendingIntent.getActivity(
            context, 0,
            android.content.Intent(Intent.ACTION_VIEW,
                "https://lethalmaus.github.io/AndroidSecurityTraining/".toUri()),
            PendingIntent.FLAG_IMMUTABLE
        )
        return "Created immutable PendingIntent (not exposed)"
    }

    companion object {
        const val ACTION_DEMO = "dev.jamescullimore.android_security_training.web.DEMO"
    }
}
