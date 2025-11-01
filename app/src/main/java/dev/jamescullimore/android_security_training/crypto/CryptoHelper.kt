package dev.jamescullimore.android_security_training.crypto

import javax.crypto.SecretKey

interface CryptoHelper {
    // Symmetric key generation (for demo only; in real apps prefer Android Keystore-backed keys)
    fun generateSymmetricKey(): SecretKey

    // Secure encryption using AES-GCM
    data class AesGcmResult(
        val iv: ByteArray,
        val cipherText: ByteArray,
        val tag: ByteArray,
        val algorithm: String = "AES/GCM/NoPadding"
    )
    fun encryptAesGcm(plaintext: ByteArray, aad: ByteArray? = null): AesGcmResult

    // Optional decrypt to validate round-trip (used locally)
    fun decryptAesGcm(result: AesGcmResult, aad: ByteArray? = null): ByteArray

    // Message authentication using HMAC-SHA256
    fun hmacSha256(data: ByteArray): ByteArray

    // Weak/incorrect examples for training (DO NOT USE IN PRODUCTION)
    fun encodeBase64Only(input: ByteArray): ByteArray
    fun encryptWeakAesEcb(plaintext: ByteArray): ByteArray

    // ECDH key agreement example (expects a real server P-256 public key in PEM/X.509 SPKI format)
    data class EcdhInfo(
        val publicKeyPem: String,
        val sharedSecretBytes: Int,
        val derivedKeyBytes: Int
    )
    fun performEcdhKeyAgreement(serverEcPublicKeyPem: String): EcdhInfo

    // Send an encrypted API payload over HTTPS (integration demo). Returns HTTP status + snippet.
    suspend fun postEncryptedJson(url: String, jsonPlaintext: String): String
}
