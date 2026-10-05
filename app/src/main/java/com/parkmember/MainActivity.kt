package com.parkmember

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@MainActivity, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
        Notifications.ensureChannels(this)
        setContent {
            ParkmemberTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ParkmemberApp()
                }
            }
        }
    }
}

private enum class Screen { Permissions, BackgroundLocation, PickCar, KeepAlive, Main }

@Composable
private fun ParkmemberApp() {
    val context = LocalContext.current

    // Bump this to re-read state: on resume (permissions may have changed in Settings)
    // and whenever the receiver/service writes new data.
    var version by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { version++ }
    DisposableEffect(Unit) {
        val prefs = ParkingStore.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var pickingDevice by remember { mutableStateOf(false) }
    var revisitingKeepAlive by remember { mutableStateOf(false) }

    val hasBasePermissions = remember(version) { Permissions.hasPreciseLocation(context) && Permissions.hasBluetooth(context) }
    val hasBackground = remember(version) { Permissions.hasBackgroundLocation(context) }
    val car = remember(version) { ParkingStore.carDevice(context) }
    val driving = remember(version) { ParkingStore.isDriving(context) }
    val spot = remember(version) { ParkingStore.parkedLocation(context) }
    val keepAliveDone = remember(version) { ParkingStore.isKeepAliveSetupDone(context) }
    val batteryOk = remember(version) { KeepAlive.isIgnoringBatteryOptimizations(context) }

    // Keep the always-on watcher running whenever the app is set up.
    LaunchedEffect(car, hasBasePermissions) {
        if (car != null && hasBasePermissions) CarWatcherService.start(context)
    }

    val screen = when {
        !hasBasePermissions -> Screen.Permissions
        !hasBackground -> Screen.BackgroundLocation
        car == null || pickingDevice -> Screen.PickCar
        !keepAliveDone || revisitingKeepAlive -> Screen.KeepAlive
        else -> Screen.Main
    }

    AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { target ->
        when (target) {
            Screen.Permissions -> PermissionStep { version++ }
            Screen.BackgroundLocation -> BackgroundLocationStep { version++ }
            Screen.PickCar -> DevicePicker(
                current = car,
                onPicked = {
                    ParkingStore.setCarDevice(context, it)
                    pickingDevice = false
                    version++
                },
                onCancel = if (car != null) ({ pickingDevice = false }) else null,
            )
            Screen.KeepAlive -> KeepAliveStep(
                isRevisit = keepAliveDone,
                onDone = {
                    ParkingStore.setKeepAliveSetupDone(context, true)
                    revisitingKeepAlive = false
                    version++
                },
            )
            Screen.Main -> if (car != null) {
                MainScreen(
                    car = car,
                    driving = driving,
                    spot = spot,
                    keepAliveOk = batteryOk,
                    onChangeCar = { pickingDevice = true },
                    onOpenKeepAlive = { revisitingKeepAlive = true },
                )
            }
        }
    }
}

fun navigateTo(context: Context, spot: ParkedLocation) {
    val lat = spot.latitude
    val lon = spot.longitude
    val googleMaps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon&mode=w"))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(googleMaps)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(My car)")))
        } catch (_: ActivityNotFoundException) {
        }
    }
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )
}
