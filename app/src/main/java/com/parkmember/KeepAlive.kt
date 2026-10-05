package com.parkmember

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helpers for the settings that let Parkmember run while closed. Stock Android only needs
 * the battery exemption; Xiaomi / Redmi / POCO (MIUI, HyperOS) additionally block apps from
 * being started in the background unless "Autostart" is on.
 */
object KeepAlive {
    private const val TAG = "KeepAlive"

    val isXiaomi: Boolean
        get() = listOf(Build.MANUFACTURER, Build.BRAND).any {
            it.lowercase() in setOf("xiaomi", "redmi", "poco")
        }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    @SuppressLint("BatteryLife") // the whole point of the app is to run in the background
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        if (!tryStart(context, request)) {
            tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) || tryAppSettings(context)
        }
    }

    /** MIUI/HyperOS "Autostart" list. */
    fun openXiaomiAutostart(context: Context) {
        val autostart = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        tryStart(context, autostart) || tryAppSettings(context)
    }

    /** MIUI/HyperOS per-app battery saver, where "No restrictions" can be chosen. */
    fun openXiaomiBatterySaver(context: Context) {
        val batterySaver = Intent()
            .setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
            .putExtra("package_name", context.packageName)
            .putExtra("package_label", context.getString(R.string.app_name))
        tryStart(context, batterySaver) || tryAppSettings(context)
    }

    private fun tryAppSettings(context: Context): Boolean = tryStart(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        // ActivityNotFoundException, or SecurityException if the OEM screen isn't exported.
        Log.w(TAG, "Couldn't open ${intent.component ?: intent.action}", e)
        false
    }
}
