package com.parkmember

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@Composable
fun MainScreen(car: CarDevice, driving: Boolean, spot: ParkedLocation?, onChangeCar: () -> Unit) {
    var recenterRequests by remember { mutableIntStateOf(0) }
    val status = when {
        driving -> CarStatus.Driving
        spot != null -> CarStatus.Parked
        else -> CarStatus.NotParked
    }

    Box(Modifier.fillMaxSize()) {
        if (spot != null) {
            ParkingMap(spot, recenterRequests, Modifier.fillMaxSize())
        } else {
            EmptyBackdrop()
        }

        // Soft fade behind the status bar and the top chips, so they read over any map.
        Box(
            Modifier.fillMaxWidth().height(120.dp).background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), Color.Transparent))
            )
        )
        TopChips(car, onChangeCar)

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (spot != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "© OpenStreetMap contributors",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF333333),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    SmallFloatingActionButton(
                        onClick = { recenterRequests++ },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(painterResource(R.drawable.ic_my_location), contentDescription = "Center on car")
                    }
                }
            }
            BottomSheetCard(car, status, spot)
        }
    }
}

@Composable
private fun TopChips(car: CarDevice, onChangeCar: () -> Unit) {
    Row(
        Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(
                Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandBadge(28.dp)
                Text("Parkmember", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            onClick = onChangeCar,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_bluetooth),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    car.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomSheetCard(car: CarDevice, status: CarStatus, spot: ParkedLocation?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Spacer(Modifier.height(18.dp))
            StatusPill(status)
            Spacer(Modifier.height(12.dp))

            if (spot != null) {
                Text(
                    relativeTime(spot.timeMillis),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    DateUtils.formatDateTime(
                        context,
                        spot.timeMillis,
                        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_TIME,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                spot.accuracyMeters?.let {
                    Text(
                        "Accuracy about ${it.toInt()} m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text("No spot saved yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "It'll be saved automatically when your phone disconnects from “${car.name}”.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (spot != null) {
                    Button(
                        onClick = { navigateTo(context, spot) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(painterResource(R.drawable.ic_walk), contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Walk to car", style = MaterialTheme.typography.titleSmall)
                    }
                }
                FilledTonalButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            val saved = ParkingRecorder.recordParking(context, timeoutMillis = 15_000)
                            saving = false
                            Toast.makeText(
                                context,
                                if (saved != null) "Parking spot saved" else "Couldn't get your location. Is location turned on?",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                    }
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (saving) "Saving…" else "Save spot now", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/** "Just now", "5 minutes ago", … — re-evaluated every 30 s so it stays current. */
@Composable
private fun relativeTime(timeMillis: Long): String {
    val now by produceState(System.currentTimeMillis(), timeMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(30_000)
        }
    }
    return if (now - timeMillis < DateUtils.MINUTE_IN_MILLIS) "Just now"
    else DateUtils.getRelativeTimeSpanString(timeMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

@Composable
private fun EmptyBackdrop() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.offset(y = (-90).dp).size(168.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
        }
    }
}

@Composable
private fun ParkingMap(spot: ParkedLocation, recenterRequests: Int, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dark = isSystemInDarkTheme()
    val point = GeoPoint(spot.latitude, spot.longitude)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(18.0)
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_car_pin)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your car"
            setOnMarkerClickListener { _, _ -> true } // no info bubble
            mapView.overlays.add(this)
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

    // Center when the spot changes, and animate back when the user taps "center".
    LaunchedEffect(spot) { mapView.controller.setCenter(point) }
    LaunchedEffect(recenterRequests) {
        if (recenterRequests > 0) mapView.controller.animateTo(point, 18.0, 600L)
    }

    AndroidView(factory = { mapView }, modifier = modifier, update = { map ->
        map.overlayManager.tilesOverlay.setColorFilter(if (dark) TilesOverlay.INVERTED_COLORS else null)
        marker.position = point
        map.invalidate()
    })
}
