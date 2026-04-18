package org.scottishtecharmy.soundscape.platform

import java.util.Locale

actual fun getDefaultLanguage(): String = Locale.getDefault().language
