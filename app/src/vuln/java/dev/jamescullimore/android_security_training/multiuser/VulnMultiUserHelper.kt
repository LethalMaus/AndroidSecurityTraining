package dev.jamescullimore.android_security_training.multiuser

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import java.io.File
import androidx.core.content.edit

// Intentionally insecure patterns for training ONLY
class VulnMultiUserHelper : MultiUserHelper {

    private fun suExec(cmd: String): Pair<Int, String> {
        return try {
            val process = ProcessBuilder("su", "--mount-master", "-c", cmd)
                .redirectErrorStream(true)
                .start()

            val out = process.inputStream.bufferedReader().use { it.readText() }
            process.exitValue() to out.trim()
        } catch (t: Throwable) {
            -2 to "[root-demo] su failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

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
        // On rooted device, list device users via shell
        val (code1, out1) = suExec("cmd user list || pm list users")
        return if (code1 == 0 && out1.isNotBlank()) {
            "[VULN][root-only demo] cmd user list:\n$out1"
        } else {
            "[VULN] Could not list users via root shell: exit=$code1 output='$out1'\nTip: Use a non-Google Play emulator and run adb root."
        }
    }

    override fun savePerUserToken(context: Context, token: String): String {
        // Bad: stores in plaintext SharedPreferences with a constant key (no per-user scoping).
        val prefs = context.getSharedPreferences("tokens_plain", Context.MODE_PRIVATE)
        prefs.edit { putString("token", token) }
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
        // Root-only teaching aid: attempt to read another user's SharedPreferences via shell
        val pkg = context.packageName
        val path = "/data/user/$targetUserId/$pkg/shared_prefs/tokens_plain.xml"
        val (code, out) = suExec("cat $path")
        return when {
            code == 0 && out != "__NO_FILE__" && out.isNotBlank() -> {
                val snippet = if (out.length > 600) out.take(600) + "\n…(truncated)…" else out
                "[VULN][root-only demo] Cross-user read of $path:\n$snippet"
            }
            code == 0 && out == "__NO_FILE__" -> "[VULN][root-only demo] Target file not found for user $targetUserId: $path"
            else -> "[VULN] Root shell failed to read $path (exit=$code): $out"
        }
    }
}
