package dev.jamescullimore.android_security_training.root

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class SecureRootHelper : RootHelper {

    override fun getSignals(context: Context): List<RootHelper.RootSignal> {
        val signals = mutableListOf<RootHelper.RootSignal>()

        // su binary paths
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/vendor/bin/su", "/system/su"
        )
        val suFound = suPaths.any { File(it).exists() }
        signals += RootHelper.RootSignal("su binary present", suFound, if (suFound) suPaths.filter { File(it).exists() }.joinToString() else null)

        // BusyBox presence (heuristic)
        val busyBoxPaths = listOf("/system/xbin/busybox", "/system/bin/busybox", "/magisk/.core/busybox")
        val busyFound = busyBoxPaths.any { File(it).exists() }
        signals += RootHelper.RootSignal("busybox present", busyFound, if (busyFound) busyBoxPaths.filter { File(it).exists() }.joinToString() else null)

        // Magisk traces
        val magiskPaths = listOf("/sbin/.magisk", "/data/adb/magisk", "/cache/.magisk", "/data/adb/modules")
        val magiskFound = magiskPaths.any { File(it).exists() }
        signals += RootHelper.RootSignal("magisk traces", magiskFound, if (magiskFound) magiskPaths.filter { File(it).exists() }.joinToString() else null)

        // Build tags
        val testKeys = (Build.TAGS ?: "").contains("test-keys", ignoreCase = true) || (Build.FINGERPRINT ?: "").contains("test-keys", true)
        signals += RootHelper.RootSignal("build tags test-keys", testKeys, null)

        // SELinux enforcing (best-effort)
        val selinuxEnforceFile = File("/sys/fs/selinux/enforce")
        val selinuxEnforcing = try {
            if (selinuxEnforceFile.exists()) FileInputStream(selinuxEnforceFile).use { it.read() == '1'.code } else null
        } catch (_: Throwable) { null }
        if (selinuxEnforcing != null) {
            signals += RootHelper.RootSignal("SELinux enforcing", selinuxEnforcing, null)
        }

        // System mount rw (very heuristic)
        val mountsRw = try {
            val mounts = File("/proc/mounts").takeIf { it.exists() }?.readText().orEmpty()
            mounts.lines().any { line ->
                (" /system " in line || line.startsWith("/system ")) && line.contains(" rw,")
            }
        } catch (_: Throwable) { false }
        signals += RootHelper.RootSignal("/system mounted rw", mountsRw, null)

        return signals
    }

    override fun isRooted(context: Context): Boolean {
        val sigs = getSignals(context)
        // Consider rooted if any strong indicator present
        return sigs.any { s ->
            when (s.name) {
                "su binary present", "magisk traces", "/system mounted rw" -> s.detected
                else -> false
            }
        }
    }

    override fun deviceInfo(): String = "SDK=${Build.VERSION.SDK_INT}; brand=${Build.BRAND}; model=${Build.MODEL}; tags=${Build.TAGS}"

    override fun playIntegrityStatus(context: Context): String = "Play Integrity: planned for later integration (placeholder)"

    override fun tamperCheck(context: Context): Boolean {
        // Compare signing cert digest against expected (Base64 NO_WRAP)
        val expected = EXPECTED_SIGNER_DIGEST_B64
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val certArray: Array<android.content.pm.Signature> = if (Build.VERSION.SDK_INT >= 28) {
                val si = info.signingInfo
                (si?.apkContentsSigners ?: emptyArray())
            } else {
                (info.signatures ?: emptyArray())
            }
            val md = MessageDigest.getInstance("SHA-256")
            val digests = certArray.map { cert -> md.digest(cert.toByteArray()) }
            val b64 = digests.joinToString("|") { d -> Base64.encodeToString(d, Base64.NO_WRAP) }
            b64.split('|').any { it == expected }
        } catch (_: Throwable) {
            false
        }
    }

    override fun setBypassEnabled(enabled: Boolean) {
        // No-op in secure builds
    }

    companion object {
        // Signer cert SHA-256 digest (Base64 NO_WRAP)
        private const val EXPECTED_SIGNER_DIGEST_B64 = "Plazc2oWHYXXVf8ZXUPiLS9fBySu3GhTc0qg/fTy+/I="
    }
}