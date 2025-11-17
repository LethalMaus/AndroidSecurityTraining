package dev.jamescullimore.android_security_training

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * BroadcastReceiver to clear local app data for labs/demos.
 *
 * Action: dev.jamescullimore.android_security_training.ACTION_CLEAR_DATA
 *
 * Secure defaults (in main manifest): not exported and gated by a signature permission.
 * Vulnerable flavor overrides: exported=true and no permission so trainers can trigger from adb.
 */
class ClearDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != "dev.jamescullimore.android_security_training.ACTION_CLEAR_DATA") return
        val what = intent.getStringExtra("what") ?: "all"
        val summary = clearData(context, what)
        Log.w(TAG, "ACTION_CLEAR_DATA executed: $summary")
        Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
    }

    private fun clearData(context: Context, what: String): String {
        var cleared = 0
        var failed = 0
        fun File.safeDeleteRecursively(label: String) {
            try {
                if (exists()) {
                    deleteRecursively()
                    cleared++
                    Log.i(TAG, "Cleared $label: $absolutePath")
                }
            } catch (t: Throwable) {
                failed++
                Log.w(TAG, "Failed to clear $label: $absolutePath -> ${t.javaClass.simpleName}: ${t.message}")
            }
        }

        val dataDir = File(context.applicationInfo.dataDir)
        val prefsDir = File(dataDir, "shared_prefs")
        val filesDir = context.filesDir
        val cacheDir = context.cacheDir
        val dbDir = File(dataDir, "databases")

        when (what.lowercase()) {
            "prefs" -> prefsDir.safeDeleteRecursively("shared_prefs")
            "files" -> filesDir.safeDeleteRecursively("files")
            "cache" -> cacheDir.safeDeleteRecursively("cache")
            "db", "database", "databases" -> dbDir.safeDeleteRecursively("databases")
            else -> {
                prefsDir.safeDeleteRecursively("shared_prefs")
                filesDir.safeDeleteRecursively("files")
                cacheDir.safeDeleteRecursively("cache")
                dbDir.safeDeleteRecursively("databases")
            }
        }

        return "Cleared $cleared area(s), $failed failed. what=$what"
    }

    companion object {
        private const val TAG = "ClearDataReceiver"
    }
}
