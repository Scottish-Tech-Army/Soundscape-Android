package org.scottishtecharmy.soundscape.geoengine

import org.scottishtecharmy.soundscape.locationprovider.SoundscapeLocation

interface LocationRecorder {
    suspend fun storeLocation(location: SoundscapeLocation)
}
