package org.scottishtecharmy.soundscape.platform

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getDefaultLanguage(): String = NSLocale.currentLocale.languageCode
