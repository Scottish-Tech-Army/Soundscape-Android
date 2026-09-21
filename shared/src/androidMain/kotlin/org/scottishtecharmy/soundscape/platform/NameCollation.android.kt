package org.scottishtecharmy.soundscape.platform

import java.text.Collator
import java.util.Locale

actual fun nameCollator(languageTag: String): Comparator<String> {
    // SECONDARY tells letters and their accents apart, but not upper and lower case. A Collator
    // isn't safe to share between threads, so each comparator gets its own.
    val collator = Collator.getInstance(Locale.forLanguageTag(languageTag))
    collator.strength = Collator.SECONDARY
    return Comparator { a, b -> collator.compare(a, b) }
}
