package com.parkmember

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service: gets a GPS fix right after the car disconnects,
 * saves it, shows a "parking saved" notification and stops itself.
 */
class ParkingLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        try {
            ServiceCompat.startForeground(this, Notifications.ID_WORKING, Notifications.working(this), type)
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!running) {
            running = true
            scope.launch {
                val spot = ParkingRecorder.recordParking(this@ParkingLocationService, timeoutMillis = 30_000)
                Notifications.showParked(this@ParkingLocationService, spot)
                ServiceCompat.stopForeground(this@ParkingLocationService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ParkingLocationService"

        /** Returns false if Android refused to start the service. */
        fun start(context: Context): Boolean {
            if (!Permissions.hasLocation(context)) return false
            return try {
                ContextCompat.startForegroundService(context, Intent(context, ParkingLocationService::class.java))
                true
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException on Android 12+
                Log.w(TAG, "Could not start service", e)
                false
            }
        }
    }
}
