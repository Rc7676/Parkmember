package com.parkmember

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Always-on foreground service that keeps the app process alive and listens for the car.
 *
 * The manifest [BluetoothReceiver] is enough on stock Android, but some phones (notably
 * Xiaomi/POCO) refuse to start a closed app for a broadcast. With this service running the
 * process is already alive, so the car's connect/disconnect events always arrive.
 */
class CarWatcherService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return
            val app = context.applicationContext
            BluetoothReceiver.handleCarEvent(app, intent.action, device.address) {
                scope.launch {
                    val spot = ParkingRecorder.recordParking(app, timeoutMillis = 30_000)
                    Notifications.showParked(app, spot)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        // These are protected system broadcasts, so nobody else can send them to us.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val car = ParkingStore.carDevice(this)
        if (car == null || !Permissions.hasBluetooth(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        Notifications.ensureChannels(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        try {
            ServiceCompat.startForeground(this, Notifications.ID_WATCHING, Notifications.watching(this, car.name), type)
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(receiver)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CarWatcherService"

        /** True while the service (and its receiver) is alive in this process. */
        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            if (ParkingStore.carDevice(context) == null || !Permissions.hasBluetooth(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, CarWatcherService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not start watcher", e)
            }
        }
    }
}
