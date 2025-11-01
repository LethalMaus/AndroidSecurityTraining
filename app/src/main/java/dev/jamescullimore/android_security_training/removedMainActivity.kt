package dev.jamescullimore.android_security_training

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.network.NetworkHelper
import dev.jamescullimore.android_security_training.network.provideNetworkHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrainingHome(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TrainingHome(modifier: Modifier = Modifier) {
    val helper: NetworkHelper = remember { provideNetworkHelper() }
    var result by remember { mutableStateOf("Press Run Demo to make a request") }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = "Certificate Pinning & Transparency",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        // Mode toggles (secure pinning topic will honor these; vuln ignores)
        Text("Mode:")
        Spacer(Modifier.height(4.dp))
        Column {
            Button(onClick = { helper.setPinningMode("bad"); result = "Mode set to bad pins (expect failure)" }) { Text("Use BAD pins (should fail)") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = { helper.setPinningMode("good"); result = "Mode set to good pins (expect success)" }) { Text("Use GOOD pins (should work)") }
            Spacer(Modifier.height(4.dp))
            Button(onClick = { helper.setPinningMode("ct"); result = "Mode set to CT-only (no pins)" }) { Text("Use CT-only (no pins)") }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                result = try {
                    helper.fetchDemo()
                } catch (t: Throwable) {
                    "Error: ${t.javaClass.simpleName}: ${t.message}"
                }
            }
        }) {
            Text("Run Demo Request")
        }
        Spacer(Modifier.height(16.dp))
        Text("Result: \n$result")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Note: Secure builds can toggle between bad pins (fail), good pins (success), and CT-only via the buttons above. Vulnerable builds ignore this and trust user CAs.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingHomePreview() {
    AndroidSecurityTrainingTheme {
        TrainingHome()
    }
}