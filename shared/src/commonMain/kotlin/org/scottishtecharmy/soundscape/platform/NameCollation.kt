package org.scottishtecharmy.soundscape.platform

import org.scottishtecharmy.soundscape.screens.onboarding.language.getAppLocale

/**
 * Orders names the way a dictionary in [languageTag] does, ignoring case. The letters of each
 * language keep their own place: Swedish puts Å, Ä and Ö after Z, Ukrainian puts Є after Е and
 * Ї after І, and an accented French É sorts among the Es rather than after Z. A plain comparison
 * of the characters, which is what String.CASE_INSENSITIVE_ORDER does, gets all of those wrong -
 * and so would comparing the names normalizeForSearch makes, which folds Å into A and Ø into O
 * because a search should match either.
 */
expect fun nameCollator(languageTag: String): Comparator<String>

/** [nameCollator] for the language the app is set to, or the system's if it isn't set to one. */
fun appNameCollator(): Comparator<String> = nameCollator(getAppLocale()?.language ?: getDefaultLanguage())
