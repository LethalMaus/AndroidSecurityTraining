package dev.jamescullimore.android_security_training

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

// Intentionally exported in the vulnerable flavor for training/demo purposes
class ExportedService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Simple side effect so it’s visible when started from adb
        Toast.makeText(this, "[VULN] ExportedService started", Toast.LENGTH_LONG).show()
        // Auto-stop after showing the toast
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
