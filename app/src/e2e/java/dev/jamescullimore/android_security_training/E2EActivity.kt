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
import androidx.compose.ui.unit.dp
import dev.jamescullimore.android_security_training.crypto.CryptoHelper
import dev.jamescullimore.android_security_training.network.provideCryptoHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import kotlinx.coroutines.launch
import android.util.Base64

class E2EActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    E2EHome(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun E2EHome(modifier: Modifier = Modifier) {
    val crypto: CryptoHelper = remember { provideCryptoHelper() }
    var input by remember { mutableStateOf("{\"msg\":\"Hello, world!\"}") }
    var result by remember { mutableStateOf("Enter JSON and choose an action") }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = "Encrypting Data Before Transport",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("JSON Payload") },
            minLines = 3
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val aad = "v1".toByteArray()
            val enc = crypto.encryptAesGcm(input.toByteArray(), aad)
            val b64Iv = Base64.encodeToString(enc.iv, Base64.NO_WRAP)
            val b64Ct = Base64.encodeToString(enc.cipherText, Base64.NO_WRAP)
            val b64Tag = Base64.encodeToString(enc.tag, Base64.NO_WRAP)
            result = "AES-GCM OK\nIV=$b64Iv\nCT=$b64Ct\nTAG=$b64Tag"
        }) { Text("Encrypt Locally (AES-GCM)") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                result = try {
                    crypto.postEncryptedJson("https://postman-echo.com/post", input)
                } catch (t: Throwable) {
                    "Error: ${t.javaClass.simpleName}: ${t.message}"
                }
            }
        }) { Text("Encrypt + Send via HTTPS") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val serverPem = """
                -----BEGIN PUBLIC KEY-----
                MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAESHikZYG7KDqWy5VPFAV8Onu1+msM
                GGFxlwWBHRlM/1QlPnrJxvceqHbv98CaRMTQ+N0uaoiLddQjCvUnQoqyhQ==
                -----END PUBLIC KEY-----
            """.trimIndent()
            val info = crypto.performEcdhKeyAgreement(serverPem)
            result = "ECDH demo:\nOur pubkey=\n${info.publicKeyPem}\nsharedSecretBytes=${info.sharedSecretBytes}, derivedKeyBytes=${info.derivedKeyBytes}"
        }) { Text("ECDH Derive Session Key (Demo)") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val b64 = Base64.encodeToString(crypto.encodeBase64Only(input.toByteArray()), Base64.NO_WRAP)
            result = "Encoding is NOT encryption. Base64=\n$b64"
        }) { Text("Encoding vs Encryption (Base64)") }
        Spacer(Modifier.height(16.dp))
        Text("Result:\n$result")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Note: Secure flavors use AES-GCM and proper key exchange; Vulnerable flavors demonstrate ECB, static keys, and Base64 misuse.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
