package dev.jamescullimore.android_security_training.storage

import android.content.Context
import android.os.Environment
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

// Intentionally insecure implementations for training ONLY
class VulnStorageHelper : StorageHelper {

    // --- Preferences ---
    override suspend fun saveTokenSecure(context: Context, token: String): String {
        // In vuln build, we still show the "secure" method to contrast. We'll store in EncryptedSharedPreferences only in secure build.
        val prefs = context.getSharedPreferences("secure_like_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token.reversed()) // bogus transformation
            .apply()
        return "[VULN] Pretended to save securely but actually stored obfuscated token in plaintext prefs"
    }

    override suspend fun loadTokenSecure(context: Context): String {
        val token = context.getSharedPreferences("secure_like_prefs", Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
        return token?.let { "[VULN] Loaded faux-secure token (still plaintext): $it" } ?: "No token"
    }

    override suspend fun saveTokenInsecure(context: Context, token: String): String {
        val prefs = context.getSharedPreferences("insecure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
        return "Saved token INSECURELY to plaintext SharedPreferences"
    }

    override suspend fun loadTokenInsecure(context: Context): String {
        val token = context.getSharedPreferences("insecure_prefs", Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
        return token?.let { "Loaded INSECURE token (plaintext): $it" } ?: "No insecure token saved"
    }

    // --- Files ---
    override suspend fun writeSecureFile(context: Context, filename: String, content: String): String {
        // In vuln builds this writes plaintext to external storage to show risk
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, filename)
        file.writeText(content)
        return "[VULN] Wrote supposed 'secure' file IN PLAINTEXT at external app files: ${file.absolutePath}"
    }

    override suspend fun writeInsecureFile(context: Context, filename: String, content: String): String {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        publicDir.mkdirs()
        val file = File(publicDir, filename)
        // This location may be readable by other apps via user grants / media scans
        file.writeText(content)
        return "Wrote INSECURE plaintext file in public Downloads: ${file.absolutePath}"
    }

    override suspend fun readInsecureFile(context: Context, filename: String): String {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(publicDir, filename)
        return if (file.exists()) {
            "Read INSECURE public file content: ${file.readText()}"
        } else {
            "Insecure public file not found at: ${file.absolutePath}"
        }
    }

    // --- SQLite (plaintext demo) ---
    override suspend fun dbPut(context: Context, key: String, value: String): String {
        val db = TokensDb(context).writableDatabase
        // Intentionally store as plaintext
        db.execSQL("INSERT OR REPLACE INTO $TABLE (k, v) VALUES (?, ?)", arrayOf(key, value))
        return "[VULN] DB: upserted ($key -> $value) into ${dbPath(context)}"
    }

    override suspend fun dbGet(context: Context, key: String): String {
        val db = TokensDb(context).readableDatabase
        db.rawQuery("SELECT v FROM $TABLE WHERE k = ?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) {
                val v = c.getString(0)
                "[VULN] DB: got value for $key: $v"
            } else {
                "[VULN] DB: no row for key=$key"
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
        return "[VULN] DB (${dbPath(context)}):\n$out"
    }

    override suspend fun dbDelete(context: Context, key: String): String {
        val db = TokensDb(context).writableDatabase
        val rows = db.delete(TABLE, "k=?", arrayOf(key))
        return "[VULN] DB: deleted $rows row(s) for key=$key"
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
