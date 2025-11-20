package dev.jamescullimore.android_security_training.web

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri

// Intentionally INSECURE for training only
class VulnWebViewHelper : WebViewHelper {

    private var leakedPendingIntent: PendingIntent? = null

    override fun configure(context: Context, webView: WebView): String {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.allowUniversalAccessFromFileURLs = true
        s.allowFileAccessFromFileURLs = true
        // Add a vulnerable JS bridge exposing a secret
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun leakToken(): String {
                Log.w("WebDemo", "JS bridge leakToken() called")
                return "API_TOKEN_ABC123" // demo secret (intentionally exposed in vuln build for training)
            }

            @JavascriptInterface
            fun sendBroadcast(msg: String) {
                Log.w("WebDemo", "JS bridge sendBroadcast(msg) called with msg=$msg")
                val i = Intent("dev.jamescullimore.android_security_training.DEMO").apply {
                    putExtra("msg", msg)
                    // Target our own app to guarantee delivery during the demo
                    setPackage(context.packageName)
                }
                context.sendBroadcast(i)
            }

            @JavascriptInterface
            fun showToast(message: String) {
                android.widget.Toast
                    .makeText(context, message, android.widget.Toast.LENGTH_LONG)
                    .show()
            }
        }, "Android")

        webView.webViewClient = object : WebViewClient() { /* accept everything by default */ }
        return "[VULN] Configured: JS=ON, addJavascriptInterface(Android.leakToken), file:// access enabled, mixed content allowed, no URL validation"
    }

    override fun loadTrusted(context: Context, webView: WebView): String {
        // Align with secure build so the demo content is reachable
        val url = "https://lethalmaus.github.io/AndroidSecurityTraining/README.md"
        webView.loadUrl(url)
        return "[VULN] Loaded trusted $url"
    }

    override fun loadUntrusted(context: Context, webView: WebView): String {
        // Create a secret file inside internal storage and load it directly via file:// to avoid asset traversal denial
        val secretFile = java.io.File(context.filesDir, "secret.txt")
        try {
            if (!secretFile.exists()) {
                secretFile.writeText("TOP-SECRET: token=lab-" + System.currentTimeMillis())
            }
        } catch (t: Throwable) {
            Log.w("WebDemo", "Failed to create secret.txt in files dir", t)
        }
        val url = "file://${secretFile.absolutePath}"
        webView.settings.allowFileAccess = true
        webView.loadUrl(url)
        return "[VULN] Loaded internal file via file:// URI: $url (file:// access is enabled in vuln build)"
    }

    override fun loadUntrustedHttp(context: Context, webView: WebView): String {
        // Use cleartext HTTP to demonstrate mixed content and lack of validation; avoids file permission issues
        val url = "http://neverssl.com/"
        webView.loadUrl(url)
        return "[VULN] Loaded untrusted (cleartext) $url"
    }

    override fun loadLocalPayload(context: Context, webView: WebView): String {
        configure(context, webView)
        // Prefer app-specific external storage (works without runtime permission on modern Android)
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val payloadFile = java.io.File(baseDir, "payload.html")
        if (!payloadFile.exists()) {
            runCatching {
                payloadFile.writeText(
                    """
                    <!DOCTYPE html>
<html>
<body>
<h1>JS Injection Test</h1>
<script>
    Android.showToast("Owned from JS");
</script>
</body>
</html>
                    """.trimIndent()
                )
            }
        }
        val url = "file://" + payloadFile.absolutePath
        webView.loadUrl(url)
        return "[VULN] Loaded local payload with JS+file access enabled: $url"
    }

    override fun loadFromIntent(context: Context, webView: WebView, url: String): String {
        configure(context, webView)
        webView.loadUrl(url)
        return "[VULN] Loaded from VIEW intent without validation: $url"
    }

    override fun runDemoJs(context: Context, webView: WebView): String {
        // Call the JS bridge to exfiltrate the token to page JS
        configure(context, webView)
        val js = "(function(){ if(window.Android){ Android.sendBroadcast('exfil:'+Android.leakToken()); return 'leaked'; } else { return 'no-bridge'; } })();"
        webView.post {
            webView.evaluateJavascript(js) { value ->
                Log.w("WebDemo", "evaluateJavascript result: $value")
            }
        }
        return "Executed JS that calls Android.leakToken() and broadcasts it (trainers can observe via logs)"
    }

    override fun sendInternalBroadcast(context: Context): String {
        val intent = Intent("dev.jamescullimore.android_security_training.DEMO").apply {
            putExtra("msg", "hello-from-vuln")
            // Ensure delivery to our app for the demo while still using exported receiver for training
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
        return "[VULN] Sent broadcast to in-app receiver (receiver remains exported in vuln build for abuse demos)"
    }

    override fun exposePendingIntent(context: Context): String {
        // Create a mutable PendingIntent and expose via a sticky broadcast (anti-pattern)
        // Avoid hard compile-time reference to WebActivity (exists only in 'web' topic source set)
        val attackUri = "http://attacker.example/pwn".toUri()
        val i = Intent(Intent.ACTION_VIEW, attackUri)

        // If the WebActivity exists in this variant, target it explicitly; otherwise fall back to generic VIEW
        val webActivityClass = "dev.jamescullimore.android_security_training.WebActivity"
        val pm = context.packageManager
        val cn = android.content.ComponentName(context.packageName, webActivityClass)
        val hasWebActivity = try {
            pm.getActivityInfo(cn, 0)
            true
        } catch (t: Throwable) {
            false
        }
        if (hasWebActivity) {
            i.setClassName(context.packageName, webActivityClass)
        }

        val pi = PendingIntent.getActivity(
            context,
            0,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        leakedPendingIntent = pi
        val leakIntent = Intent("dev.jamescullimore.android_security_training.LEAK_PI").apply {
            putExtra("pi", pi)
            // Target our own app to guarantee delivery during the demo
            setPackage(context.packageName)
        }
        context.sendBroadcast(leakIntent)
        return "[VULN] Leaked mutable PendingIntent via broadcast action dev...LEAK_PI"
    }
}
