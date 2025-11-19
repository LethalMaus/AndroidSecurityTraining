package dev.jamescullimore.android_security_training.network

import dev.jamescullimore.android_security_training.crypto.CryptoHelper
import dev.jamescullimore.android_security_training.crypto.VulnCryptoHelper
import dev.jamescullimore.android_security_training.deeplink.DeepLinkHelper
import dev.jamescullimore.android_security_training.deeplink.VulnDeepLinkHelper
import dev.jamescullimore.android_security_training.multiuser.MultiUserHelper
import dev.jamescullimore.android_security_training.multiuser.VulnMultiUserHelper
import dev.jamescullimore.android_security_training.perm.PermDemoHelper
import dev.jamescullimore.android_security_training.perm.VulnPermDemoHelper
import dev.jamescullimore.android_security_training.re.ReDemoHelper
import dev.jamescullimore.android_security_training.re.VulnReDemoHelper
import dev.jamescullimore.android_security_training.risks.RisksHelper
import dev.jamescullimore.android_security_training.risks.VulnRisksHelper
import dev.jamescullimore.android_security_training.root.RootHelper
import dev.jamescullimore.android_security_training.root.VulnRootHelper
import dev.jamescullimore.android_security_training.storage.StorageHelper
import dev.jamescullimore.android_security_training.storage.VulnStorageHelper
import dev.jamescullimore.android_security_training.web.VulnWebViewHelper
import dev.jamescullimore.android_security_training.web.WebViewHelper

fun provideNetworkHelper(): NetworkHelper = VulnNetworkHelper()
fun provideCryptoHelper(): CryptoHelper = VulnCryptoHelper()
fun provideReDemoHelper(): ReDemoHelper = VulnReDemoHelper()
fun providePermDemoHelper(): PermDemoHelper = VulnPermDemoHelper()
fun provideDeepLinkHelper(): DeepLinkHelper = VulnDeepLinkHelper()
fun provideStorageHelper(): StorageHelper = VulnStorageHelper()
fun provideRootHelper(): RootHelper = VulnRootHelper()
fun provideWebViewHelper(): WebViewHelper = VulnWebViewHelper()
fun provideMultiUserHelper(): MultiUserHelper = VulnMultiUserHelper()
fun provideRisksHelper(): RisksHelper = VulnRisksHelper()
