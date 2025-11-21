package dev.jamescullimore.android_security_training

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.root.RootHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

class RootActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                RootScreen(
                    onToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
}

@Composable
fun RootScreen(onToast: (String) -> Unit) {
    var signals by remember { mutableStateOf<List<RootHelper.RootSignal>>(emptyList()) }
    var rooted by remember { mutableStateOf<Boolean?>(null) }
    var tamperOk by remember { mutableStateOf<Boolean?>(null) }
    var integrity by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val helper = provideRootHelper()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Text(text = "Variant: ${BuildConfig.FLAVOR}")
            Text(text = helper.deviceInfo())

            Button(onClick = {
                signals = helper.getSignals(ctx)
                rooted = helper.isRooted(ctx)
            }, modifier = Modifier.padding(top = 12.dp)) { Text("Run Root Checks") }

            rooted?.let { Text("isRooted: $it") }
            if (signals.isNotEmpty()) {
                Text("Signals:")
                signals.forEach { s ->
                    Text("- ${s.name}: ${s.detected}${s.details?.let { d -> " — $d" } ?: ""}")
                }
            }

            Button(onClick = {
                val r = helper.isRooted(ctx)
                if (r) onToast("Blocked: device appears rooted") else onToast("Allowed: not rooted")
            }, modifier = Modifier.padding(top = 12.dp)) { Text("Simulate Block if Rooted") }

            var bypassEnabled by remember { mutableStateOf(false) }
            Button(onClick = {
                bypassEnabled = !bypassEnabled
                helper.setBypassEnabled(bypassEnabled)
                onToast("Bypass toggle: $bypassEnabled (effective in vuln builds)")
            }, modifier = Modifier.padding(top = 12.dp)) { Text("Toggle Bypass (vuln)") }

            Button(onClick = {
                tamperOk = helper.tamperCheck(ctx)
            }, modifier = Modifier.padding(top = 12.dp)) { Text("Tamper Check") }
            tamperOk?.let { Text("Tamper check passed: $it") }

            Button(
                onClick = {
                    integrity = helper.playIntegrityStatus(ctx)
                },
                modifier = Modifier.padding(top = 12.dp)
            ) { Text("Play Integrity Status (placeholder)") }
            integrity?.let { Text("Integrity: $it") }
        }
    }
}


@Preview
@Composable
internal fun RootScreenPreview() {
    RootScreen(onToast = {})
}
