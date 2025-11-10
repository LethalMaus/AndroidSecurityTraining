package dev.jamescullimore.android_security_training.re

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class SecureReDemoHelper : ReDemoHelper {

    // No hardcoded secrets in secure builds
    override fun getHardcodedSecret(): String = "<not present — use secure storage/remote config>"

    override fun readLeakyAsset(context: Context): String = try {
        // Intentionally not shipping sensitive asset in secure builds
        context.assets.open("sensitive.txt").use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (_: Throwable) {
        "Asset not present (secured)."
    }

    override suspend fun tryDynamicDexLoad(context: Context, dexOrJarPath: String): String {
        val path = if (dexOrJarPath.equals("self", ignoreCase = true)) context.packageCodePath else dexOrJarPath
        return "Dynamic code loading is blocked by policy in secure builds (requested='$path')."
    }

    override fun getSigningInfo(context: Context): String = runCatching {
        val digest = signingCertSha256B64(context)
        digest
    }.getOrElse { err -> "Error: ${err.message}" }

    override fun verifyExpectedSignature(context: Context): Boolean {
        val actual = runCatching { signingCertSha256B64(context) }.getOrNull() ?: return false
        val expected = EXPECTED_CERT_DIGEST_B64 // Release signing certificate SHA-256 (Base64 NO_WRAP)
        return expected.isNotBlank() && actual == expected
    }

    override fun getMethodToBeChangedAndResignedValue(): Boolean {
        // In secure build, expose a constant indicator; students won't modify this build variant
        return false
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

    companion object {
        private const val EXPECTED_CERT_DIGEST_B64: String = "Plazc2oWHYXXVf8ZXUPiLS9fBySu3GhTc0qg/fTy+/I="
    }
}
