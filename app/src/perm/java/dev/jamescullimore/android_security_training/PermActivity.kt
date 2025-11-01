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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.perm.PermDemoHelper
import dev.jamescullimore.android_security_training.network.providePermDemoHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme

class PermActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermHome(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PermHome(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val helper: PermDemoHelper = remember { providePermDemoHelper() }
    var result by remember { mutableStateOf("Permission Internals & App Packaging demo") }
    var uriText by remember { mutableStateOf("") }

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = "Permissions & Packaging", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            result = helper.uidGidAndSignatureInfo(context)
        }) { Text("Show UID/GID & Signing Info") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            result = helper.tryStartProtectedService(context)
        }) { Text("Start DemoService") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = uriText, onValueChange = { uriText = it }, label = { Text("Content URI (optional)") })
        Spacer(Modifier.height(4.dp))
        Button(onClick = {
            result = helper.tryQueryDemoProvider(context, uriText.ifBlank { helper.defaultDemoUri(context) })
        }) { Text("Query DemoProvider") }
        Spacer(Modifier.height(16.dp))
        Text("Result:\n$result")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Secure builds restrict exported components and require a custom signature permission. Vuln builds export components without protection to show risks.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
