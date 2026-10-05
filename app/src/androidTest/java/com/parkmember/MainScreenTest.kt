package com.parkmember

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
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
        ParkingStore.setKeepAliveSetupDone(context, true)
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
        compose.onNodeWithText("Choose your car").assertIsDisplayed()
        compose.onNodeWithText("Step 3 of 4").assertIsDisplayed()
        compose.onNodeWithText("Pair a new device").assertIsDisplayed()
    }

    @Test
    fun withCarButNoSpotExplainsWhatHappensNext() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("2-no-spot-yet") {
            compose.onNodeWithText("Not parked yet").assertIsDisplayed()
            compose.onNodeWithText("Test Car").assertIsDisplayed()
            compose.onNodeWithText("Save spot now").assertIsDisplayed()
            compose.onNodeWithText("No spot saved yet").assertIsDisplayed()
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
            compose.onNodeWithText("5 minutes ago").assertIsDisplayed()
        }
    }

    @Test
    fun darkModeParkedScreen() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        ParkingStore.setParkedLocation(context, ParkedLocation(TEST_LAT, TEST_LON, 8f, System.currentTimeMillis()))
        TestSupport.shell("cmd uimode night yes")
        try {
            launch("7-parked-dark", settleMillis = 4_000) {
                compose.onNodeWithText("Just now").assertIsDisplayed()
            }
        } finally {
            TestSupport.shell("cmd uimode night no")
        }
    }

    @Test
    fun keepAliveStepShownOnceThenMainScreen() {
        ParkingStore.setKeepAliveSetupDone(context, false)
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("8-keep-running") {
            compose.onNodeWithText("Keep it running").assertIsDisplayed()
            compose.onNodeWithText("Step 4 of 4").assertIsDisplayed()
            compose.onNodeWithText("Battery optimization").assertIsDisplayed()
            compose.onNodeWithText("Finish setup").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Not parked yet").assertIsDisplayed()
        }
        assertTrue(ParkingStore.isKeepAliveSetupDone(context))
    }

    @Test
    fun settingsButtonReopensKeepAliveStep() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("9-keep-running-revisit") {
            compose.onNode(hasContentDescription("Background settings", substring = true)).performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Keep it running").assertIsDisplayed()
            compose.onNodeWithText("Done").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Not parked yet").assertIsDisplayed()
        }
    }

    @Test
    fun watcherServiceStartsOnceSetUp() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        ActivityScenario.launch(MainActivity::class.java).use {
            val deadline = System.currentTimeMillis() + 10_000
            while (!CarWatcherService.isRunning && System.currentTimeMillis() < deadline) Thread.sleep(200)
            assertTrue("CarWatcherService should be running", CarWatcherService.isRunning)
        }
        assertTrue("watcher keeps running after the app closes", CarWatcherService.isRunning)
    }

    @Test
    fun changeCarOpensPickerWithCancel() {
        ParkingStore.setCarDevice(context, TEST_CAR)
        launch("6-change-car") {
            compose.onNodeWithText("Test Car").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Choose your car").assertIsDisplayed()
            compose.onNodeWithText("Cancel").assertIsDisplayed()
        }
    }
}

