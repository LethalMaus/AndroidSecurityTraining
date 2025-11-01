package dev.jamescullimore.android_security_training.network

import dev.jamescullimore.android_security_training.crypto.CryptoHelper
import dev.jamescullimore.android_security_training.crypto.SecureCryptoHelper
import dev.jamescullimore.android_security_training.re.ReDemoHelper
import dev.jamescullimore.android_security_training.re.SecureReDemoHelper
import dev.jamescullimore.android_security_training.perm.PermDemoHelper
import dev.jamescullimore.android_security_training.deeplink.DeepLinkHelper
import dev.jamescullimore.android_security_training.deeplink.SecureDeepLinkHelper
import dev.jamescullimore.android_security_training.storage.SecureStorageHelper
import dev.jamescullimore.android_security_training.storage.StorageHelper
import dev.jamescullimore.android_security_training.root.RootHelper
import dev.jamescullimore.android_security_training.root.SecureRootHelper
import dev.jamescullimore.android_security_training.multiuser.MultiUserHelper
import dev.jamescullimore.android_security_training.multiuser.SecureMultiUserHelper
import dev.jamescullimore.android_security_training.BuildConfig

// Route secure builds through the manual TrustManager demo when enabled via BuildConfig flag.
private val MANUAL_PIN: Boolean = BuildConfig.MANUAL_PIN

fun provideNetworkHelper(): NetworkHelper =
    if (MANUAL_PIN) ManualPinNetworkHelper() else SecureNetworkHelper()

fun provideCryptoHelper(): CryptoHelper = SecureCryptoHelper()

fun provideReDemoHelper(): ReDemoHelper = SecureReDemoHelper()

fun providePermDemoHelper(): PermDemoHelper = dev.jamescullimore.android_security_training.perm.SecurePermDemoHelper()

fun provideDeepLinkHelper(): DeepLinkHelper = SecureDeepLinkHelper()

fun provideStorageHelper(): StorageHelper = SecureStorageHelper()

fun provideRootHelper(): RootHelper = SecureRootHelper()

fun provideWebViewHelper(): dev.jamescullimore.android_security_training.web.WebViewHelper = dev.jamescullimore.android_security_training.web.SecureWebViewHelper()

fun provideMultiUserHelper(): MultiUserHelper = SecureMultiUserHelper()
