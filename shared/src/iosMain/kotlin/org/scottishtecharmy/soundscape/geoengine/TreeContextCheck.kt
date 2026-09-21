package org.scottishtecharmy.soundscape.geoengine

internal actual fun assertRunningInTreeContext() {
    // No-op on iOS — the single-thread assertion is Android-specific
}
