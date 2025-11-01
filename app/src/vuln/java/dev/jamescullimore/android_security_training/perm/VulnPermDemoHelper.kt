package dev.jamescullimore.android_security_training.perm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process

class VulnPermDemoHelper : PermDemoHelper {
    override fun uidGidAndSignatureInfo(context: Context): String {
        // Minimal info; no signing digest calculation
        return "PID=${Process.myPid()} UID=${Process.myUid()}\npackage=${context.packageName}\n(signing digest not checked)"
    }

    override fun tryStartProtectedService(context: Context): String {
        return try {
            // Tries to start service explicitly; in vuln builds components may be exported without protection
            val intent = Intent()
            intent.setClassName(context, "dev.jamescullimore.android_security_training.perm.DemoService")
            val cn = context.startService(intent)
            "startService result: $cn"
        } catch (t: Throwable) {
            "startService error: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun tryQueryDemoProvider(context: Context, uri: String): String {
        return try {
            val u = Uri.parse(uri)
            context.contentResolver.query(u, null, null, null, null)?.use { c ->
                val rows = c.count
                "query ok: rows=$rows"
            } ?: "query returned null"
        } catch (t: Throwable) {
            "query error: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun defaultDemoUri(context: Context): String = "content://${context.packageName}.demo/hello"
}
