# Parkmember

An Android app that remembers where you parked, automatically.

## How it works

1. **Setup:** grant permissions, then pick your car's Bluetooth from the devices already paired with your phone.
2. **Phone connects to the car** → you're driving.
3. **Phone disconnects from the car** → you've parked. The app gets a GPS fix and saves it.
4. Open the app to see your car on a map, and tap **Walk to car** for walking directions.

You can also tap **Save spot now** to save your location by hand.

## Technical notes

- Kotlin + Jetpack Compose, min Android 8.0 (API 26), target API 35.
- `BluetoothReceiver` is declared in the manifest and listens for `ACL_CONNECTED` / `ACL_DISCONNECTED`.
  These broadcasts are exempt from Android's background-broadcast limits, so it works while the app is closed.
- On disconnect, `ParkingLocationService` (a short foreground service of type `location`) asks the
  Fused Location Provider for a fresh high-accuracy fix (up to 30 s, falling back to the last known
  location), saves it, posts a notification, and stops.
- `CarWatcherService` is a small always-on foreground service (type `connectedDevice`, silent
  notification) that keeps the process alive and listens for the car itself. Needed on phones such
  as Xiaomi/Redmi/POCO, which refuse to start a closed app for a broadcast. `BootReceiver` restarts
  it after a reboot or app update. When it's running, the manifest receiver steps aside so the spot
  isn't saved twice.
- Setup step 4 ("Keep it running") asks to skip battery optimization and, on Xiaomi/Redmi/POCO,
  links straight to the Autostart and battery-saver screens.
- Data is stored locally in SharedPreferences (`ParkingStore`) and never leaves the phone.
- The map uses OpenStreetMap through osmdroid, so it needs no API key.

## Permissions

| Permission | Why |
|---|---|
| `BLUETOOTH_CONNECT` | Detect the car connecting and disconnecting, and list paired devices |
| Fine + background location | Save the spot while the app is closed (location must be set to "Allow all the time") |
| Foreground service (location) | Get a GPS fix right after disconnect |
| Notifications | Show "Parking spot saved" |

## Build

Open the project in Android Studio and run it, or:

```sh
./gradlew assembleDebug
```

## Xiaomi / Redmi / POCO phones

These phones block background apps by default. In setup step 4 (or the ⚙ button on the main
screen) turn on:

1. **Autostart** for Parkmember
2. **Battery saver → No restrictions**
3. Optional: in recent apps, long-press Parkmember and tap the lock icon

## Signing

`app/debug.keystore` is a throwaway debug key committed on purpose, so every CI build is signed
the same way and installs as an update over the previous one.
