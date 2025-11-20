package dev.jamescullimore.android_security_training.multiuser

import android.content.Context

interface MultiUserHelper {
    /** Returns a multi-line string with user/app/uid info and environment notes. */
    fun getRuntimeInfo(context: Context): String

    /** Best-effort listing of users/profiles (may require privileges on production devices). */
    fun listUsersBestEffort(context: Context): String

    // Per-user scoped token storage (secure)
    fun savePerUserToken(context: Context, token: String): String
    fun loadPerUserToken(context: Context): String

    // Intentionally global/insecure storage to illustrate leakage across users/profiles (vuln path)
    fun saveGlobalTokenInsecure(context: Context, token: String): String
    fun loadGlobalTokenInsecure(context: Context): String

    /** Attempt cross-user read (will fail without special permissions; used to show SecurityException handling). */
    fun tryCrossUserRead(context: Context, targetUserId: Int): String

    /** Attempt to send a broadcast to another user. */
    fun trySendBroadcastAsUser(context: Context, targetUserId: Int, action: String): String

    /** Attempt to create a Context for another user. */
    fun tryCreateContextAsUser(context: Context, targetUserId: Int): String
}
