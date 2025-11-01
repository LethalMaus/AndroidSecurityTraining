package dev.jamescullimore.android_security_training.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

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
            val code = uri.getQueryParameter("code") ?: "<none>"
            val state = uri.getQueryParameter("state") ?: "<none>"
            sb.append("\nparams: code=").append(code).append(", state=").append(state)
        }
        return sb.toString()
    }

    override fun handleIncomingIntent(intent: Intent): String {
        val uri = intent.data
        Log.i("DeepLink", "[VULN] Received: $uri with extras=${intent.extras}")
        return if (uri != null) {
            "[VULN] Accepted ANY (no validation): scheme=${uri.scheme} host=${uri.host} path=${uri.path} code=${uri.getQueryParameter("code")}" 
        } else {
            "[VULN] No URI"
        }
    }

    override fun safeNavigateExample(context: Context, uriString: String): String {
        // Unsafe: attempts to launch whatever the URI indicates without validation
        return try {
            val u = Uri.parse(uriString)
            val i = Intent(Intent.ACTION_VIEW, u).addCategory(Intent.CATEGORY_BROWSABLE)
            context.startActivity(i)
            "[VULN] Launched external VIEW intent to $u (no scheme/host checks)"
        } catch (t: Throwable) {
            "[VULN] Launch failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }
}
