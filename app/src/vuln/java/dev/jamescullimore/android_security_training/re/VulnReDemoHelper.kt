package dev.jamescullimore.android_security_training.re

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
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
            val path = if (dexOrJarPath.equals("self", ignoreCase = true)) context.packageCodePath else dexOrJarPath
            val src = File(path)
            val optDir = File(context.codeCacheDir, "dexopt").apply { mkdirs() }
            val cl = DexClassLoader(src.absolutePath, optDir.absolutePath, null, context.classLoader)

            // First try to load a known class from our own APK to demo self-loading
            val appBuildConfig = runCatching { cl.loadClass("dev.jamescullimore.android_security_training.BuildConfig") }.getOrNull()
            if (appBuildConfig != null) {
                val appIdField = runCatching { appBuildConfig.getField("APPLICATION_ID") }.getOrNull()
                val versionNameField = runCatching { appBuildConfig.getField("VERSION_NAME") }.getOrNull()
                val appId = runCatching { appIdField?.get(null) as? String }.getOrNull()
                val versionName = runCatching { versionNameField?.get(null) as? String }.getOrNull()
                return@runCatching "Loaded self APK via DexClassLoader: BuildConfig{APPLICATION_ID=$appId, VERSION_NAME=$versionName} (path=$path)"
            }

            // Fallback to previous demo class name if present in provided dex/jar
            val klass = cl.loadClass("dev.training.dynamic.Hello")
            val method = klass.getDeclaredMethod("hello")
            val result = method.invoke(null) as? String ?: "(null)"
            "Loaded Hello.hello(): $result (path=$path)"
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

    private fun signingCertSha256B64(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val cf = CertificateFactory.getInstance("X509")
        val sigBytesList: List<ByteArray> = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val signInfo = info.signingInfo
            val sigs = if (signInfo != null && signInfo.hasMultipleSigners()) signInfo.apkContentsSigners else signInfo?.signingCertificateHistory
            sigs?.map { it.toByteArray() } ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.map { it.toByteArray() } ?: emptyList()
        }
        val first = sigBytesList.firstOrNull() ?: error("No signatures")
        val cert = cf.generateCertificate(ByteArrayInputStream(first)) as X509Certificate
        val sha = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        return Base64.encodeToString(sha, Base64.NO_WRAP)
    }
}
