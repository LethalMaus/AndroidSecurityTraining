package dev.jamescullimore.android_security_training.storage

import android.content.Context

interface StorageHelper {
    // Preferences
    suspend fun saveTokenSecure(context: Context, token: String): String
    suspend fun loadTokenSecure(context: Context): String

    suspend fun saveTokenInsecure(context: Context, token: String): String
    suspend fun loadTokenInsecure(context: Context): String

    // Files
    suspend fun writeSecureFile(context: Context, filename: String, content: String): String
    suspend fun writeInsecureFile(context: Context, filename: String, content: String): String
    suspend fun readInsecureFile(context: Context, filename: String): String

    // SQLite demo (plaintext DB for training)
    suspend fun dbPut(context: Context, key: String, value: String): String
    suspend fun dbGet(context: Context, key: String): String
    suspend fun dbList(context: Context): String
    suspend fun dbDelete(context: Context, key: String): String
}
