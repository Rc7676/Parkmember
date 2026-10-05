package com.parkmember

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@MainActivity, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
        Notifications.ensureChannels(this)
        setContent { ParkmemberTheme { ParkmemberApp() } }
    }
}

@Composable
private fun ParkmemberTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val hasBasePermissions = remember(version) { Permissions.hasLocation(context) && Permissions.hasBluetooth(context) }
    val hasBackground = remember(version) { Permissions.hasBackgroundLocation(context) }
    val car = remember(version) { ParkingStore.carDevice(context) }
    val driving = remember(version) { ParkingStore.isDriving(context) }
    val spot = remember(version) { ParkingStore.parkedLocation(context) }

    Scaffold(topBar = { TopAppBar(title = { Text("Parkmember") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                !hasBasePermissions -> PermissionStep { version++ }
                !hasBackground -> BackgroundLocationStep { version++ }
                car == null || pickingDevice -> DevicePicker(
                    onPicked = {
                        ParkingStore.setCarDevice(context, it)
                        pickingDevice = false
                        version++
                    },
                    onCancel = if (car != null) ({ pickingDevice = false }) else null,
                )
                else -> MainScreen(
                    car = car,
                    driving = driving,
                    spot = spot,
                    onChangeCar = { pickingDevice = true },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Setup steps

@Composable
private fun SetupCard(title: String, body: String, content: @Composable () -> Unit) {
    Card(Modifier.padding(16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            content()
        }
    }
}

@Composable
private fun PermissionStep(onResult: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onResult() }
    SetupCard(
        title = "Step 1 of 3 · Permissions",
        body = "Parkmember needs Bluetooth access to notice when your phone connects to your car, " +
            "and location access to remember where you parked.",
    ) {
        Button(onClick = { launcher.launch(Permissions.foregroundPermissions()) }) { Text("Grant permissions") }
        TextButton(onClick = { openAppSettings(context) }) { Text("Open app settings") }
    }
}

@Composable
private fun BackgroundLocationStep(onResult: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    SetupCard(
        title = "Step 2 of 3 · Location while closed",
        body = "To save your spot automatically when you leave the car, location must be allowed " +
            "\"All the time\". Your location is only read at the moment your car's Bluetooth disconnects, " +
            "and it never leaves your phone.",
    ) {
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                launcher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }) { Text("Allow all the time") }
        TextButton(onClick = { openAppSettings(context) }) { Text("Open app settings") }
    }
}

@SuppressLint("MissingPermission") // only shown once BLUETOOTH_CONNECT is granted
private fun pairedDevices(context: Context): List<CarDevice>? {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
    return try {
        adapter.bondedDevices.orEmpty()
            .map { CarDevice(it.address, it.name ?: it.address) }
            .sortedBy { it.name.lowercase() }
    } catch (e: SecurityException) {
        emptyList()
    }
}

@Composable
private fun DevicePicker(onPicked: (CarDevice) -> Unit, onCancel: (() -> Unit)?) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val devices = remember(refresh) { pairedDevices(context) }

    Column {
        SetupCard(
            title = "Step 3 of 3 · Choose your car",
            body = "Pick your car's Bluetooth from the devices paired with this phone. " +
                "When the phone disconnects from it, Parkmember saves where you are.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { refresh++ }) { Text("Refresh") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }) { Text("Pair a new device") }
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
        when {
            devices == null -> Text("This phone has no Bluetooth.", Modifier.padding(16.dp))
            devices.isEmpty() -> Text(
                "No paired devices found. Turn on Bluetooth and pair your phone with your car first.",
                Modifier.padding(16.dp),
            )
            else -> LazyColumn {
                items(devices, key = { it.address }) { device ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onPicked(device) }.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(device.name, style = MaterialTheme.typography.bodyLarge)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Main screen

@Composable
private fun MainScreen(car: CarDevice, driving: Boolean, spot: ParkedLocation?, onChangeCar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(if (driving) "Driving" else "Parked") })
                AssistChip(onClick = onChangeCar, label = { Text("Car: ${car.name}") })
            }
            if (spot != null) {
                Text(
                    "Parked " + DateUtils.getRelativeTimeSpanString(spot.timeMillis) +
                        " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(spot.timeMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                spot.accuracyMeters?.let {
                    Text("Accuracy about ${it.toInt()} m", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (spot != null) {
            ParkingMap(spot, Modifier.weight(1f).fillMaxWidth())
        } else {
            Text(
                "No parking spot saved yet. It will be saved automatically the next time your phone " +
                    "disconnects from \"${car.name}\".",
                Modifier.padding(16.dp).weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (spot != null) {
                Button(onClick = { navigateTo(context, spot) }, Modifier.weight(1f)) { Text("Walk to car") }
            }
            OutlinedButton(
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        ParkingRecorder.recordParking(context, timeoutMillis = 15_000)
                        saving = false
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (saving) "Saving…" else "Save spot now") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ParkingMap(spot: ParkedLocation, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier, update = { map ->
        val point = GeoPoint(spot.latitude, spot.longitude)
        map.overlays.removeAll { it is Marker }
        map.overlays.add(Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your car"
        })
        map.controller.setCenter(point)
        map.invalidate()
    })
}

// ---------------------------------------------------------------- Helpers

private fun navigateTo(context: Context, spot: ParkedLocation) {
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

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )
}
