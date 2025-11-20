package dev.jamescullimore.android_security_training.crypto

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * INTENTIONALLY VULNERABLE implementation for training ONLY.
 * Demonstrates common mistakes: AES/ECB, static keys, confusing Base64 with encryption, no integrity.
 */
class VulnCryptoHelper : CryptoHelper {

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true } // DO NOT DO THIS IN REAL APPS
            .build()
    }

    // Static hard-coded key: trivial to extract via reverse engineering.
    private val staticKey: SecretKey = SecretKeySpec(
        // 16 bytes (128-bit) predictable value
        byteArrayOf(1,2,3,4,5,6,7,8,8,7,6,5,4,3,2,1),
        "AES"
    )

    override fun generateSymmetricKey(): SecretKey = staticKey

    override fun encryptAesGcm(plaintext: ByteArray, aad: ByteArray?): CryptoHelper.AesGcmResult {
        // Fake GCM: actually using ECB and no IV/Tag; filled with zeros for demo structure
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, staticKey)
        val ct = cipher.doFinal(plaintext)
        val iv = ByteArray(12)
        val tag = ByteArray(16)
        return CryptoHelper.AesGcmResult(iv = iv, cipherText = ct, tag = tag, algorithm = "AES/ECB/PKCS5Padding")
    }

    override fun decryptAesGcm(result: CryptoHelper.AesGcmResult, aad: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, staticKey)
        return cipher.doFinal(result.cipherText)
    }

    override fun hmacSha256(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA1") // weaker HMAC just to showcase difference
        mac.init(SecretKeySpec(staticKey.encoded, "HmacSHA1"))
        return mac.doFinal(data)
    }

    override fun encodeBase64Only(input: ByteArray): ByteArray = Base64.encode(input, Base64.NO_WRAP)

    override fun encryptWeakAesEcb(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, staticKey)
        return cipher.doFinal(plaintext)
    }

    override fun performEcdhKeyAgreement(serverEcPublicKeyPem: String): CryptoHelper.EcdhInfo {
        // No real ECDH here; returns dummy values so students can compare with secure build
        return CryptoHelper.EcdhInfo(
            publicKeyPem = "// Not generated in vuln build",
            sharedSecretBytes = 0,
            derivedKeyBytes = staticKey.encoded.size
        )
    }

    override suspend fun postEncryptedJson(url: String, jsonPlaintext: String): String = withContext(Dispatchers.IO) {
        // Misuse: treat Base64 as encryption and send without IV/tag/AAD
        val bodyJson = """
            {
              "alg": "BASE64",
              "ciphertext": "${Base64.encodeToString(jsonPlaintext.toByteArray(), Base64.NO_WRAP)}"
            }
        """.trimIndent()
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val snippet = resp.body.string().take(400)
            "HTTP ${resp.code}: $snippet"
        }
    }
}
