package dev.jamescullimore.android_security_training.crypto

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecureCryptoHelper : CryptoHelper {

    private val random = SecureRandom()
    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Simple in-memory demo key (for training). In production, use AndroidKeyStore.
    private var symmetricKey: SecretKey? = null

    override fun generateSymmetricKey(): SecretKey {
        val keyBytes = ByteArray(32)
        random.nextBytes(keyBytes)
        val key = SecretKeySpec(keyBytes, "AES")
        symmetricKey = key
        return key
    }

    private fun currentKey(): SecretKey = symmetricKey ?: generateSymmetricKey()

    override fun encryptAesGcm(plaintext: ByteArray, aad: ByteArray?): CryptoHelper.AesGcmResult {
        val key = currentKey()
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        if (aad != null) cipher.updateAAD(aad)
        val out = cipher.doFinal(plaintext)
        // Split cipherText and tag (last 16 bytes) for teaching clarity
        val tagLen = 16
        val ct = out.copyOfRange(0, out.size - tagLen)
        val tag = out.copyOfRange(out.size - tagLen, out.size)
        return CryptoHelper.AesGcmResult(iv, ct, tag)
    }

    override fun decryptAesGcm(result: CryptoHelper.AesGcmResult, aad: ByteArray?): ByteArray {
        val key = currentKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, result.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        if (aad != null) cipher.updateAAD(aad)
        val comb = result.cipherText + result.tag
        return cipher.doFinal(comb)
    }

    override fun hmacSha256(data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val macKey = currentKey() // for demo, reuse same material; in production, use distinct keys per purpose
        mac.init(SecretKeySpec(macKey.encoded, "HmacSHA256"))
        return mac.doFinal(data)
    }

    override fun encodeBase64Only(input: ByteArray): ByteArray = Base64.encode(input, Base64.NO_WRAP)

    override fun encryptWeakAesEcb(plaintext: ByteArray): ByteArray {
        // Not actually used in secure helper; just return a correct encryption to compare
        return encryptAesGcm(plaintext).cipherText
    }

    override fun performEcdhKeyAgreement(serverEcPublicKeyPem: String): CryptoHelper.EcdhInfo {
        // Minimal ECDH example with P-256; expects a PEM-encoded X.509 SubjectPublicKeyInfo
        val pem = serverEcPublicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()
        val serverPub = Base64.decode(pem, Base64.DEFAULT)
        val kf = KeyFactory.getInstance("EC")
        val pubKey = kf.generatePublic(X509EncodedKeySpec(serverPub))

        val ka = KeyAgreement.getInstance("ECDH")
        val keyPair = java.security.KeyPairGenerator.getInstance("EC").apply {
            initialize(256, random)
        }.genKeyPair()
        ka.init(keyPair.private)
        ka.doPhase(pubKey, true)
        val shared = ka.generateSecret()

        // Derive AES key via PBKDF2 (HKDF would be better; using PBKDF2 here to avoid extra deps)
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = skf.generateSecret(PBEKeySpec(Base64.encodeToString(shared, Base64.NO_WRAP).toCharArray(), salt, 10_000, 256))
        val derived = SecretKeySpec(key.encoded, "AES")
        symmetricKey = derived

        return CryptoHelper.EcdhInfo(
            publicKeyPem = exportPublicKeyPem(keyPair.public.encoded),
            sharedSecretBytes = shared.size,
            derivedKeyBytes = derived.encoded.size
        )
    }

    private fun exportPublicKeyPem(spki: ByteArray): String {
        val b64 = Base64.encodeToString(spki, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$b64\n-----END PUBLIC KEY-----"
    }

    override suspend fun postEncryptedJson(url: String, jsonPlaintext: String): String = withContext(Dispatchers.IO) {
        val aad = "v1".toByteArray()
        val enc = encryptAesGcm(jsonPlaintext.toByteArray(Charsets.UTF_8), aad)
        val bodyJson = """
            {
              "alg": "AES-GCM",
              "iv": "${Base64.encodeToString(enc.iv, Base64.NO_WRAP)}",
              "tag": "${Base64.encodeToString(enc.tag, Base64.NO_WRAP)}",
              "aad": "${Base64.encodeToString(aad, Base64.NO_WRAP)}",
              "ciphertext": "${Base64.encodeToString(enc.cipherText, Base64.NO_WRAP)}"
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
