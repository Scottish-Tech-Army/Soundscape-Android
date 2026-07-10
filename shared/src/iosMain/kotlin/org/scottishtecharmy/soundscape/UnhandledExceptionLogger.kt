package org.scottishtecharmy.soundscape

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSLog

private val install: Boolean by lazy {
    @OptIn(ExperimentalNativeApi::class)
    setUnhandledExceptionHook { throwable ->
        NSLog("Soundscape uncaught: %@\n%@", throwable.toString(), throwable.stackTraceToString())
    }
    true
}

internal fun installUnhandledExceptionLogger() {
    install
}
