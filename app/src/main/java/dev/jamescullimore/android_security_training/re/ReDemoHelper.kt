package dev.jamescullimore.android_security_training.re

import android.content.Context

interface ReDemoHelper {
    fun getHardcodedSecret(): String
    fun readLeakyAsset(context: Context): String
    suspend fun tryDynamicDexLoad(context: Context, dexOrJarPath: String): String
    fun getSigningInfo(context: Context): String
    fun verifyExpectedSignature(context: Context): Boolean
}
