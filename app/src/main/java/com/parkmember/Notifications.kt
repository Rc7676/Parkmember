package com.parkmember

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.DateFormat
import java.util.Date

object Notifications {
    private const val CHANNEL_PARKED = "parked"
    private const val CHANNEL_WORKING = "working"
    const val ID_WORKING = 1
    private const val ID_PARKED = 2

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PARKED, "Parking saved", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_WORKING, "Saving parking location", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun working(context: Context) = NotificationCompat.Builder(context, CHANNEL_WORKING)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Saving where you parked…")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    @SuppressLint("MissingPermission") // checked via Permissions.hasNotifications
    fun showParked(context: Context, spot: ParkedLocation?) {
        if (!Permissions.hasNotifications(context)) return
        ensureChannels(context)
        val text = if (spot != null) {
            "Saved at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(spot.timeMillis)) +
                ". Tap to see where your car is."
        } else {
            "Couldn't get your location. Tap to save it manually."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_PARKED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (spot != null) "Parking spot saved" else "Parking spot not saved")
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_PARKED, notification)
    }

    fun clearParked(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PARKED)
}
