package dev.jamescullimore.android_security_training.perm

import android.content.Context

interface PermDemoHelper {
    fun uidGidAndSignatureInfo(context: Context): String
    fun tryStartProtectedService(context: Context): String
    fun tryQueryDemoProvider(context: Context, uri: String): String
    fun defaultDemoUri(context: Context): String
}
