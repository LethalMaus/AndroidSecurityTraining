package dev.jamescullimore.android_security_training.root

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.scottyab.rootbeer.RootBeer
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest


class SecureRootHelper : RootHelper {

    override fun getSignals(context: Context): List<RootHelper.RootSignal> {
        val signals = mutableListOf<RootHelper.RootSignal>()

        // Emulator hints (helps explain why adb has root but apps don't)
        val roQemu = getProp("ro.kernel.qemu")
        val hw = (getProp("ro.hardware") ?: "")
        val emulator = (roQemu == "1") || hw.contains("goldfish", true) || hw.contains("ranchu", true) || hw.contains("qemu", true)
        val adbdRoot = getProp("service.adb.root") // often "1" on emulators when adbd runs as root
        signals += RootHelper.RootSignal("emulator (qemu)", emulator, "ro.kernel.qemu=$roQemu, ro.hardware=$hw")
        if (!adbdRoot.isNullOrBlank()) {
            signals += RootHelper.RootSignal("adbd root (emulator)", adbdRoot == "1", "service.adb.root=$adbdRoot")
        }

        // su binary common paths (expanded set)
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/vendor/bin/su", "/system/su", "/system_ext/bin/su", "/magisk/.core/bin/su",
            "/data/adb/magisk/busybox/su", "/data/adb/ksu/bin/su", "/apex/com.android.runtime/bin/su"
        )
        val presentSu = suPaths.filter { pathExists(it) }
        val suFound = presentSu.isNotEmpty()
        signals += RootHelper.RootSignal(
            "su binary present",
            suFound,
            if (suFound) presentSu.joinToString() else null
        )

        // su found via PATH lookup (which su)
        val suInPath = findInPath("su") ?: shellWhich("su")
        signals += RootHelper.RootSignal(
            "su in PATH",
            suInPath != null,
            suInPath
        )

        // Try executing su to see if we can get root (best-effort)
        val (canSu, suDetails) = canUseSu()
        signals += RootHelper.RootSignal("can execute su", canSu, suDetails)

        // If su appears present but exec fails on an emulator, explain likely adbd-root-only scenario
        if ((suFound || suInPath != null) && !canSu && emulator) {
            val where = (presentSu + listOfNotNull(suInPath)).distinct().joinToString().ifBlank { "unknown" }
            signals += RootHelper.RootSignal(
                "su exec denied (likely adbd-root-only emulator)",
                true,
                "su at: $where; service.adb.root=${adbdRoot ?: "<n/a>"}"
            )
        }

        // Integrate RootBeer library checks (additional heuristics)
        try {
            val rb = RootBeer(context)
            val rbIsRooted = runCatching { rb.isRooted }.getOrDefault(false)
            val rbIsRootedNoBB = runCatching { rb.isRootedWithoutBusyBoxCheck }.getOrDefault(false)
            val rbNative = runCatching { rb.checkForRootNative() }.getOrDefault(false)
            val rbSu = runCatching { rb.checkForSuBinary() }.getOrDefault(false)
            val rbBusy = runCatching { rb.checkForBusyBoxBinary() }.getOrDefault(false)
            val rbRW = runCatching { rb.checkForRWPaths() }.getOrDefault(false)
            val rbDangerProps = runCatching { rb.checkForDangerousProps() }.getOrDefault(false)
            val rbDangerApps = runCatching { rb.detectPotentiallyDangerousApps() }.getOrDefault(false)
            val rbRootMgrApps = runCatching { rb.detectRootManagementApps() }.getOrDefault(false)
            val rbTestKeys = runCatching { rb.detectTestKeys() }.getOrDefault(false)
            val rbMagisk = runCatching { rb.checkForMagiskBinary() }.getOrDefault(false)

            signals += RootHelper.RootSignal("rootbeer: isRooted", rbIsRooted, null)
            signals += RootHelper.RootSignal("rootbeer: isRootedWithoutBusyBoxCheck", rbIsRootedNoBB, null)
            signals += RootHelper.RootSignal("rootbeer: native check", rbNative, null)
            signals += RootHelper.RootSignal("rootbeer: su binary", rbSu, null)
            signals += RootHelper.RootSignal("rootbeer: busybox binary", rbBusy, null)
            signals += RootHelper.RootSignal("rootbeer: rw paths", rbRW, null)
            signals += RootHelper.RootSignal("rootbeer: dangerous props", rbDangerProps, null)
            signals += RootHelper.RootSignal("rootbeer: dangerous apps", rbDangerApps, null)
            signals += RootHelper.RootSignal("rootbeer: root mgmt apps", rbRootMgrApps, null)
            signals += RootHelper.RootSignal("rootbeer: test-keys", rbTestKeys, null)
            signals += RootHelper.RootSignal("rootbeer: magisk binary", rbMagisk, null)
        } catch (_: Throwable) {
            signals += RootHelper.RootSignal("rootbeer: unavailable", false, "library not present or init failed")
        }

        // BusyBox presence (heuristic)
        val busyBoxPaths = listOf(
            "/system/xbin/busybox", "/system/bin/busybox", "/magisk/.core/busybox",
            "/data/adb/magisk/busybox/busybox"
        )
        val presentBusy = busyBoxPaths.filter { File(it).exists() }
        val busyFound = presentBusy.isNotEmpty()
        signals += RootHelper.RootSignal("busybox present", busyFound, if (busyFound) presentBusy.joinToString() else null)

        // Magisk traces
        val magiskPaths = listOf(
            "/sbin/.magisk", "/data/adb/magisk", "/cache/.magisk", "/data/adb/modules",
            "/data/adb/service.d", "/cache/magisk.log", "/data/adb/post-fs-data.d"
        )
        val presentMagisk = magiskPaths.filter { File(it).exists() }
        val magiskFound = presentMagisk.isNotEmpty()
        signals += RootHelper.RootSignal("magisk traces", magiskFound, if (magiskFound) presentMagisk.joinToString() else null)

        // Known root management packages installed (best-effort)
        val rootPkgs = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.kingroot.kinguser",
            "com.thirdparty.superuser"
        )
        val installed = rootPkgs.filter { isInstalled(context, it) }
        signals += RootHelper.RootSignal("root mgmt packages", installed.isNotEmpty(), if (installed.isNotEmpty()) installed.joinToString() else null)

        // Build tags
        val testKeys = (Build.TAGS ?: "").contains("test-keys", ignoreCase = true) || (Build.FINGERPRINT ?: "").contains("test-keys", true)
        signals += RootHelper.RootSignal("build tags test-keys", testKeys, null)

        // System properties
        val roSecure = getProp("ro.secure")
        val roDebuggable = getProp("ro.debuggable")
        if (roSecure != null) signals += RootHelper.RootSignal("ro.secure == 0", roSecure == "0", "ro.secure=$roSecure")
        if (roDebuggable != null) signals += RootHelper.RootSignal("ro.debuggable == 1", roDebuggable == "1", "ro.debuggable=$roDebuggable")

        // SELinux enforcing (best-effort)
        val selinuxEnforceFile = File("/sys/fs/selinux/enforce")
        val selinuxEnforcing = try {
            if (selinuxEnforceFile.exists()) FileInputStream(selinuxEnforceFile).use { it.read() == '1'.code } else null
        } catch (_: Throwable) { null }
        if (selinuxEnforcing != null) {
            signals += RootHelper.RootSignal("SELinux enforcing", selinuxEnforcing, null)
        }

        // /system mounted rw (heuristic)
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
                "can execute su",
                "su binary present",
                "su in PATH",
                "magisk traces",
                "root mgmt packages",
                "ro.secure == 0",
                "/system mounted rw",
                // Consider RootBeer aggregate/native checks as strong indicators too
                "rootbeer: isRooted",
                "rootbeer: native check" -> s.detected
                else -> false
            }
        } || (sigs.find { it.name == "ro.debuggable == 1" }?.detected == true && (Build.TAGS ?: "").contains("test-keys", true))
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
            val certArray: Array<android.content.pm.Signature> = (info.signingInfo?.apkContentsSigners ?: emptyArray())
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

    private fun findInPath(binary: String): String? {
        return try {
            val path = System.getenv("PATH") ?: return null
            path.split(':').firstNotNullOfOrNull { dir ->
                val f = File(dir, binary)
                if (f.exists()) f.absolutePath else null
            }
        } catch (_: Throwable) { null }
    }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Throwable) { false }

    private fun getProp(name: String): String? = try {
        val p = Runtime.getRuntime().exec(arrayOf("getprop", name))
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        out.ifBlank { null }
    } catch (_: Throwable) { null }

    /** Try executing su to check if a root shell is available. Returns Pair<detected, details>. */
    private fun canUseSu(): Pair<Boolean, String?> {
        fun tryCmd(vararg cmd: String): Pair<Boolean, String?> {
            return try {
                val p = Runtime.getRuntime().exec(cmd)
                val exit = p.waitFor()
                val out = p.inputStream.bufferedReader().readText()
                val err = p.errorStream.bufferedReader().readText()
                val all = (out + "\n" + err).trim()
                if (exit == 0 && (out.contains("uid=0") || out.contains("root") || all.contains("uid=0")))
                    true to "${cmd.joinToString(" ")} => $all"
                else false to "${cmd.joinToString(" ")} (exit=$exit) => $all"
            } catch (t: Throwable) {
                false to "${cmd.joinToString(" ")} failed: ${t.javaClass.simpleName}: ${t.message}"
            }
        }
        val attempts = listOf(
            arrayOf("su", "-c", "id"),
            arrayOf("su", "0", "id"),
            arrayOf("su", "-c", "whoami")
        )
        for (a in attempts) {
            val r = tryCmd(*a)
            if (r.first) return r
        }
        // Try interactive su session: write commands to stdin
        try {
            val p = ProcessBuilder("su").redirectErrorStream(true).start()
            p.outputStream.bufferedWriter().use { w ->
                w.write("id\n")
                w.write("exit\n")
                w.flush()
            }
            val out = p.inputStream.bufferedReader().readText()
            val exit = p.waitFor()
            return if (exit == 0 && (out.contains("uid=0") || out.contains("root"))) {
                true to "interactive su => $out"
            } else {
                false to "interactive su (exit=$exit) => $out"
            }
        } catch (t: Throwable) {
            // fallthrough with diagnostic
        }
        return false to attempts.joinToString(" | ") { it.joinToString(" ") }
    }

    // Fallback existence check using shell in case File.exists() is constrained by namespaces
    private fun pathExists(path: String): Boolean {
        return try {
            if (File(path).exists()) return true
            val res = runShell("[ -e \"$path\" ] && echo yes || echo no")
            res.second.contains("yes")
        } catch (_: Throwable) { false }
    }

    // Execute a small shell command and return Triple(exitCode, stdout, stderr)
    private fun runShell(cmd: String): Triple<Int, String, String> {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val exit = p.waitFor()
            Triple(exit, out, err)
        } catch (t: Throwable) {
            Triple(-1, "", t.message ?: "")
        }
    }

    // Use the shell PATH to locate a binary (more reliable than app env PATH on some devices)
    private fun shellWhich(binary: String): String? {
        val (exit, out, _) = runShell("command -v $binary || which $binary")
        val path = out.lineSequence().firstOrNull { it.contains('/') }?.trim()
        return if (exit == 0 && !path.isNullOrBlank()) path else null
    }

    companion object {
        // Signer cert SHA-256 digest (Base64 NO_WRAP)
        private const val EXPECTED_SIGNER_DIGEST_B64 = "Plazc2oWHYXXVf8ZXUPiLS9fBySu3GhTc0qg/fTy+/I="
    }
}