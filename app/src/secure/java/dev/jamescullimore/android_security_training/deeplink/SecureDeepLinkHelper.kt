package dev.jamescullimore.android_security_training.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

class SecureDeepLinkHelper : DeepLinkHelper {

    // Verified app link host
    private val allowedScheme = "https"
    private val allowedHost = "lethalmaus.github.io"
    private val allowedPathPrefixes = listOf("/AndroidSecurityTraining", "/AndroidSecurityTraining/welcome", "/AndroidSecurityTraining/auth/callback")

    override fun describeIncomingIntent(intent: Intent?): String {
        if (intent == null) return "<no intent>"
        val uri = intent.data
        val sb = StringBuilder()
        sb.append("action=").append(intent.action)
        sb.append("\ncategories=").append(intent.categories?.joinToString())
        sb.append("\nuri=").append(uri)
        val ok = uri?.let { validateUri(it) } ?: false
        sb.append("\nvalidated=").append(ok)
        if (ok) {
            val code = uri.getQueryParameter("code")?.let { "<redacted>" } ?: "<none>"
            val state = uri.getQueryParameter("state") ?: "<none>"
            sb.append("\nparams: code=").append(code).append(", state=").append(state)
        }
        return sb.toString()
    }

    override fun handleIncomingIntent(intent: Intent): String {
        val uri = intent.data
        return if (uri != null && validateUri(uri)) {
            val state = uri.getQueryParameter("state")
            if (state.isNullOrBlank()) {
                "Rejected: missing state parameter"
            } else {
                "Accepted: scheme=${uri.scheme} host=${uri.host} path=${uri.path} (code redacted)"
            }
        } else {
            "Rejected: invalid scheme/host/path"
        }
    }

    override fun safeNavigateExample(context: Context, uriString: String): String {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return "Invalid URI"
        return if (validateUri(uri)) {
            // Example: after validation, proceed to internal screen (not implemented here)
            "Navigation allowed to ${uri.path}"
        } else {
            "Navigation blocked: untrusted URI"
        }
    }

    private fun validateUri(uri: Uri): Boolean {
        if (!allowedScheme.equals(uri.scheme, ignoreCase = true)) return false
        if (!allowedHost.equals(uri.host, ignoreCase = true)) return false
        val path = uri.path ?: return false
        return allowedPathPrefixes.any { path.startsWith(it) }
    }
}
