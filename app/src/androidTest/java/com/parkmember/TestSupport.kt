package com.parkmember

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/** Test location; CI keeps feeding it to the emulator's GPS (see .github/qa.sh). */
const val TEST_LAT = 32.0853
const val TEST_LON = 34.7818

const val CAR_ADDRESS = "00:11:22:AA:BB:CC"
val TEST_CAR = CarDevice(CAR_ADDRESS, "Test Car")

object TestSupport {
    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Runs an adb-shell-level command and waits for it to finish. */
    fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().decodeToString() }
    }

    fun grantAllPermissions() {
        val pkg = context.packageName
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.forEach { shell("pm grant $pkg $it") }
        shell("cmd location set-location-enabled true")
    }

    fun resetStore() {
        ParkingStore.prefs(context).edit().clear().commit()
    }

    /** Saved to /data/local/tmp/qa so CI can pull it after the test APK is uninstalled. */
    fun screenshot(name: String) {
        shell("mkdir -p /data/local/tmp/qa")
        shell("screencap -p /data/local/tmp/qa/$name.png")
    }
}
