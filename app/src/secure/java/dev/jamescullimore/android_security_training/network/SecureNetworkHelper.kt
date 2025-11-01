package dev.jamescullimore.android_security_training.network

import dev.jamescullimore.android_security_training.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Secure implementation supporting three modes:
 * - bad: Intentionally incorrect pins (should FAIL)
 * - good: Correct pins (should SUCCEED)
 * - ct: No pins in code; rely on platform trust + Network Security Config with <certificateTransparency enabled="true"/>
 */
class SecureNetworkHelper : NetworkHelper {

    @Volatile private var mode: String = runCatching {
        BuildConfig::class.java.getField("PIN_MODE").get(null) as? String
    }.getOrNull() ?: "bad"
    @Volatile private var client: OkHttpClient = buildClient(mode)

    private fun buildClient(mode: String): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Log SCT / Expect-CT related headers if present
                val sct = response.header("X-SCT") ?: "(no X-SCT header)"
                val expectCt = response.header("Expect-CT") ?: "(no Expect-CT header)"
                android.util.Log.i("CT", "mode=$mode; SCT: $sct, Expect-CT: $expectCt")
                response
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)

        when (mode.lowercase()) {
            "bad" -> {
                // Deliberately wrong pins to demonstrate failure
                val badPinner = CertificatePinner.Builder()
                    .add("api.github.com",
                        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
                    .build()
                builder.certificatePinner(badPinner)
            }
            "good" -> {
                val goodPinner = CertificatePinner.Builder()
                    .add(
                        "api.github.com",
                        "sha256/1EkvzibgiE3k+xdsv+7UU5vhV8kdFCQiUiFdMX5Guuk=",
                        "sha256/fXkqYy8jL6cDXcYJvLgk0i8V0CVg28t3Tw4eBeaHeoA="
                    )
                    .build()
                builder.certificatePinner(goodPinner)
            }
            else -> {
                // ct (or any other value): no CertificatePinner; rely on platform trust + CT in NSC
            }
        }
        return builder.build()
    }

    override fun setPinningMode(mode: String) {
        val normalized = mode.lowercase()
        if (normalized != this.mode.lowercase()) {
            this.mode = normalized
            this.client = buildClient(normalized)
        }
    }

    override suspend fun fetchDemo(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val firstLine = resp.body.string().lineSequence().firstOrNull()?.take(200)
            "[mode=${this@SecureNetworkHelper.mode}] HTTP ${resp.code}: ${firstLine ?: "<empty>"}"
        }
    }
}
