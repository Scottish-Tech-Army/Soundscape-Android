package org.scottishtecharmy.soundscape.services

import kotlinx.coroutines.flow.StateFlow
import org.scottishtecharmy.soundscape.services.mediacontrol.MediaControllableService

/**
 * Minimal abstraction shared state-holders use to reach the platform service.
 *
 * Android backs this with `SoundscapeServiceConnection`, where `service` is null until
 * the bound service has connected. iOS backs it with `IosSoundscapeService` itself,
 * where `service` is always non-null.
 */
interface ServiceConnection {
    val serviceBoundState: StateFlow<Boolean>
    val service: MediaControllableService?
}
