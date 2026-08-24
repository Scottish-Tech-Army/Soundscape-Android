package org.scottishtecharmy.soundscape.utils

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

// Firebase's iOS SDK also exports a class called `Analytics`, so rename the
// ObjC-visible symbol to avoid collision in Swift call sites. Kotlin call
// sites still use the plain `Analytics` name.
@OptIn(ExperimentalObjCName::class)
@ObjCName("SoundscapeAnalytics", exact = true)
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?>? = null)
    fun logCostlyEvent(name: String, params: Map<String, Any?>? = null)

    fun crashSetCustomKey(key: String, value: String)
    fun crashLogNotes(name: String)
}
