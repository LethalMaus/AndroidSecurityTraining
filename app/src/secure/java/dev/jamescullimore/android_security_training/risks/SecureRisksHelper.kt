package dev.jamescullimore.android_security_training.risks

import android.view.Window

class SecureRisksHelper : RisksHelper {
    override fun toggleFlagSecure(window: Window): String {
        // In secure builds, we block runtime toggling to avoid weakening protections.
        return "[SECURE] Action blocked: Runtime toggling of FLAG_SECURE is disabled in secure builds."
    }
}
