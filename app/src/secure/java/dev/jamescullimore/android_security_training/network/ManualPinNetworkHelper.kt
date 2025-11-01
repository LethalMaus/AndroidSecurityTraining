package dev.jamescullimore.android_security_training.network

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Manual pinning demo using a custom TrustManager.
 *
 * WARNING: This is for training only. Rolling your own TrustManager is easy to get wrong. Risks include:
 * - Not scoping pins to a specific host
 * - Bypassing parts of the platform certificate validation
 * - Forgetting to include backup pins / rotation logic
 * - Failing to handle intermediates properly
 *
 * This implementation delegates to the platform's default X509TrustManager first (full chain validation),
 * then enforces an SPKI SHA-256 pin on the leaf certificate.
 */
class ManualPinNetworkHelper : NetworkHelper {

    // Pinned host and pins (SPKI SHA-256, Base64, NO_WRAP)
    private val pinnedHost = "api.github.com"
    private val allowedPins: Set<String> = setOf(
        "1EkvzibgiE3k+xdsv+7UU5vhV8kdFCQiUiFdMX5Guuk=",
        "fXkqYy8jL6cDXcYJvLgk0i8V0CVg28t3Tw4eBeaHeoA="
    )

    private fun defaultTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val tm = tmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
        return tm ?: throw IllegalStateException("No X509TrustManager found")
    }

    private fun spkiSha256Pin(cert: X509Certificate): String {
        val pubKey = cert.publicKey.encoded // SubjectPublicKeyInfo (DER)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(pubKey)
        return Base64.encodeToString(sha256, Base64.NO_WRAP)
    }

    private inner class PinnedTrustManager(
        private val delegate: X509TrustManager,
        private val host: String,
        private val pins: Set<String>
    ) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            delegate.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // First, perform platform validation (chain, expiry, policy, etc.)
            delegate.checkServerTrusted(chain, authType)

            if (chain.isNullOrEmpty()) throw CertificateException("Empty server cert chain")
            val leaf = chain[0]
            val pin = spkiSha256Pin(leaf)
            if (!pins.contains(pin)) {
                throw CertificateException("Pin mismatch for host $host. Got $pin")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
    }

    private fun pinnedClientForHost(host: String, pins: Set<String>): OkHttpClient {
        val baseTm = defaultTrustManager()
        val pinnedTm = PinnedTrustManager(baseTm, host, pins)
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(pinnedTm), SecureRandom())

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        return OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, pinnedTm)
            .hostnameVerifier { hostname, session ->
                // Enforce expected host; otherwise fall back to default verifier result
                if (hostname.equals(host, ignoreCase = true)) {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                } else {
                    false
                }
            }
            .addInterceptor(logging)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val sct = response.header("X-SCT") ?: "(no X-SCT header)"
                val expectCt = response.header("Expect-CT") ?: "(no Expect-CT header)"
                Log.i("CT", "SCT: $sct, Expect-CT: $expectCt")
                response
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val client: OkHttpClient by lazy { pinnedClientForHost(pinnedHost, allowedPins) }

    override suspend fun fetchDemo(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val firstLine = resp.body.string().lineSequence().firstOrNull()?.take(200)
            "HTTP ${resp.code}: ${firstLine ?: "<empty>"}"
        }
    }
}
