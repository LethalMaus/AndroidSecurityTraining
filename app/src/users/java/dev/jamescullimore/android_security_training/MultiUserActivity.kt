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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.network.provideMultiUserHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme

class MultiUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val helper = provideMultiUserHelper()
                    val output = remember { mutableStateOf("Ready. Variant: ${BuildConfig.FLAVOR}") }
                    val targetUser = remember { mutableStateOf("10") } // default demo user id

                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)) {
                        Text("Android Multi-User Architecture Lab — Variant: ${BuildConfig.FLAVOR}")
                        Spacer(Modifier.height(8.dp))

                        Button(onClick = { output.value = helper.getRuntimeInfo(this@MultiUserActivity) }) {
                            Text("Show Runtime Info (user/app/uid)")
                        }
                        Button(onClick = { output.value = helper.listUsersBestEffort(this@MultiUserActivity) }) {
                            Text("List Users")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { output.value = helper.savePerUserToken(this@MultiUserActivity, "token-user-${System.currentTimeMillis()}") }) {
                            Text("Save Per-User Token (secure)")
                        }
                        Button(onClick = { output.value = helper.loadPerUserToken(this@MultiUserActivity) }) {
                            Text("Load Per-User Token (secure)")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { output.value = helper.saveGlobalTokenInsecure(this@MultiUserActivity, "GLOBAL-${System.currentTimeMillis()}") }) {
                            Text("Save GLOBAL Token (INSECURE demo)")
                        }
                        Button(onClick = { output.value = helper.loadGlobalTokenInsecure(this@MultiUserActivity) }) {
                            Text("Load GLOBAL Token (INSECURE demo)")
                        }
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = targetUser.value,
                            onValueChange = { targetUser.value = it.filter { ch -> ch.isDigit() }.take(6) },
                            label = { Text("Target userId for cross-user read (adb pm list users)") }
                        )
                        Button(onClick = {
                            val id = targetUser.value.toIntOrNull() ?: -1
                            output.value = helper.tryCrossUserRead(this@MultiUserActivity, id)
                        }) { Text("Try Cross-User Read") }

                        Spacer(Modifier.height(12.dp))
                        Text(output.value)
                    }
                }
            }
        }
    }
}
