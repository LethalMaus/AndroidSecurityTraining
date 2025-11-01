package dev.jamescullimore.android_security_training.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import androidx.core.content.edit

class SecureStorageHelper : StorageHelper {

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun securePrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs", // stored in /data/data/<pkg>/shared_prefs
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    // --- Preferences ---
    override suspend fun saveTokenSecure(context: Context, token: String): String {
        val prefs = securePrefs(context)
        prefs.edit { putString(KEY_TOKEN, token) }
        return "Saved token securely to EncryptedSharedPreferences"
    }

    override suspend fun loadTokenSecure(context: Context): String {
        val token = securePrefs(context).getString(KEY_TOKEN, null)
        return token?.let { "Loaded secure token: ${it.take(4)}… (redacted)" }
            ?: "No secure token saved"
    }

    override suspend fun saveTokenInsecure(context: Context, token: String): String {
        // In secure helper we still demonstrate the insecure path for comparison (plaintext prefs)
        val prefs = context.getSharedPreferences("insecure_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_TOKEN, token) }
        return "Saved token INSECURELY to plaintext SharedPreferences (for comparison)"
    }

    override suspend fun loadTokenInsecure(context: Context): String {
        val token = context.getSharedPreferences("insecure_prefs", Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
        return token?.let { "Loaded INSECURE token (plaintext): $it" } ?: "No insecure token saved"
    }

    // --- Files ---
    override suspend fun writeSecureFile(context: Context, filename: String, content: String): String {
        val file = File(context.filesDir, filename)
        if (file.exists()) file.delete()
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey(context),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileOutput().use { it.write(content.toByteArray()) }
        return "Wrote encrypted file at: ${file.absolutePath}"
    }

    override suspend fun writeInsecureFile(context: Context, filename: String, content: String): String {
        val file = File(context.cacheDir, filename) // plaintext in cache dir
        file.writeText(content)
        return "Wrote INSECURE plaintext file at: ${file.absolutePath}"
    }

    override suspend fun readInsecureFile(context: Context, filename: String): String {
        val file = File(context.cacheDir, filename)
        return if (file.exists()) {
            "Read INSECURE file content: ${file.readText()}"
        } else {
            "Insecure file not found at: ${file.absolutePath}"
        }
    }

    // --- SQLite (plaintext demo) ---
    override suspend fun dbPut(context: Context, key: String, value: String): String {
        val db = TokensDb(context).writableDatabase
        db.execSQL("INSERT OR REPLACE INTO $TABLE (k, v) VALUES (?, ?)", arrayOf(key, value))
        return "DB: upserted ($key -> ${value.take(4)}… ) into ${dbPath(context)}"
    }

    override suspend fun dbGet(context: Context, key: String): String {
        val db = TokensDb(context).readableDatabase
        db.rawQuery("SELECT v FROM $TABLE WHERE k = ?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) {
                val v = c.getString(0)
                "DB: got value for $key: ${v}"
            } else {
                "DB: no row for key=$key"
            }
        }
    }

    override suspend fun dbList(context: Context): String {
        val db = TokensDb(context).readableDatabase
        val sb = StringBuilder()
        db.rawQuery("SELECT k, v FROM $TABLE ORDER BY k", null).use { c ->
            while (c.moveToNext()) {
                sb.append(c.getString(0)).append(" -> ").append(c.getString(1)).append('\n')
            }
        }
        val out = sb.toString().ifBlank { "<empty>" }
        return "DB (${dbPath(context)}):\n$out"
    }

    override suspend fun dbDelete(context: Context, key: String): String {
        val db = TokensDb(context).writableDatabase
        val rows = db.delete(TABLE, "k=?", arrayOf(key))
        return "DB: deleted $rows row(s) for key=$key"
    }

    private fun dbPath(context: Context): String = context.getDatabasePath(DB_NAME).absolutePath

    private class TokensDb(ctx: Context) : SQLiteOpenHelper(ctx, DB_NAME, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // No-op for demo
        }
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val DB_NAME = "tokens.db"
        private const val TABLE = "tokens"
    }
}
