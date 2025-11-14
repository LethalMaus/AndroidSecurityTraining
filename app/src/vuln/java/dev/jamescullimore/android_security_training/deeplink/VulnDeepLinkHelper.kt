package dev.jamescullimore.android_security_training.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri

/**
 * INTENTIONALLY VULNERABLE: accepts broad inputs, trusts data, and may leak info.
 */
class VulnDeepLinkHelper : DeepLinkHelper {

    override fun describeIncomingIntent(intent: Intent?): String {
        if (intent == null) return "[VULN] <no intent>"
        val uri = intent.data
        val sb = StringBuilder()
        sb.append("[VULN] action=").append(intent.action)
        sb.append("\ncategories=").append(intent.categories?.joinToString())
        sb.append("\nuri=").append(uri)
        if (uri != null) {
            val scheme = uri.scheme ?: "<none>"
            val host = uri.host ?: "<none>"
            val path = uri.path ?: "<none>"
            val token = uri.getQueryParameter("token") ?: "<none>"
            val code = uri.getQueryParameter("code") ?: "<none>"
            val state = uri.getQueryParameter("state") ?: "<none>"

            // VULNERABLE: naive prefix check, does not canonicalize ".." segments
            val prefix = "/AndroidSecurityTraining/open"
            val naiveAccept = path.startsWith(prefix)

            // For demonstration, compute a canonicalized path (not used by the app's decision)
            val canonicalPath = canonicalizePath(path)

            sb.append("\nparsed: scheme=").append(scheme)
                .append(" host=").append(host)
                .append(" path=").append(path)
                .append("\ncanonicalPath=").append(canonicalPath)
                .append("\nnaiveAccept(prefix '$prefix')=").append(naiveAccept)
                .append("\nparams: token=").append(token)
                .append(", code=").append(code)
                .append(", state=").append(state)
        }
        return sb.toString()
    }

    override fun handleIncomingIntent(intent: Intent): String {
        val uri = intent.data
        Log.i("DeepLink", "[VULN] Received: $uri with extras=${intent.extras}")
        return if (uri != null) {
            "[VULN] Accepted ANY (no validation): scheme=${uri.scheme} host=${uri.host} path=${uri.path} token=${uri.getQueryParameter("token")} code=${uri.getQueryParameter("code")}" 
        } else {
            "[VULN] No URI"
        }
    }

    override fun safeNavigateExample(context: Context, uriString: String): String {
        // Unsafe: attempts to launch whatever the URI indicates without validation
        return try {
            val u = uriString.toUri()
            val i = Intent(Intent.ACTION_VIEW, u).addCategory(Intent.CATEGORY_BROWSABLE)
            context.startActivity(i)
            "[VULN] Launched external VIEW intent to $u (no scheme/host checks)"
        } catch (t: Throwable) {
            "[VULN] Launch failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    // Simple path canonicalization (dot-segments) for demonstration purposes only
    private fun canonicalizePath(originalPath: String): String {
        if (originalPath.isEmpty() || originalPath == "<none>") return originalPath
        val out = ArrayDeque<String>()
        val parts = originalPath.split('/')
        for (p in parts) {
            when {
                p.isEmpty() || p == "." -> { /* skip */ }
                p == ".." -> if (out.isNotEmpty()) out.removeLast()
                else -> out.addLast(p)
            }
        }
        return "/" + out.joinToString("/")
    }
}
