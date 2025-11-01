package dev.jamescullimore.android_security_training.deeplink

import android.content.Context
import android.content.Intent

interface DeepLinkHelper {
    // Returns a human-readable summary of the incoming intent and validation outcome
    fun describeIncomingIntent(intent: Intent?): String

    // Processes an incoming VIEW intent: validates and returns a result string (no navigation side effects)
    fun handleIncomingIntent(intent: Intent): String

    // Example of safe internal navigation decision based on a URL string (used by UI button)
    fun safeNavigateExample(context: Context, uriString: String): String
}
