package org.scottishtecharmy.soundscape

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.scottishtecharmy.soundscape.screens.home.Navigator

class IntentTest {

    private fun testRedirectUrl(url: String, expectedResult: String) {
        val si = SoundscapeIntents(Navigator())
        val result = si.getRedirectUrlSync(url, InstrumentationRegistry.getInstrumentation().targetContext)
        assert(expectedResult == result)
    }

    @Test
    fun testRedirectUrls() {
        testRedirectUrl("maps.app.goo.gl", "")
        testRedirectUrl("nonexistant://maps.app.goo.gl", "")
        testRedirectUrl("https://maps .app.goo.gl", "")
        testRedirectUrl("https://maps.app.goo.gl/aaaaaa", "")
        testRedirectUrl("https://maps.app.goo.gl/swZebxu9itRfpVSM7", "Allander Leisure Centre")
        testRedirectUrl("https://maps.app.goo.gl/fqkoybFWrmPBS4yr8", "Cemil Baba Restaurant")
        testRedirectUrl("https://www.google.com/maps/place/Fondouk+El+Attarine/@36.7964615,10.1715603,1727m/data=!3m1!1e3!4m6!3m5!1s0x12fd34753f88629b:0x93e228b181a5f676!8m2!3d36.7978821!4d10.1717918!16s%2Fg%2F11bzx2g3m4!5m1!1e1?entry=ttu&g_ep=EgoyMDI1MDkxMC4wIKXMDSoASAFQAw%3D%3D", "Fondouk El Attarine")
        testRedirectUrl("https://maps.app.goo.gl/L4odMyJsGZGDhWpe6", "Prestige Student Living Bridle Works")
    }
}