package dev.jamescullimore.android_security_training.risks

import android.view.Window

/**
 * Helper for Risks topic behaviors that differ between secure and vuln flavors.
 */
interface RisksHelper {
    /**
     * Toggle FLAG_SECURE on the provided Window.
     * - vuln flavor: actually toggles and returns a message describing the new state.
     * - secure flavor: does not change state and returns a blocked message.
     */
    fun toggleFlagSecure(window: Window): String
}
