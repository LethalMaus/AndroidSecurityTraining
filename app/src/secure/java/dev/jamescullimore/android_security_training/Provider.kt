package dev.jamescullimore.android_security_training

import dev.jamescullimore.android_security_training.BuildConfig
import dev.jamescullimore.android_security_training.crypto.CryptoHelper
import dev.jamescullimore.android_security_training.crypto.SecureCryptoHelper
import dev.jamescullimore.android_security_training.deeplink.DeepLinkHelper
import dev.jamescullimore.android_security_training.deeplink.SecureDeepLinkHelper
import dev.jamescullimore.android_security_training.multiuser.MultiUserHelper
import dev.jamescullimore.android_security_training.multiuser.SecureMultiUserHelper
import dev.jamescullimore.android_security_training.network.ManualPinNetworkHelper
import dev.jamescullimore.android_security_training.network.NetworkHelper
import dev.jamescullimore.android_security_training.network.SecureNetworkHelper
import dev.jamescullimore.android_security_training.perm.PermDemoHelper
import dev.jamescullimore.android_security_training.perm.SecurePermDemoHelper
import dev.jamescullimore.android_security_training.re.ReDemoHelper
import dev.jamescullimore.android_security_training.re.SecureReDemoHelper
import dev.jamescullimore.android_security_training.risks.RisksHelper
import dev.jamescullimore.android_security_training.risks.SecureRisksHelper
import dev.jamescullimore.android_security_training.root.RootHelper
import dev.jamescullimore.android_security_training.root.SecureRootHelper
import dev.jamescullimore.android_security_training.storage.SecureStorageHelper
import dev.jamescullimore.android_security_training.storage.StorageHelper
import dev.jamescullimore.android_security_training.web.SecureWebViewHelper
import dev.jamescullimore.android_security_training.web.WebViewHelper

// Route secure builds through the manual TrustManager demo when enabled via BuildConfig flag.
private val MANUAL_PIN: Boolean = BuildConfig.MANUAL_PIN

fun provideNetworkHelper(): NetworkHelper =
    if (MANUAL_PIN) ManualPinNetworkHelper() else SecureNetworkHelper()

fun provideCryptoHelper(): CryptoHelper = SecureCryptoHelper()
fun provideReDemoHelper(): ReDemoHelper = SecureReDemoHelper()
fun providePermDemoHelper(): PermDemoHelper = SecurePermDemoHelper()
fun provideDeepLinkHelper(): DeepLinkHelper = SecureDeepLinkHelper()
fun provideStorageHelper(): StorageHelper = SecureStorageHelper()
fun provideRootHelper(): RootHelper = SecureRootHelper()
fun provideWebViewHelper(): WebViewHelper = SecureWebViewHelper()
fun provideMultiUserHelper(): MultiUserHelper = SecureMultiUserHelper()
fun provideRisksHelper(): RisksHelper = SecureRisksHelper()
