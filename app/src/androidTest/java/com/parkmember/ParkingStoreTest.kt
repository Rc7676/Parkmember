package com.parkmember

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkingStoreTest {
    private val context get() = TestSupport.context

    @Before
    fun setUp() = TestSupport.resetStore()

    @Test
    fun emptyStoreHasNothingSaved() {
        assertNull(ParkingStore.carDevice(context))
        assertNull(ParkingStore.parkedLocation(context))
        assertFalse(ParkingStore.isDriving(context))
    }

    @Test
    fun carDeviceRoundTrips() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        assertEquals(TEST_CAR, ParkingStore.carDevice(context))
    }

    @Test
    fun parkedLocationRoundTripsWithFullPrecision() {
        val spot = ParkedLocation(32.08531234567, -34.78187654321, 4.5f, 1_700_000_000_000)
        ParkingStore.setParkedLocation(context, spot)
        assertEquals(spot, ParkingStore.parkedLocation(context))
    }

    @Test
    fun parkedLocationWithoutAccuracyClearsOldAccuracy() {
        ParkingStore.setParkedLocation(context, ParkedLocation(1.0, 2.0, 10f, 1))
        ParkingStore.setParkedLocation(context, ParkedLocation(3.0, 4.0, null, 2))
        assertEquals(ParkedLocation(3.0, 4.0, null, 2), ParkingStore.parkedLocation(context))
    }

    @Test
    fun drivingFlagRoundTrips() {
        ParkingStore.setDriving(context, true)
        assertTrue(ParkingStore.isDriving(context))
        ParkingStore.setDriving(context, false)
        assertFalse(ParkingStore.isDriving(context))
    }
}
