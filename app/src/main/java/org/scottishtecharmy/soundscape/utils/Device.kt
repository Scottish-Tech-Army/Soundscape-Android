package org.scottishtecharmy.soundscape.utils

interface Device {
    fun platform(): Platform
    fun osSdkVersionNumber(): String
}

enum class Platform {
    Android
}

class AndroidDevice(
    private val osSdkVersionNumber: String
) : Device {
    override fun platform(): Platform = Platform.Android
    override fun osSdkVersionNumber(): String = osSdkVersionNumber
}