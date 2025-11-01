package dev.jamescullimore.android_security_training.root

import android.content.Context

/** Root detection/training interface for Anti-Root topic. */
interface RootHelper {
    data class RootSignal(val name: String, val detected: Boolean, val details: String? = null)

    fun getSignals(context: Context): List<RootSignal>
    fun isRooted(context: Context): Boolean
    fun deviceInfo(): String

    /** Placeholder string for classroom: integrate Play Integrity in real exercises. */
    fun playIntegrityStatus(context: Context): String

    /** Simple tamper check (e.g., signing cert digest). Implemented in secure helper. */
    fun tamperCheck(context: Context): Boolean

    /** In vuln builds this enables a trivial bypass; secure builds ignore. */
    fun setBypassEnabled(enabled: Boolean)
}