package dev.jamescullimore.android_security_training.risks

import android.view.Window
import android.view.WindowManager

class VulnRisksHelper : RisksHelper {
    override fun toggleFlagSecure(window: Window): String {
        val isSet = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        return if (isSet) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            "Disabled FLAG_SECURE. Screenshots allowed."
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            "Enabled FLAG_SECURE. Screenshots/recents should be blocked."
        }
    }
}
