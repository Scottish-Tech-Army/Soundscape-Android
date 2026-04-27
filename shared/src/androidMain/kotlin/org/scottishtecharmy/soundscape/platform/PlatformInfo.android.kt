package org.scottishtecharmy.soundscape.platform

actual fun appVersionName(): String = try {
    val buildConfigClass = Class.forName("org.scottishtecharmy.soundscape.BuildConfig")
    buildConfigClass.getField("VERSION_NAME").get(null) as? String ?: "0.0.0"
} catch (_: Throwable) {
    "0.0.0"
}

actual val analyticsEnabled: Boolean = try {
    val buildConfigClass = Class.forName("org.scottishtecharmy.soundscape.BuildConfig")
    !buildConfigClass.getField("DUMMY_ANALYTICS").getBoolean(null)
} catch (_: Throwable) {
    true
}
