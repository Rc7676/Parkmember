package com.parkmember

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context get() = TestSupport.context

    @Before
    fun setUp() {
        TestSupport.grantAllPermissions()
        TestSupport.resetStore()
    }

    private fun launch(screenshotName: String, settleMillis: Long = 0, checks: () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            checks()
            if (settleMillis > 0) Thread.sleep(settleMillis)
            TestSupport.screenshot(screenshotName)
        }
    }

    @Test
    fun withoutCarShowsDevicePicker() = launch("1-choose-car") {
        compose.onNodeWithText("Step 3 of 3 · Choose your car").assertIsDisplayed()
        compose.onNodeWithText("Pair a new device").assertIsDisplayed()
    }

    @Test
    fun withCarButNoSpotExplainsWhatHappensNext() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("2-no-spot-yet") {
            compose.onNodeWithText("Not parked yet").assertIsDisplayed()
            compose.onNodeWithText("Car: Test Car").assertIsDisplayed()
            compose.onNodeWithText("Save spot now").assertIsDisplayed()
        }
    }

    @Test
    fun drivingIsShown() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        ParkingStore.setDriving(context, true)
        launch("3-driving") {
            compose.onNodeWithText("Driving").assertIsDisplayed()
        }
    }

    @Test
    fun savedSpotIsShownOnMap() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        ParkingStore.setParkedLocation(
            context,
            ParkedLocation(TEST_LAT, TEST_LON, 8f, System.currentTimeMillis() - 5 * 60_000),
        )
        launch("5-parked-map", settleMillis = 4_000) {
            compose.onNodeWithText("Parked").assertIsDisplayed()
            compose.onNodeWithText("Walk to car").assertIsDisplayed()
            compose.onNodeWithText("Accuracy about 8 m").assertIsDisplayed()
        }
    }

    @Test
    fun changeCarOpensPickerWithCancel() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("6-change-car") {
            compose.onNodeWithText("Car: Test Car").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Step 3 of 3 · Choose your car").assertIsDisplayed()
            compose.onNodeWithText("Cancel").assertIsDisplayed()
        }
    }
}

