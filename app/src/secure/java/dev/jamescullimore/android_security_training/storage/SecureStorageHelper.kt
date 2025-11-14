package dev.jamescullimore.android_security_training.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import androidx.core.content.edit
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

    private fun logSecurityEvent(event: String) {
        Log.w(TAG, "SECURITY: $event")
    }

    private fun getOrCreateHmacKey(context: Context): ByteArray {
        val prefs = securePrefs(context)
        val existing = prefs.getString(KEY_HMAC, null)
        if (existing != null) return Base64.decode(existing, Base64.NO_WRAP)
        val rnd = ByteArray(32)
        SecureRandom().nextBytes(rnd)
        prefs.edit { putString(KEY_HMAC, Base64.encodeToString(rnd, Base64.NO_WRAP)) }
        return rnd
    }

    private fun hmac(key: ByteArray, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val out = mac.doFinal(data.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    // --- Preferences ---
    override suspend fun saveTokenSecure(context: Context, token: String): String {
        val prefs = securePrefs(context)
        prefs.edit { putString(KEY_TOKEN, token) }
        return "Saved token securely to EncryptedSharedPreferences"
    }

    override suspend fun loadTokenSecure(context: Context): String {
        return try {
            val token = securePrefs(context).getString(KEY_TOKEN, null)
            token?.let { "Loaded secure token: ${it.take(4)}… (redacted)" } ?: "No secure token saved"
        } catch (t: Throwable) {
            // If prefs file was tampered/corrupted, clear and force re-auth
            runCatching { securePrefs(context).edit { clear() } }
            logSecurityEvent("EncryptedSharedPreferences read failure (possible tamper): ${t.javaClass.simpleName}: ${t.message}")
            "[SECURE] Secure prefs appear tampered/corrupted. Cleared. Please re-authenticate."
        }
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

    // --- SQLite (plaintext demo with integrity MAC) ---
    override suspend fun dbPut(context: Context, key: String, value: String): String {
        val db = TokensDb(context).writableDatabase
        val mac = hmac(getOrCreateHmacKey(context), "$key|$value")
        db.execSQL("INSERT OR REPLACE INTO $TABLE (k, v, mac) VALUES (?, ?, ?)", arrayOf(key, value, mac))
        return "DB: upserted ($key -> ${value.take(4)}… ) into ${dbPath(context)}"
    }

    override suspend fun dbGet(context: Context, key: String): String {
        val db = TokensDb(context).readableDatabase
        db.rawQuery("SELECT v, mac FROM $TABLE WHERE k = ?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) {
                val v = c.getString(0)
                val mac = c.getString(1)
                val expect = hmac(getOrCreateHmacKey(context), "$key|$v")
                return if (mac == expect) {
                    "DB: got value for $key: ${v}"
                } else {
                    logSecurityEvent("DB tamper detected for key='$key' (MAC mismatch)")
                    "[SECURE] DB record for '$key' appears tampered. Please re-authenticate."
                }
            } else {
                "DB: no row for key=$key"
            }
        }
    }

    override suspend fun dbList(context: Context): String {
        val db = TokensDb(context).readableDatabase
        val sb = StringBuilder()
        db.rawQuery("SELECT k, v, mac FROM $TABLE ORDER BY k", null).use { c ->
            while (c.moveToNext()) {
                val k = c.getString(0)
                val v = c.getString(1)
                val mac = c.getString(2)
                val expect = hmac(getOrCreateHmacKey(context), "$k|$v")
                if (mac == expect) {
                    sb.append(k).append(" -> ").append(v)
                } else {
                    logSecurityEvent("DB tamper detected for key='$k' during list (MAC mismatch or missing)")
                    sb.append(k).append(" -> ").append("<tampered>")
                }
                sb.append('\n')
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

    private class TokensDb(private val ctx: Context) : SQLiteOpenHelper(ctx, DB_NAME, null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE (k TEXT PRIMARY KEY, v TEXT NOT NULL, mac TEXT)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                // Add MAC column if upgrading from older schema. We don't backfill; old rows will be flagged and require re-auth/write.
                try { db.execSQL("ALTER TABLE $TABLE ADD COLUMN mac TEXT") } catch (_: Throwable) { /* column may already exist */ }
            }
        }
    }

    companion object {
        private const val TAG = "SecureStorage"
        private const val KEY_TOKEN = "token"
        private const val KEY_HMAC = "hmac_key"
        private const val DB_NAME = "tokens.db"
        private const val TABLE = "tokens"
    }
}
