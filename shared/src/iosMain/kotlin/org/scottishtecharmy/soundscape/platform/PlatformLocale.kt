package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

actual fun getDefaultLanguageTag(): String = NSLocale.currentLocale.localeIdentifier.replace('_', '-')
