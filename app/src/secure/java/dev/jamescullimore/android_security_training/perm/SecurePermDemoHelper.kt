package dev.jamescullimore.android_security_training.perm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.util.Base64
import java.security.MessageDigest
import androidx.core.net.toUri

class SecurePermDemoHelper : PermDemoHelper {
    override fun uidGidAndSignatureInfo(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val signingBytes: ByteArray? = try {
            if (Build.VERSION.SDK_INT >= 28) {
                @Suppress("DEPRECATION")
                val pInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                pInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val pInfo = pm.getPackageInfo(pkg, 0)
                @Suppress("DEPRECATION")
                pInfo.signatures?.firstOrNull()?.toByteArray()
            }
        } catch (t: Throwable) {
            null
        }
        val shaB64 = signingBytes?.let {
            val sha = MessageDigest.getInstance("SHA-256").digest(it)
            Base64.encodeToString(sha, Base64.NO_WRAP)
        } ?: "<unknown>"
        return "PID=${Process.myPid()} UID=${Process.myUid()}\npackage=$pkg\nsigningSHA256(B64)=$shaB64"
    }

    override fun tryStartProtectedService(context: Context): String {
        return try {
            val intent = Intent()
            intent.setClassName(context, "dev.jamescullimore.android_security_training.perm.DemoService")
            val cn = context.startService(intent)
            "startService result: $cn (expected: may fail for external callers; internal allowed)"
        } catch (t: Throwable) {
            "startService error: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun tryQueryDemoProvider(context: Context, uri: String): String {
        return try {
            val u = uri.toUri()
            context.contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val valIdx = c.getColumnIndex("value")
                    val msg = if (valIdx >= 0) c.getString(valIdx) else "row count=${c.count}"
                    "query ok: $msg"
                } else {
                    "query ok: empty"
                }
            } ?: "query returned null"
        } catch (t: Throwable) {
            "query error: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun defaultDemoUri(context: Context): String = "content://${context.packageName}.demo/hello"
}
