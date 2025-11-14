package dev.jamescullimore.android_security_training

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.deeplink.DeepLinkHelper
import dev.jamescullimore.android_security_training.network.provideDeepLinkHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import androidx.core.net.toUri

class DeepLinksActivity : ComponentActivity() {
    private val currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentIntentState.value = intent
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DeepLinksHome(
                        modifier = Modifier.padding(innerPadding),
                        currentIntent = currentIntentState.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // When the activity is already on top (singleTop), new deep links arrive here
        currentIntentState.value = intent
    }
}

@Composable
fun DeepLinksHome(modifier: Modifier = Modifier, currentIntent: Intent?) {
    val context = LocalContext.current
    val helper: DeepLinkHelper = remember { provideDeepLinkHelper() }
    var received by remember { mutableStateOf(helper.describeIncomingIntent(currentIntent)) }
    var uriText by remember { mutableStateOf("https://lab.example.com/welcome?code=abc&state=123") }
    val verifiedBase = "https://lethalmaus.github.io/AndroidSecurityTraining"
    val verifiedUrl = "$verifiedBase/welcome?code=abc&state=123"
    var result by remember { mutableStateOf("Deep Links demo: craft and send VIEW intents") }

    // Update the displayed received-intent summary whenever a new intent arrives
    LaunchedEffect(currentIntent) {
        received = helper.describeIncomingIntent(currentIntent)
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = "Deep Links", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Received Intent:\n$received")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = uriText, onValueChange = { uriText = it }, label = { Text("URI to VIEW") })
        Spacer(Modifier.height(4.dp))
        Button(onClick = {
            uriText = if (uriText.startsWith(verifiedBase)) {
                "https://lab.example.com/welcome?code=abc&state=123"
            } else {
                verifiedUrl
            }
        }) { Text(if (uriText.startsWith(verifiedBase)) "Switch to Unverified URL" else "Switch to Verified URL") }
        Spacer(Modifier.height(4.dp))
        Text(text = if (uriText.startsWith(verifiedBase)) "Mode: Verified app link (should be accepted)" else "Mode: Unverified/custom link (should be rejected)")
        Spacer(Modifier.height(4.dp))
        Button(onClick = {
            val test = Intent(Intent.ACTION_VIEW, uriText.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            test.setClass(context, DeepLinksActivity::class.java)
            context.startActivity(test)
            result = "Sent VIEW intent"
        }) { Text("Send VIEW Intent") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            result = helper.safeNavigateExample(context, uriText)
        }) { Text("Navigate Internally (Secure Path)") }
        Spacer(Modifier.height(16.dp))
        Text("Result:\n$result")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Note: Secure builds validate scheme/host/path and auto-verify app links; Vulnerable builds accept broad http(s) links and echo untrusted data.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
