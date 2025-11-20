package dev.jamescullimore.android_security_training.multiuser

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import androidx.core.content.edit
import java.lang.reflect.InvocationTargetException

class SecureMultiUserHelper : MultiUserHelper {

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private fun perUserSuffix(context: Context): String {
        // ANDROID_ID is per-user (and per-app signing key on some versions). Good enough for demo scoping.
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun securePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "per_user_secure_prefs",
        masterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getRuntimeInfo(context: Context): String {
        val uid = Process.myUid()
        val appId = uid % 100000 // heuristic formula used on AOSP for multi-user UIDs
        val userId = uid / 100000
        val device = "SDK=${Build.VERSION.SDK_INT}; brand=${Build.BRAND}; model=${Build.MODEL}; tags=${Build.TAGS}"
        return buildString {
            appendLine("Runtime Info:")
            appendLine("  uid=$uid (userId=$userId, appId=$appId)")
            appendLine("  ANDROID_ID(per-user)=${perUserSuffix(context)}")
            appendLine("  $device")
            appendLine("Notes: On many builds, uid = userId*100000 + appId. Users are isolated; app data is per-user sandbox.")
        }
    }

    override fun listUsersBestEffort(context: Context): String {
        // Third-party apps typically cannot enumerate other users. Demonstrate limitation.
        return "Listing users requires privileged permissions (INTERACT_ACROSS_USERS). As a third-party app, access is denied."
    }

    override fun savePerUserToken(context: Context, token: String): String {
        val key = "token_${perUserSuffix(context)}"
        securePrefs(context).edit { putString(key, token) }
        return "Saved token scoped to current user using EncryptedSharedPreferences (key=$key)"
    }

    override fun loadPerUserToken(context: Context): String {
        val key = "token_${perUserSuffix(context)}"
        val t = securePrefs(context).getString(key, null)
        return t?.let { "Loaded per-user token (redacted): ${redact(it)}" } ?: "No per-user token found (key=$key)"
    }

    override fun saveGlobalTokenInsecure(context: Context, token: String): String {
        // Demonstrate why global/shared locations are risky even across profiles (e.g., external storage)
        val f = File(context.getExternalFilesDir(null), "global_token.txt")
        f.writeText(token)
        return "[DEMO] Wrote GLOBAL plaintext token to ${f.absolutePath} (avoid this; can be visible across users/profiles on some devices)"
    }

    override fun loadGlobalTokenInsecure(context: Context): String {
        val f = File(context.getExternalFilesDir(null), "global_token.txt")
        return if (f.exists()) "[DEMO] Read GLOBAL plaintext token: ${f.readText()}" else "No global token file at ${f.absolutePath}"
    }

    override fun tryCrossUserRead(context: Context, targetUserId: Int): String {
        // We do not hold INTERACT_ACROSS_USERS; show expected denial.
        return "Cross-user access denied: requires INTERACT_ACROSS_USERS(_FULL). Demonstration only; see README for adb/AAOS notes."
    }

    private fun userHandleCompat(targetUserId: Int): Any {
        val userHandleClass = Class.forName("android.os.UserHandle")
        return try {
            val ofMethod = userHandleClass.getMethod("of", Int::class.javaPrimitiveType)
            ofMethod.invoke(null, targetUserId)
        } catch (_: NoSuchMethodException) {
            // Older API fallback: hidden constructor UserHandle(int)
            val ctor = userHandleClass.getDeclaredConstructor(Int::class.javaPrimitiveType)
            ctor.isAccessible = true
            ctor.newInstance(targetUserId)
        }
    }

    private fun unwrapInvocationTarget(ex: Throwable): Throwable {
        return if (ex is InvocationTargetException && ex.cause != null) ex.cause!! else ex
    }

    override fun trySendBroadcastAsUser(context: Context, targetUserId: Int, action: String): String {
        return try {
            val intent = Intent(action)
            val userHandleClass = Class.forName("android.os.UserHandle")
            val userHandle = userHandleCompat(targetUserId)
            val m = Context::class.java.getMethod("sendBroadcastAsUser", Intent::class.java, userHandleClass)
            m.invoke(context, intent, userHandle)
            "sendBroadcastAsUser invoked; expected SecurityException on stock devices"
        } catch (t: Throwable) {
            val root = unwrapInvocationTarget(t)
            "sendBroadcastAsUser failed: ${root.javaClass.simpleName}: ${root.message}"
        }
    }

    override fun tryCreateContextAsUser(context: Context, targetUserId: Int): String {
        return try {
            val userHandleClass = Class.forName("android.os.UserHandle")
            val userHandle = userHandleCompat(targetUserId)
            val m = Context::class.java.getMethod(
                "createPackageContextAsUser",
                String::class.java,
                Int::class.javaPrimitiveType,
                userHandleClass
            )
            val other = m.invoke(context, context.packageName, 0, userHandle) as Context
            "createPackageContextAsUser succeeded (unexpected on stock devices): ${other.packageName}"
        } catch (t: Throwable) {
            val root = unwrapInvocationTarget(t)
            "createPackageContextAsUser failed: ${root.javaClass.simpleName}: ${root.message}"
        }
    }

    private fun redact(s: String): String = if (s.length <= 4) "****" else s.take(2) + "…" + s.takeLast(2)
}
