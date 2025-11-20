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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import kotlinx.coroutines.launch

class StorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                StorageScreen()
            }
        }
    }
}

@Composable
fun StorageScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val output = remember { mutableStateOf("Ready.") }
    val helper = remember { provideStorageHelper() }
    val root = remember { provideRootHelper() }
    val scope = rememberCoroutineScope()

    fun guardIfRooted(action: suspend () -> String): suspend () -> String = {
        if (root.isRooted(context)) {
            "[SECURE] Blocked action due to detected root/jailbreak (demo). Use Root topic to discuss policy.)"
        } else action()
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = "Local Data Storage Lab")
            Spacer(Modifier.height(8.dp))
            // Preferences (secure)
            Button(onClick = {
                scope.launch {
                    output.value =
                        guardIfRooted { helper.saveTokenSecure(context, "secret-token-123") }()
                }
            }) { Text("Save Token (EncryptedSharedPreferences)") }
            Button(onClick = {
                scope.launch { output.value = helper.loadTokenSecure(context) }
            }) { Text("Load Token (Encrypted)") }
            Spacer(Modifier.height(8.dp))
            // Preferences (insecure)
            Button(onClick = {
                scope.launch {
                    output.value = helper.saveTokenInsecure(context, "secret-token-123")
                }
            }) { Text("Save Token (Plain SharedPreferences)") }
            Button(onClick = {
                scope.launch { output.value = helper.loadTokenInsecure(context) }
            }) { Text("Load Token (Plain)") }
            Spacer(Modifier.height(8.dp))
            // Files
            Button(onClick = {
                scope.launch {
                    output.value = guardIfRooted {
                        helper.writeSecureFile(
                            context,
                            "secure.txt",
                            "Sensitive file content"
                        )
                    }()
                }
            }) { Text("Write Secure File (EncryptedFile)") }
            Button(onClick = {
                scope.launch {
                    output.value =
                        helper.writeInsecureFile(context, "insecure.txt", "Sensitive file content")
                }
            }) { Text("Write Insecure File (Plaintext)") }
            Button(onClick = {
                scope.launch { output.value = helper.readInsecureFile(context, "insecure.txt") }
            }) { Text("Read Insecure File") }
            Spacer(Modifier.height(8.dp))
            // SQLite
            Button(onClick = {
                scope.launch {
                    output.value =
                        guardIfRooted { helper.dbPut(context, "session_token", "db-secret-456") }()
                }
            }) { Text("DB Put (session_token)") }
            Button(onClick = {
                scope.launch { output.value = helper.dbGet(context, "session_token") }
            }) { Text("DB Get (session_token)") }
            Button(onClick = {
                scope.launch { output.value = helper.dbList(context) }
            }) { Text("DB List All") }
            Button(onClick = {
                scope.launch { output.value = helper.dbDelete(context, "session_token") }
            }) { Text("DB Delete (session_token)") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    output.value = "Root signals: \n" + root.getSignals(context)
                        .joinToString("\n") + "\nIs rooted? " + root.isRooted(context)
                }
            }) { Text("Run Root Checks (from Root topic helper)") }
            Spacer(Modifier.height(12.dp))
            Text(text = output.value)
        }
    }
}

@Preview
@Composable
internal fun StorageScreenPreview() {
    StorageScreen()
}
