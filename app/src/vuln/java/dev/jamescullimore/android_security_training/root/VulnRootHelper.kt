package dev.jamescullimore.android_security_training.root

import android.content.Context
import android.os.Build

class VulnRootHelper : RootHelper {
    private var bypass: Boolean = false

    override fun getSignals(context: Context): List<RootHelper.RootSignal> {
        // Provide minimal/no signals and leak info for training
        val list = mutableListOf<RootHelper.RootSignal>()
        list += RootHelper.RootSignal("signals disabled (vuln)", false, "Training-only: not checking root signals")
        return list
    }

    override fun isRooted(context: Context): Boolean {
        // Intentionally return false or allow toggled bypass
        return if (bypass) false else false
    }

    override fun deviceInfo(): String = "SDK=${Build.VERSION.SDK_INT}; brand=${Build.BRAND}; model=${Build.MODEL}"

    override fun playIntegrityStatus(context: Context): String = "Play Integrity: not integrated in vuln build (training demo); no enforcement."

    override fun tamperCheck(context: Context): Boolean = true // Does nothing in vuln builds

    override fun setBypassEnabled(enabled: Boolean) { bypass = enabled }
}