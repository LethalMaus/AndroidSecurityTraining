package dev.jamescullimore.android_security_training

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

object WebStateHolder {
    @Volatile var lastBroadcast: String? = null
}

class DemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val extras = intent.extras
        val msg = "Received broadcast: action=$action extras=$extras"
        Log.w("WebDemo", msg)
        WebStateHolder.lastBroadcast = msg

        // User-visible feedback so the demo works even without Logcat open
        when (action) {
            "dev.jamescullimore.android_security_training.DEMO" -> {
                val m = intent.getStringExtra("msg")
                Toast.makeText(context, "DEMO broadcast: msg=$m", Toast.LENGTH_LONG).show()
            }
            "dev.jamescullimore.android_security_training.LEAK_PI" -> {
                Toast.makeText(context, "LEAK_PI received — attempting to trigger PendingIntent", Toast.LENGTH_LONG).show()
                val pi = intent.getParcelableExtra<PendingIntent>("pi")
                if (pi != null) {
                    try {
                        Log.w("WebDemo", "Attempting to trigger leaked PendingIntent (this is the insecure demo)...")
                        pi.send(context, 0, null, null, null)
                        Log.w("WebDemo", "Leaked PendingIntent was triggered via BroadcastReceiver")
                        Toast.makeText(context, "Leaked PendingIntent TRIGGERED (WebActivity may pop)", Toast.LENGTH_LONG).show()
                    } catch (t: Throwable) {
                        Log.e("WebDemo", "Failed to trigger PendingIntent", t)
                        Toast.makeText(context, "Failed to trigger PendingIntent: $t", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("WebDemo", "No PendingIntent extra named 'pi' found on LEAK_PI broadcast")
                    Toast.makeText(context, "LEAK_PI: No 'pi' extra found", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                // Generic toast for other actions (defensive)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
