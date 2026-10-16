package org.scottishtecharmy.soundscape.platform

import java.util.Locale

actual fun getDefaultLanguageTag(): String = Locale.getDefault().toLanguageTag()
