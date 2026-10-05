package com.parkmember

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Grabs the phone's current location and stores it as the parking spot. */
object ParkingRecorder {
    private const val TAG = "ParkingRecorder"

    /**
     * Tries for a fresh, accurate fix for up to [timeoutMillis]; falls back to the last
     * known location. Returns the saved spot, or null if no location was available.
     */
    suspend fun recordParking(context: Context, timeoutMillis: Long): ParkedLocation? {
        val location = currentLocation(context, timeoutMillis) ?: return null
        val spot = ParkedLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            timeMillis = System.currentTimeMillis(),
        )
        ParkingStore.setParkedLocation(context, spot)
        return spot
    }

    @SuppressLint("MissingPermission") // checked via Permissions.hasLocation
    private suspend fun currentLocation(context: Context, timeoutMillis: Long): Location? = coroutineScope {
        if (!Permissions.hasLocation(context)) return@coroutineScope null
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            val cancel = CancellationTokenSource()
            val fresh = withTimeoutOrNull(timeoutMillis) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token).await()
            }
            if (fresh == null) cancel.cancel()
            fresh ?: client.lastLocation.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not get location", e)
            null
        }
    }
}
