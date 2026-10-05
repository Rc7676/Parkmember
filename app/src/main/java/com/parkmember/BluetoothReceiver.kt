package com.parkmember

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Connected to the car's Bluetooth  -> the user is driving.
 * Disconnected from it             -> the user just parked, so save the location.
 */
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // When the always-on watcher is alive it handles the event itself; don't save twice.
        if (CarWatcherService.isRunning) return
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            ?: return
        handleCarEvent(context, intent.action, device.address) { recordInline(context.applicationContext) }
    }

    /** Fallback if the foreground service can't be started: a quick fix within the broadcast window. */
    private fun recordInline(context: Context) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val spot = ParkingRecorder.recordParking(context, timeoutMillis = 8_000)
                Notifications.showParked(context, spot)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothReceiver"

        /**
         * Reacts to the car connecting or disconnecting. Ignores every other device.
         * [recordWithoutService] runs if the foreground service can't be started.
         */
        fun handleCarEvent(context: Context, action: String?, address: String, recordWithoutService: () -> Unit) {
            val car = ParkingStore.carDevice(context) ?: return
            if (!address.equals(car.address, ignoreCase = true)) return

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.i(TAG, "Connected to car ${car.name}")
                    ParkingStore.setDriving(context, true)
                    Notifications.clearParked(context)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from car ${car.name}, saving parking spot")
                    ParkingStore.setDriving(context, false)
                    if (!ParkingLocationService.start(context)) recordWithoutService()
                }
            }
        }
    }
}
