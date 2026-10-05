package com.parkmember

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the car watcher after the phone reboots or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON", // some OEMs, Xiaomi included
            Intent.ACTION_MY_PACKAGE_REPLACED -> CarWatcherService.start(context)
        }
    }
}
