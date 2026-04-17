package org.scottishtecharmy.soundscape.geoengine.utils.geocoders

import java.text.Normalizer

actual fun normalizeUnicode(input: String): String =
    Normalizer.normalize(input, Normalizer.Form.NFKD)
