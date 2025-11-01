package dev.jamescullimore.android_security_training.network

/**
 * Simple interface to perform a demo HTTPS request and return a short string result.
 * Implementations will differ per securityProfile flavor (secure vs vuln).
 */
interface NetworkHelper {
    suspend fun fetchDemo(url: String = DEFAULT_URL): String

    /** Optional: allow runtime toggle for pinning demo modes ("bad" | "good" | "ct"). Default no-op. */
    fun setPinningMode(mode: String) { /* default no-op for implementations that don't support it */ }

    companion object {
        const val DEFAULT_URL: String = "https://api.github.com/"
    }
}
