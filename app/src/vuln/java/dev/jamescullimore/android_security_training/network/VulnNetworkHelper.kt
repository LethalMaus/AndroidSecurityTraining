package dev.jamescullimore.android_security_training.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Vulnerable implementation for training purposes:
 * - Trust-all X509TrustManager (Do NOT use in production)
 * - HostnameVerifier that always returns true
 * - Allows cleartext traffic (see network security config for vuln flavors)
 */
class VulnNetworkHelper : NetworkHelper {

    private fun trustAllSslSocketFactory(): Pair<SSLSocketFactory, X509TrustManager> {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        return context.socketFactory to trustAll
    }

    private val client: OkHttpClient by lazy {
        val (sslSocketFactory, trustManager) = trustAllSslSocketFactory()
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun fetchDemo(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val firstLine = resp.body?.string()?.lineSequence()?.firstOrNull()?.take(200)
            "HTTP ${resp.code}: ${firstLine ?: "<empty>"}"
        }
    }
}
