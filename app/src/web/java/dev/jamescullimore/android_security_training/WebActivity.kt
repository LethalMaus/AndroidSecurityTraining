package dev.jamescullimore.android_security_training

import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jamescullimore.android_security_training.ui.theme.AndroidSecurityTrainingTheme
import dev.jamescullimore.android_security_training.web.WebViewHelper

class WebActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AndroidSecurityTrainingTheme {
                WebScreen(incoming = intent, provideWebViewHelper())
            }
        }
    }
}

@Composable
fun WebScreen(incoming: Intent?, helper: WebViewHelper) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val ctx = LocalContext.current
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
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    factory = { context ->
                        WebView(context).apply {
                            webViewState.value = this
                            isVerticalScrollBarEnabled = true
                        }
                    },
                    update = {
                        // no-op
                    }
                )
            }

            // If launched via VIEW intent with data, auto-load it once the WebView exists
            LaunchedEffect(webViewState.value) {
                val wv = webViewState.value
                val data = incoming?.data
                if (wv != null && incoming?.action == Intent.ACTION_VIEW && data != null) {
                    // Best-effort configure before loading
                    runCatching { helper.configure(ctx, wv) }
                    output.value = helper.loadFromIntent(ctx, wv, data.toString())
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(onClick = {
                    webViewState.value?.let { output.value = helper.loadTrusted(ctx, it) }
                }) { Text("Load Trusted URL (https)") }

                Button(onClick = {
                    webViewState.value?.let { output.value = helper.loadUntrustedHttp(ctx, it) }
                }) { Text("Load Untrusted HTTP (cleartext)") }

                Button(onClick = {
                    webViewState.value?.let { output.value = helper.loadUntrusted(ctx, it) }
                }) { Text("Load Untrusted FILE (path traversal)") }

                Button(onClick = {
                    webViewState.value?.let { wv ->
                        output.value = helper.loadLocalPayload(ctx, wv)
                    }
                }) { Text("Load Local Payload (app external files)") }

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

@Preview
@Composable
internal fun WebScreenPreview() {
    WebScreen(incoming = null, provideWebViewHelper())
}
