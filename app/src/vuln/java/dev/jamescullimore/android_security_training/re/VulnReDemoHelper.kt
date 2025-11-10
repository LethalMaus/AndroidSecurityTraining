package dev.jamescullimore.android_security_training.re

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.net.Uri
import dalvik.system.DexClassLoader
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * INTENTIONALLY VULNERABLE: Demonstrates secrets in code, asset leakage, and unsafe dynamic code loading.
 */
class VulnReDemoHelper : ReDemoHelper {

    // Obvious hardcoded secret for students to find via JADX/apktool (intentionally present in vuln build)
    private val SUPER_SECRET_API_KEY = "sk_live_REPLACE_ME_123456"

    override fun getHardcodedSecret(): String = SUPER_SECRET_API_KEY

    override fun readLeakyAsset(context: Context): String = try {
        context.assets.open("sensitive.txt").use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (t: Throwable) {
        "Asset missing: ${t.message}"
    }

    override suspend fun tryDynamicDexLoad(context: Context, dexOrJarPath: String): String {
        return runCatching {
            // Accept only a full path or the special keyword 'self'. Keep it simple.
            val raw = dexOrJarPath.trim().trim('"', '\'')
            val normalized = if (raw.startsWith("file://", ignoreCase = true)) {
                Uri.parse(raw).path ?: raw
            } else raw

            val path = if (normalized.equals("self", ignoreCase = true)) context.packageCodePath else normalized

            // Resolve the file and try common sdcard alias if initial lookup fails
            var src = File(path)
            if (!src.exists()) {
                val alt = when {
                    path.startsWith("/sdcard/") -> path.replaceFirst("/sdcard/", "/storage/emulated/0/")
                    path.startsWith("/storage/emulated/0/") -> path.replaceFirst("/storage/emulated/0/", "/sdcard/")
                    else -> null
                }
                if (alt != null) {
                    val altFile = File(alt)
                    if (altFile.exists()) src = altFile
                }
            }

            if (!src.exists()) return@runCatching "Dynamic load failed: File not found at ${src.absolutePath}"
            if (!src.isFile) return@runCatching "Dynamic load failed: Not a file: ${src.absolutePath}"

            val optDir = File(context.codeCacheDir, "dexopt").apply { mkdirs() }
            val cl = DexClassLoader(src.absolutePath, optDir.absolutePath, null, context.classLoader)

            // Self-load demo
            if (normalized.equals("self", ignoreCase = true)) {
                val appBuildConfig = runCatching { cl.loadClass("dev.jamescullimore.android_security_training.BuildConfig") }.getOrNull()
                if (appBuildConfig != null) {
                    val appIdField = runCatching { appBuildConfig.getField("APPLICATION_ID") }.getOrNull()
                    val versionNameField = runCatching { appBuildConfig.getField("VERSION_NAME") }.getOrNull()
                    val appId = runCatching { appIdField?.get(null) as? String }.getOrNull()
                    val versionName = runCatching { versionNameField?.get(null) as? String }.getOrNull()
                    return@runCatching "Loaded self APK via DexClassLoader: BuildConfig{APPLICATION_ID=$appId, VERSION_NAME=$versionName} (path=${src.absolutePath})"
                }
            }

            // Preferred demo: dev.training.dynamic.Hello.greet()
            val tryPreferred = runCatching {
                val k = cl.loadClass("dev.training.dynamic.Hello")
                val m = k.getDeclaredMethod("greet")
                m.invoke(null) as? String
            }.getOrNull()
            if (tryPreferred != null) return@runCatching "Loaded dev.training.dynamic.Hello.greet(): ${tryPreferred} (path=${src.absolutePath})"

            // Keep failure simple now
            "Dynamic load failed: ClassNotFound for dev.training.dynamic.Hello.greet() (path=${src.absolutePath})"
        }.getOrElse { err -> "Dynamic load failed: ${err.javaClass.simpleName}: ${err.message}" }
    }

    override fun getSigningInfo(context: Context): String = runCatching {
        val digest = signingCertSha256B64(context)
        digest
    }.getOrElse { err -> "Error: ${err.message}" }

    override fun verifyExpectedSignature(context: Context): Boolean {
        // Vulnerable: does not verify expected signature at all
        return true
    }

    override fun getMethodToBeChangedAndResignedValue(): Boolean {
        return false
    }

    private fun signingCertSha256B64(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val cf = CertificateFactory.getInstance("X509")
        val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        val signInfo = info.signingInfo
        val sigs = if (signInfo != null && signInfo.hasMultipleSigners()) signInfo.apkContentsSigners else signInfo?.signingCertificateHistory
        val sigBytesList: List<ByteArray> = sigs?.map { it.toByteArray() } ?: emptyList()
        val first = sigBytesList.firstOrNull() ?: error("No signatures")
        val cert = cf.generateCertificate(ByteArrayInputStream(first)) as X509Certificate
        val sha = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        return Base64.encodeToString(sha, Base64.NO_WRAP)
    }
}
