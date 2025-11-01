package dev.jamescullimore.android_security_training.multiuser

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import java.io.File

// Intentionally insecure patterns for training ONLY
class VulnMultiUserHelper : MultiUserHelper {

    override fun getRuntimeInfo(context: Context): String {
        val uid = Process.myUid()
        val appId = uid % 100000
        val userId = uid / 100000
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val device = "SDK=${Build.VERSION.SDK_INT}; brand=${Build.BRAND}; model=${Build.MODEL}"
        return buildString {
            appendLine("Runtime Info (vuln): uid=$uid (userId=$userId appId=$appId)")
            appendLine("ANDROID_ID=$androidId — used wrongly as a global key in this build")
            appendLine(device)
            appendLine("WARNING: This build writes tokens to shared external storage and uses same keys across users.")
        }
    }

    override fun listUsersBestEffort(context: Context): String {
        // Pretend to list users; in reality, we cannot without privileges. Show misleading behavior.
        return "[VULN] Attempted to list users without privileges — returned none. (Attackers may assume single-user and mis-scope data.)"
    }

    override fun savePerUserToken(context: Context, token: String): String {
        // Bad: stores in plaintext SharedPreferences with a constant key (no per-user scoping).
        val prefs = context.getSharedPreferences("tokens_plain", Context.MODE_PRIVATE)
        prefs.edit().putString("token", token).apply()
        return "[VULN] Saved token in plaintext SharedPreferences under constant key 'token' (not per-user scoped)"
    }

    override fun loadPerUserToken(context: Context): String {
        val prefs = context.getSharedPreferences("tokens_plain", Context.MODE_PRIVATE)
        val t = prefs.getString("token", null)
        return t?.let { "[VULN] Loaded token (plaintext, globally keyed): $it" } ?: "No token"
    }

    override fun saveGlobalTokenInsecure(context: Context, token: String): String {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        publicDir.mkdirs()
        val file = File(publicDir, "global_token.txt")
        file.writeText(token)
        return "[VULN] Wrote GLOBAL token in public Downloads: ${file.absolutePath}"
    }

    override fun loadGlobalTokenInsecure(context: Context): String {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "global_token.txt")
        return if (file.exists()) "[VULN] Read GLOBAL token: ${file.readText()}" else "No global token file"
    }

    override fun tryCrossUserRead(context: Context, targetUserId: Int): String {
        // Naively attempt to read another user's app-internal file path (will fail or read current user only)
        val path = "/data/user/$targetUserId/${context.packageName}/shared_prefs/tokens_plain.xml"
        return try {
            val text = File(path).takeIf { it.exists() }?.readText()
            if (text != null) "[VULN] Attempted direct path read of other user: found content length=${text.length} (this should not be possible on secure devices)" else "[VULN] File not found for other user (as expected)"
        } catch (t: Throwable) {
            "[VULN] Cross-user file read failed: ${t::class.java.simpleName}: ${t.message}"
        }
    }
}
