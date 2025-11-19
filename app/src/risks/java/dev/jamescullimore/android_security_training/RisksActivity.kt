package dev.jamescullimore.android_security_training

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.security.NetworkSecurityPolicy
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RisksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val output = remember { mutableStateOf("Ready. Topic: Other Common App Risks") }
                    val scope = rememberCoroutineScope()

                    fun showAppConfig(): String {
                        val pm = packageManager
                        val ai: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
                        val debuggable = (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                        val allowBackup = (ai.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
                        val cleartextPermitted = try {
                            NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted
                        } catch (_: Throwable) { null }
                        val windowSecure = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                        return buildString {
                            appendLine("App Config")
                            appendLine("- debuggable: $debuggable (debug builds auto-enable; avoid setting in release)")
                            appendLine("- allowBackup: $allowBackup (set false to prevent adb backup / Auto Backup)")
                            appendLine("- cleartextTrafficPermitted: ${cleartextPermitted ?: "unknown"}")
                            appendLine("- windowSecure(FLAG_SECURE): $windowSecure (set to block screenshots/overlays)")
                            appendLine("- appId: $packageName")
                        }
                    }

                    fun listExported(): String {
                        val pm = packageManager
                        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS or PackageManager.GET_RECEIVERS
                        val pi: PackageInfo = pm.getPackageInfo(packageName, flags)
                        val sb = StringBuilder()
                        sb.appendLine("Exported components (from manifest)")
                        pi.activities?.filter { it.exported }?.forEach { sb.appendLine("- activity: ${it.name}") }
                        pi.services?.filter { it.exported }?.forEach { sb.appendLine("- service: ${it.name}") }
                        pi.providers?.filter { it.exported }?.forEach { sb.appendLine("- provider: ${it.name} (auth=${it.authority})") }
                        pi.receivers?.filter { it.exported }?.forEach { sb.appendLine("- receiver: ${it.name}") }
                        val out = sb.toString().ifBlank { "<none exported>" }
                        return "$out\n(Compare secure vs vuln builds; see Permission Internals and Web topics for demos.)"
                    }

                    suspend fun testCleartext(): String {
                        return withContext(Dispatchers.IO) {
                            try {
                                val client = OkHttpClient.Builder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .readTimeout(5, TimeUnit.SECONDS)
                                    .build()
                                val req = Request.Builder().url("http://neverssl.com/").build()
                                client.newCall(req).execute().use { resp ->
                                    val ok = resp.isSuccessful
                                    "HTTP cleartext request result: status=${resp.code} (success=$ok). If this succeeded, cleartext was permitted by policy."
                                }
                            } catch (t: Throwable) {
                                Log.w("Risks", "cleartext test failed", t)
                                "HTTP cleartext request failed: ${t::class.java.simpleName}: ${t.message}. Likely blocked by Network Security Config or platform policy."
                            }
                        }
                    }

                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)) {
                        Text("Other Common App Risks Lab")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { output.value = showAppConfig() }) { Text("Show App Config (debuggable, backup, cleartext, FLAG_SECURE)") }
                        Button(onClick = { scope.launch { output.value = testCleartext() } }) { Text("Test Cleartext HTTP (neverssl.com)") }
                        Button(onClick = { output.value = listExported() }) { Text("List Exported Components") }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val msg = dev.jamescullimore.android_security_training.network.provideRisksHelper()
                                .toggleFlagSecure(window)
                            output.value = msg + "\n\n" + showAppConfig()
                        }) { Text("Toggle FLAG_SECURE (mitigate tapjacking/pixnapping)") }
                        Spacer(Modifier.height(12.dp))
                        Text("Open Merged Manifest view in Android Studio to compare secure vs vuln builds. See README Section 10 for OWASP MASVS mapping and real-world leaks.")
                        Spacer(Modifier.height(12.dp))
                        Text(output.value)
                    }
                }
            }
        }
    }
}
