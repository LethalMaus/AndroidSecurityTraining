package dev.jamescullimore.android_security_training

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jamescullimore.android_security_training.network.provideWebViewHelper
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme

class WebActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val ctx = LocalContext.current
                    val helper = provideWebViewHelper()
                    val output = remember { mutableStateOf("Ready.") }
                    val webViewState = remember { mutableStateOf<WebView?>(null) }

                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)) {
                        Text("WebView & Component Security Lab — Variant: ${BuildConfig.FLAVOR}", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { context ->
                                    WebView(context).apply {
                                        webViewState.value = this
                                    }
                                },
                                update = {
                                    // no-op
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Button(onClick = {
                                webViewState.value?.let { output.value = helper.configure(ctx, it) }
                            }) { Text("Configure WebView") }

                            Button(onClick = {
                                webViewState.value?.let { output.value = helper.loadTrusted(ctx, it) }
                            }) { Text("Load Trusted URL (https)") }

                            Button(onClick = {
                                webViewState.value?.let { output.value = helper.loadUntrusted(ctx, it) }
                            }) { Text("Load Untrusted URL (http/file)") }

                            Button(onClick = {
                                webViewState.value?.let { output.value = helper.runDemoJs(ctx, it) }
                            }) { Text("Run JS Demo / Bridge Call") }

                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                output.value = helper.sendInternalBroadcast(ctx)
                            }) { Text("Send Internal Broadcast") }

                            Button(onClick = {
                                output.value = helper.exposePendingIntent(ctx)
                            }) { Text("Expose PendingIntent (demo)") }

                            Spacer(Modifier.height(12.dp))
                            Text(output.value)
                        }
                    }
                }
            }
        }
    }
}
