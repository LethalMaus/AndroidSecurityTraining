package dev.jamescullimore.android_security_training.web

import android.content.Context
import android.webkit.WebView

interface WebViewHelper {
    data class State(
        val notes: String = "",
        val lastJsMessage: String? = null,
        val lastBroadcast: String? = null,
        val pendingIntentInfo: String? = null,
    )

    fun configure(context: Context, webView: WebView): String
    fun loadTrusted(context: Context, webView: WebView): String
    fun loadUntrusted(context: Context, webView: WebView): String
    fun runDemoJs(context: Context, webView: WebView): String
    fun sendInternalBroadcast(context: Context): String
    fun exposePendingIntent(context: Context): String
}
