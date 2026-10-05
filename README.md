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
