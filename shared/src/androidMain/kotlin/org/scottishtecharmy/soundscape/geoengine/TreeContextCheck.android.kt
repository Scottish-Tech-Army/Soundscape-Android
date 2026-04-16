package org.scottishtecharmy.soundscape.geoengine

internal actual fun assertRunningInTreeContext() {
    assert(Thread.currentThread().name.startsWith("TreeContext"))
}
