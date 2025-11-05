package dev.jamescullimore.android_security_training.network

import android.net.http.X509TrustManagerExtensions
import android.util.Base64
import android.util.Log
import dev.jamescullimore.android_security_training.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Socket
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
 * This implementation delegates to the platform's default X509TrustManager first (full chain validation)
 * using hostname-aware checks, then enforces an SPKI SHA-256 pin on the leaf certificate.
 */
class ManualPinNetworkHelper : NetworkHelper {

    // Mode for behavior toggling; read from BuildConfig.PIN_MODE (e.g., "bad", "good", "ct", "mitm")
    private val mode: String = runCatching {
        BuildConfig::class.java.getField("PIN_MODE").get(null) as? String
    }.getOrNull() ?: "bad"

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

    private inner class HostnameAwarePinnedTM(
        private val delegate: X509TrustManager,
        private val host: String,
        private val pins: Set<String>
    ) : X509ExtendedTrustManager() {

        private val ext = X509TrustManagerExtensions(delegate)

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            delegate.checkClientTrusted(chain, authType)
        }

        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?, authType: String?, socket: Socket?
        ) {
            delegate.checkClientTrusted(chain, authType)
        }

        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?
        ) {
            delegate.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Fallback path without hostname (rare). Do platform validation, then pins.
            delegate.checkServerTrusted(chain, authType)
            enforcePins(chain)
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?, authType: String?, socket: Socket?
        ) {
            val peerHost = (socket as? SSLSocket)?.handshakeSession?.peerHost
            validateWithHostname(chain, authType, peerHost)
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?
        ) {
            val peerHost = engine?.peerHost
            validateWithHostname(chain, authType, peerHost)
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers

        private fun validateWithHostname(
            chain: Array<out X509Certificate>?, authType: String?, hostname: String?
        ) {
            if (hostname.isNullOrEmpty()) {
                // Fall back to non-hostname variant
                checkServerTrusted(chain, authType)
                return
            }
            // Platform validation with hostname-aware path (required when domain configs/CT are used)
            val cleaned = ext.checkServerTrusted(chain, authType, hostname)
            enforcePins(cleaned.toTypedArray())
        }

        private fun enforcePins(chain: Array<out X509Certificate>?) {
            if (mode.equals("mitm", ignoreCase = true)) {
                Log.w("Pinning", "PIN_MODE=mitm: bypassing SPKI pin enforcement for host $host (debug/demo only)")
                return
            }
            if (chain.isNullOrEmpty()) throw CertificateException("Empty server cert chain")
            val leaf = chain[0]
            val pin = spkiSha256Pin(leaf)
            if (!pins.contains(pin)) {
                throw CertificateException("Pin mismatch for host $host. Got $pin")
            }
        }
    }

    private fun pinnedClientForHost(host: String, pins: Set<String>): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        if (mode.equals("mitm", ignoreCase = true)) {
            // Debug-only: build a trust-all client to allow interception with mitmproxy.
            // Hostname verification remains default (expected cert CN/SAN must match host),
            // and Network Security Config CT enforcement is bypassed by using our own TM.
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAll)
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        val baseTm = defaultTrustManager()
        val pinnedTm = HostnameAwarePinnedTM(baseTm, host, pins)
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(pinnedTm), SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, pinnedTm)
            // Use default hostname verifier (OkHttp/Conscrypt) for proper endpoint identification
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
