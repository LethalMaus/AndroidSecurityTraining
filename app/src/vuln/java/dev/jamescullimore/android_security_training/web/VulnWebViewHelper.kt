package dev.jamescullimore.android_security_training.web

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
                val i = Intent("dev.jamescullimore.android_security_training.web.DEMO").apply {
                    putExtra("msg", msg)
                    // Target our own app to guarantee delivery during the demo
                    setPackage(context.packageName)
                }
                context.sendBroadcast(i)
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
        // Use cleartext HTTP to demonstrate mixed content and lack of validation; avoids file permission issues
        val url = "http://neverssl.com/"
        webView.loadUrl(url)
        return "[VULN] Loaded untrusted (cleartext) $url"
    }

    override fun runDemoJs(context: Context, webView: WebView): String {
        // Call the JS bridge to exfiltrate the token to page JS
        val js = "(function(){ if(window.Android){ Android.sendBroadcast('exfil:'+Android.leakToken()); return 'leaked'; } else { return 'no-bridge'; } })();"
        webView.evaluateJavascript(js) { value ->
            Log.w("WebDemo", "evaluateJavascript result: $value")
        }
        return "Executed JS that calls Android.leakToken() and broadcasts it (trainers can observe via logs)"
    }

    override fun sendInternalBroadcast(context: Context): String {
        val intent = Intent("dev.jamescullimore.android_security_training.web.DEMO").apply {
            putExtra("msg", "hello-from-vuln")
            // Ensure delivery to our app for the demo while still using exported receiver for training
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
        return "[VULN] Sent broadcast to in-app receiver (receiver remains exported in vuln build for abuse demos)"
    }

    override fun exposePendingIntent(context: Context): String {
        // Create a mutable PendingIntent and expose via a sticky broadcast (anti-pattern)
        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, dev.jamescullimore.android_security_training.WebActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "http://attacker.example/pwn".toUri()
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        leakedPendingIntent = pi
        val leakIntent = Intent("dev.jamescullimore.android_security_training.web.LEAK_PI").apply {
            putExtra("pi", pi)
            // Target our own app to guarantee delivery during the demo
            setPackage(context.packageName)
        }
        context.sendBroadcast(leakIntent)
        return "[VULN] Leaked mutable PendingIntent via broadcast action dev...LEAK_PI"
    }
}
