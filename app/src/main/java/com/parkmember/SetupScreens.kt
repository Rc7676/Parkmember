package com.parkmember

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private const val TOTAL_STEPS = 3

/**
 * Shared layout for the setup steps: brand header, progress, scrollable content,
 * and the action buttons pinned to the bottom.
 */
@Composable
private fun SetupLayout(
    step: Int?,
    title: String,
    body: String,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandBadge(40.dp)
                Text("Parkmember", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
            if (step != null) {
                StepIndicator(step)
                Spacer(Modifier.height(24.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(TOTAL_STEPS) { i ->
                Box(
                    Modifier.weight(1f).height(6.dp).clip(CircleShape).background(
                        if (i < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
        Text(
            "Step $step of $TOTAL_STEPS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------- Step 1

@Composable
fun PermissionStep(onResult: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onResult() }
    SetupLayout(
        step = 1,
        title = "Never forget where you parked",
        body = "Parkmember saves your spot automatically the moment you leave your car. First, it needs a few permissions.",
        actions = {
            PrimaryButton("Continue") { launcher.launch(Permissions.foregroundPermissions()) }
            TextButton(onClick = { openAppSettings(context) }, Modifier.fillMaxWidth()) { Text("Open app settings") }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            FeatureRow(painterResource(R.drawable.ic_bluetooth), "Bluetooth", "Notice when your phone connects to your car")
            FeatureRow(
                rememberVectorPainter(Icons.Filled.LocationOn),
                "Precise location",
                "Remember exactly where you parked. Choose \"Precise\" — approximate can be off by a kilometre.",
            )
            FeatureRow(rememberVectorPainter(Icons.Filled.Notifications), "Notifications", "Let you know when your spot is saved")
        }
    }
}

// ---------------------------------------------------------------- Step 2

@Composable
fun BackgroundLocationStep(onResult: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    SetupLayout(
        step = 2,
        title = "Allow location all the time",
        body = "So your spot is saved even when Parkmember is closed — which is most of the time.",
        actions = {
            PrimaryButton("Allow all the time") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    launcher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            TextButton(onClick = { openAppSettings(context) }, Modifier.fillMaxWidth()) { Text("Open app settings") }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            FeatureRow(rememberVectorPainter(Icons.Filled.Lock), "Stays on your phone", "Your location is never uploaded anywhere")
            FeatureRow(painterResource(R.drawable.ic_car), "Only when you park", "Read once, when your car's Bluetooth disconnects")
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Text(
                        "On the next screen, pick \"Allow all the time\".",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Step 3

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
fun DevicePicker(current: CarDevice?, onPicked: (CarDevice) -> Unit, onCancel: (() -> Unit)?) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    // Re-read when coming back from Bluetooth settings after pairing a new device.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val devices = remember(refresh) { pairedDevices(context) }

    SetupLayout(
        step = if (onCancel == null) 3 else null,
        title = "Choose your car",
        body = "Pick your car's Bluetooth from the devices paired with this phone.",
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Pair a new device")
                }
                FilledTonalButton(onClick = { refresh++ }, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel, Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        },
    ) {
        when {
            devices == null -> EmptyDevices("This phone has no Bluetooth.")
            devices.isEmpty() -> EmptyDevices("No paired devices yet. Turn on Bluetooth and pair your phone with your car first.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                devices.forEach { device ->
                    DeviceCard(device, selected = device.address == current?.address) { onPicked(device) }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: CarDevice, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBubble(painterResource(R.drawable.ic_bluetooth), size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyDevices(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBubble(
            painterResource(R.drawable.ic_bluetooth),
            size = 72.dp,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
