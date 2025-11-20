package dev.jamescullimore.android_security_training

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.re.ReDemoHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import kotlinx.coroutines.launch

class REActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                REHome()
            }
        }
    }
}

@Composable
fun REHome() {
    val context = LocalContext.current
    val helper: ReDemoHelper = remember { provideReDemoHelper() }
    var dexPath by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Reverse Engineering Lab: Choose an action") }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = "Reverse Engineering APKs", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                result = "Hardcoded secret: ${helper.getHardcodedSecret()}"
            }) { Text("Show Hardcoded Secret") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                result = helper.readLeakyAsset(context)
            }) { Text("Read Leaky Asset") }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = dexPath,
                onValueChange = { dexPath = it },
                label = { Text("Dynamic DEX/JAR or 'self' for app APK") })
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
                scope.launch {
                    result = helper.tryDynamicDexLoad(context, dexPath.ifBlank { "self" })
                }
            }) { Text("Attempt Dynamic DEX Load") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val info = helper.getSigningInfo(context)
                result = "Signing cert SHA-256=\n${info}\nVerified=${
                    helper.verifyExpectedSignature(context)
                }"
            }) { Text("Show App Signature / Verify") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                result =
                    "methodToBeChangedAndResigned() value: ${helper.getMethodToBeChangedAndResignedValue()}"
            }) { Text("Show methodToBeChangedAndResigned Value") }
            Spacer(Modifier.height(16.dp))
            Text("Result:\n$result")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Note: Vulnerable builds leak secrets/assets and allow dynamic code; Secure builds avoid secrets, exclude assets, and enforce signature checks.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview
@Composable
internal fun REHomePreview() {
    REHome()
}
