package org.scottishtecharmy.soundscape.locationprovider.bose

import org.scottishtecharmy.soundscape.locationprovider.Accuracy
import org.scottishtecharmy.soundscape.locationprovider.DirectionProvider
import org.scottishtecharmy.soundscape.locationprovider.HeadTrackingStatus
import org.scottishtecharmy.soundscape.locationprovider.LocationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * BoseFramesHeadTrackingProvider's frame-decode / calibration data path
 * (onBytes/processSample, and the BoseFramesDataParser + HeadphoneCalibrationManager
 * pipeline it drives) is only reachable by calling start(), which immediately
 * hands off to `client.runSession(...)` - [BoseFramesClient] is a final class
 * that talks directly to the Kable BLE stack (Scanner/Peripheral) with no
 * interface or open member to substitute a fake, and there's no mocking
 * library available in shared/commonTest. All of the frame-processing methods
 * are private, so there is also no seam to feed synthetic bytes in directly
 * without modifying production code.
 *
 * Given that, this suite only covers the small slice of behaviour reachable
 * without a live BLE connection: default flow values and that stop()/destroy()
 * are safe to call before start(). The parser layer this class sits on is
 * already covered by BoseFramesDataParserTest, and the calibration layer it
 * delegates to is covered by HeadphoneCalibrationManagerTest.
 */
class BoseFramesHeadTrackingProviderTest {

    private class FakeDirectionProvider : DirectionProvider()

    private class FakeLocationProvider : LocationProvider() {
        override fun start(accuracy: Accuracy) {}
        override fun destroy() {}
    }

    private fun provider() = BoseFramesHeadTrackingProvider(FakeDirectionProvider(), FakeLocationProvider())

    @Test
    fun initialStateIsInactiveWithNoHeading() {
        val p = provider()
        assertEquals(HeadTrackingStatus.Inactive, p.statusFlow.value)
        assertNull(p.headHeadingFlow.value)
    }

    @Test
    fun stopBeforeStartIsSafeAndLeavesStateInactive() {
        val p = provider()
        p.stop()
        assertEquals(HeadTrackingStatus.Inactive, p.statusFlow.value)
        assertNull(p.headHeadingFlow.value)
    }

    @Test
    fun destroyBeforeStartIsSafe() {
        val p = provider()
        p.destroy()
        assertEquals(HeadTrackingStatus.Inactive, p.statusFlow.value)
    }
}
