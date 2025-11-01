package dev.jamescullimore.android_security_training.perm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DemoService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("PermDemo", "DemoService started by ${intent?.`package`}")
        // Do nothing, just a demo
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
