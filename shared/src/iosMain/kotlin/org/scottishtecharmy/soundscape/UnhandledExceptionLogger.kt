package org.scottishtecharmy.soundscape

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

private val install: Boolean by lazy {
    @OptIn(ExperimentalNativeApi::class)
    setUnhandledExceptionHook { throwable ->
        // Use println (stderr) rather than NSLog. Kotlin/Native's NSLog
        // binding takes vararg Any? and cannot reliably bridge Kotlin
        // strings to NSString* for %@ formatters — the ObjC format machinery
        // then calls objc_opt_respondsToSelector on a bad pointer and
        // crashes the app before the exception ever surfaces. println
        // writes to stderr, which iOS captures in Console.app / device
        // logs just as visibly.
        println("Soundscape uncaught: $throwable")
        println(throwable.stackTraceToString())
    }
    true
}

internal fun installUnhandledExceptionLogger() {
    install
}
