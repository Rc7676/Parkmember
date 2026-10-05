package com.parkmember

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class CarDevice(val address: String, val name: String)

data class ParkedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timeMillis: Long,
)

/** Persists the chosen car Bluetooth device and the last parking spot. */
object ParkingStore {
    private const val PREFS = "parkmember"
    private const val KEY_DEVICE_ADDRESS = "device_address"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_ACCURACY = "accuracy"
    private const val KEY_TIME = "time"
    private const val KEY_DRIVING = "driving"
    private const val KEY_KEEP_ALIVE_DONE = "keep_alive_done"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun carDevice(context: Context): CarDevice? {
        val p = prefs(context)
        val address = p.getString(KEY_DEVICE_ADDRESS, null) ?: return null
        return CarDevice(address, p.getString(KEY_DEVICE_NAME, null) ?: address)
    }

    fun setCarDevice(context: Context, device: CarDevice) = prefs(context).edit {
        putString(KEY_DEVICE_ADDRESS, device.address)
        putString(KEY_DEVICE_NAME, device.name)
    }

    fun isDriving(context: Context): Boolean = prefs(context).getBoolean(KEY_DRIVING, false)

    fun setDriving(context: Context, driving: Boolean) = prefs(context).edit {
        putBoolean(KEY_DRIVING, driving)
    }

    /** Whether the user has been through the "keep it running" setup step. */
    fun isKeepAliveSetupDone(context: Context): Boolean = prefs(context).getBoolean(KEY_KEEP_ALIVE_DONE, false)

    fun setKeepAliveSetupDone(context: Context, done: Boolean) = prefs(context).edit {
        putBoolean(KEY_KEEP_ALIVE_DONE, done)
    }

    fun parkedLocation(context: Context): ParkedLocation? {
        val p = prefs(context)
        if (!p.contains(KEY_LAT) || !p.contains(KEY_LON)) return null
        return ParkedLocation(
            latitude = Double.fromBits(p.getLong(KEY_LAT, 0)),
            longitude = Double.fromBits(p.getLong(KEY_LON, 0)),
            accuracyMeters = if (p.contains(KEY_ACCURACY)) p.getFloat(KEY_ACCURACY, 0f) else null,
            timeMillis = p.getLong(KEY_TIME, 0),
        )
    }

    fun setParkedLocation(context: Context, location: ParkedLocation) = prefs(context).edit {
        putLong(KEY_LAT, location.latitude.toRawBits())
        putLong(KEY_LON, location.longitude.toRawBits())
        if (location.accuracyMeters != null) putFloat(KEY_ACCURACY, location.accuracyMeters)
        else remove(KEY_ACCURACY)
        putLong(KEY_TIME, location.timeMillis)
    }
}
