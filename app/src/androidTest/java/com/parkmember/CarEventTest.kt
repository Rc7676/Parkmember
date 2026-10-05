package com.parkmember

import android.bluetooth.BluetoothDevice
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises what happens when the car connects / disconnects. Android doesn't let tests send
 * the real (protected) Bluetooth broadcast, so these call the receiver's handler directly.
 */
@RunWith(AndroidJUnit4::class)
class CarEventTest {
    private val context get() = TestSupport.context

    @Before
    fun setUp() {
        TestSupport.grantAllPermissions()
        TestSupport.resetStore()
        ParkingStore.setCarDevice(context, TEST_CAR)
    }

    private fun event(action: String, address: String, fallback: () -> Unit = {}) =
        BluetoothReceiver.handleCarEvent(context, action, address, fallback)

    @Test
    fun connectingToCarMeansDriving() {
        event(BluetoothDevice.ACTION_ACL_CONNECTED, CAR_ADDRESS.lowercase())
        assertTrue(ParkingStore.isDriving(context))
    }

    @Test
    fun otherDevicesAreIgnored() {
        event(BluetoothDevice.ACTION_ACL_CONNECTED, "66:77:88:99:AA:BB") { fail("should be ignored") }
        assertFalse(ParkingStore.isDriving(context))

        ParkingStore.setDriving(context, true)
        event(BluetoothDevice.ACTION_ACL_DISCONNECTED, "66:77:88:99:AA:BB") { fail("should be ignored") }
        assertTrue(ParkingStore.isDriving(context))
        assertNull(ParkingStore.parkedLocation(context))
    }

    @Test
    fun nothingHappensBeforeACarIsChosen() {
        TestSupport.resetStore()
        event(BluetoothDevice.ACTION_ACL_DISCONNECTED, CAR_ADDRESS) { fail("no car chosen") }
        assertNull(ParkingStore.parkedLocation(context))
    }

    @Test
    fun disconnectingFromCarSavesCurrentLocation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            event(BluetoothDevice.ACTION_ACL_CONNECTED, CAR_ADDRESS)
            assertTrue(ParkingStore.isDriving(context))

            val before = System.currentTimeMillis()
            var usedFallback = false
            event(BluetoothDevice.ACTION_ACL_DISCONNECTED, CAR_ADDRESS) {
                usedFallback = true
                runBlocking { ParkingRecorder.recordParking(context, timeoutMillis = 30_000) }
            }
            assertFalse(ParkingStore.isDriving(context))
            assertFalse("foreground service should have started", usedFallback)

            val deadline = System.currentTimeMillis() + 60_000
            var spot = ParkingStore.parkedLocation(context)
            while (spot == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(500)
                spot = ParkingStore.parkedLocation(context)
            }
            assertNotNull("parking spot was not saved within 60 s", spot)
            spot!!
            assertEquals(TEST_LAT, spot.latitude, 0.001)
            assertEquals(TEST_LON, spot.longitude, 0.001)
            assertTrue(spot.timeMillis >= before)

            Thread.sleep(4_000) // let map tiles load
            TestSupport.screenshot("4-after-disconnect")
        }
    }

    @Test
    fun manualRecordingSavesCurrentLocation() {
        val spot = runBlocking { ParkingRecorder.recordParking(context, timeoutMillis = 30_000) }
        assertNotNull(spot)
        assertEquals(spot, ParkingStore.parkedLocation(context))
        assertEquals(TEST_LAT, spot!!.latitude, 0.001)
    }
}
