package org.scottishtecharmy.soundscape.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityEnhanced
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier
import platform.Foundation.localizedStringForLocaleIdentifier
import org.scottishtecharmy.soundscape.screens.home.settings.TtsVoiceOption

data class IosTtsVoiceInfo(
    val identifier: String,
    /** The voice's own name, e.g. "Satu". Only unique within a language - see [shapeVoiceList]. */
    val name: String,
    /** The voice's own BCP-47 tag, e.g. "fi-FI". */
    val language: String,
    /** [language] named in the app's own language, e.g. "Finnish (Finland)". */
    val languageDisplayName: String,
    /** Whether this voice speaks the language the app itself is in. */
    val inAppLanguage: Boolean,
    val family: IosVoiceFamily,
)

/**
 * Where a voice came from. iOS reports third-party voices through the iOS 17 speech provider API
 * alongside Apple's own, with nothing but the identifier to tell them apart, and the picker shows
 * one sub-list per family because a user who installs eSpeak gets hundreds of voices at once.
 *
 * Declared in the order the picker shows them, so the ones somebody would actually navigate by
 * come first; [NOVELTY] is never shown at all.
 */
enum class IosVoiceFamily {
    /** Apple's own ordinary voices - Daniel, Satu, Serena. Listed directly under the language. */
    STANDARD,

    /** The Eloquence voices - Flo, Eddy, Grandma, Grandpa, Reed, Rocko, Sandy, Shelley. */
    ELOQUENCE,

    /** eSpeak, installed by the user as a speech provider app. */
    ESPEAK,

    /** Any other provider the user has installed, which we can't name - see [voiceFamilyOf]. */
    OTHER,

    /** Apple's joke voices - Bubbles, Zarvox, Bad News, Organ. Dropped by [shapeVoiceList]. */
    NOVELTY,
}

/**
 * Which family a voice belongs to, worked out from its identifier - the only thing iOS gives us
 * that distinguishes them. This is the Soundscape community app's classification
 * (VoiceCatalogueProviderGroup.classify) with its novelty handling folded in:
 *
 * - `com.apple.speech.synthesis.voice.*` are the novelty voices. The community app reads iOS 17's
 *   `voiceTraits.contains(.isNoveltyVoice)` and falls back to a list of exactly these identifiers;
 *   this app's iOS 16 deployment target can't call voiceTraits without a runtime check, and the
 *   prefix is what that fallback list amounts to.
 * - `com.apple.eloquence.*` are Eloquence.
 * - anything with "espeak" in it is eSpeak, wherever the provider puts it in the identifier.
 * - anything else outside `com.apple.` came from a provider app we have never heard of. Users
 *   install whatever they like, so there is nothing to match on and no name to show: they share
 *   one "other voices" sub-list rather than being guessed at.
 * - everything left is one of Apple's own ordinary voices.
 *
 * Matching is case-insensitive, as it is in the community app - a provider chooses its own
 * identifiers and nothing promises their case.
 */
internal fun voiceFamilyOf(identifier: String): IosVoiceFamily {
    val normalized = identifier.lowercase()
    return when {
        normalized.startsWith("com.apple.speech.synthesis.voice.") -> IosVoiceFamily.NOVELTY
        normalized.startsWith("com.apple.eloquence.") -> IosVoiceFamily.ELOQUENCE
        normalized.contains("espeak") -> IosVoiceFamily.ESPEAK
        !normalized.startsWith("com.apple.") -> IosVoiceFamily.OTHER
        else -> IosVoiceFamily.STANDARD
    }
}

/**
 * Voices the legacy Soundscape iOS app suppressed because they sound robotic
 * compared to the modern voices and can't be removed by the user.
 */
private val DISALLOWED_VOICES = setOf(
    "com.apple.speech.synthesis.voice.Fred",
    "com.apple.speech.synthesis.voice.Victoria",
    "com.apple.speech.voice.Alex",
)

/**
 * A voice as iOS describes it, with the locale lookups already done so that
 * [shapeVoiceList] can be tested without AVFoundation.
 */
internal data class RawTtsVoice(
    val identifier: String,
    val name: String,
    /** The voice's own BCP-47 tag, e.g. "fi-FI". */
    val language: String,
    /** [language]'s language code, e.g. "fi". */
    val languageCode: String,
    /** [language] named in the app's own language, e.g. "Finnish (Finland)". */
    val languageDisplayName: String,
    val enhanced: Boolean,
)

/**
 * Every installed voice, filtered and ordered for the voice picker - see [shapeVoiceList].
 *
 * Voices in a language other than the app's are worth offering because the app's language and the
 * language of the street names around the user are two different things: a Finnish user running
 * the app in English needs a Finnish voice to make sense of Finnish street names.
 */
@OptIn(ExperimentalForeignApi::class)
fun availableTtsVoices(): List<IosTtsVoiceInfo> {
    val appLocale = NSLocale.currentLocale
    val rawVoices = AVSpeechSynthesisVoice.speechVoices()
        .filterIsInstance<AVSpeechSynthesisVoice>()
        .map { voice ->
            RawTtsVoice(
                identifier = voice.identifier,
                name = voice.name,
                language = voice.language,
                languageCode = NSLocale(voice.language).languageCode,
                // Named in the app's language, so an English-speaking user looking for a Finnish
                // voice reads "Finnish (Finland)" rather than "suomi" or "fi-FI". iOS hands back
                // the identifier itself for a language it can't name.
                languageDisplayName = appLocale.localizedStringForLocaleIdentifier(voice.language),
                enhanced = voice.quality == AVSpeechSynthesisVoiceQualityEnhanced,
            )
        }

    return shapeVoiceList(
        rawVoices,
        currentLanguageCode = appLocale.languageCode,
        // NSLocale spells its own identifier with an underscore ("en_GB") while a voice spells
        // its language with a hyphen ("en-GB"), so bring the two together before comparing.
        currentLocaleTag = appLocale.localeIdentifier.replace("_", "-"),
    )
}

/**
 * Filters and orders the voices iOS reports:
 *
 * - the novelty voices go. Bubbles and Zarvox are jokes, and this is an app whose whole job is
 *   telling a blind user where they are; nobody needs to meet one of those while looking for a
 *   voice to navigate by. This covers the legacy app's Fred and Victoria too, leaving
 *   [DISALLOWED_VOICES] to catch Alex, which is an ordinary voice it suppressed on its own.
 * - disallowed voices go, and where both an enhanced and a default-quality voice exist for the
 *   same speaker only the enhanced one is kept. The transformation that collides the
 *   "premium"/"compact" identifier tokens matches the legacy `TTSConfigHelper.loadVoices(...)`.
 * - one voice per name per language per provider, because iOS can report the same voice twice
 *   under two identifier schemes. The provider has to be part of that, because two of them
 *   genuinely do ship a voice of the same name: eSpeak and Eloquence both have a Grandma, a
 *   Grandpa and a Reed in English, and they are different voices.
 * - the app's own locale first - an app running in English (United Kingdom) leads with exactly
 *   those voices, not with English (Australia) because A sorts before U - then the rest of its
 *   language, then every other language in the order the user reads them rather than by raw
 *   "fi-FI" tag. Each language's voices stay together, and within a language the ordinary voices
 *   come before the Eloquence and provider ones.
 *
 * Grouping by language is what makes [IosTtsVoiceInfo.name] usable on its own. iOS ships the
 * Eloquence voices - Flo, Eddy, Grandma, Grandpa, Reed, Rocko, Sandy, Shelley - in every language
 * they cover and reports the bare name for each, so a flat list repeats the same handful of names
 * with nothing to tell them apart. Within one language each name appears once.
 */
internal fun shapeVoiceList(
    rawVoices: List<RawTtsVoice>,
    currentLanguageCode: String,
    currentLocaleTag: String,
): List<IosTtsVoiceInfo> {
    val allowed = rawVoices.filter {
        it.identifier !in DISALLOWED_VOICES &&
            voiceFamilyOf(it.identifier) != IosVoiceFamily.NOVELTY
    }

    val enhancedStubs = allowed
        .filter { it.enhanced }
        .map { it.identifier.replace("premium", "") }
        .toSet()

    val condensed = allowed.filter { voice ->
        voice.enhanced || voice.identifier.replace("compact", "") !in enhancedStubs
    }

    return condensed
        // Where one provider gives the same name twice for a language, the better-sounding one
        // wins.
        .sortedByDescending { it.enhanced }
        .distinctBy { Triple(it.language, voiceFamilyOf(it.identifier), it.name) }
        .sortedWith(
            compareByDescending<RawTtsVoice> {
                it.language.equals(currentLocaleTag, ignoreCase = true)
            }
                .thenByDescending { it.languageCode == currentLanguageCode }
                .thenBy { it.languageDisplayName }
                .thenBy { voiceFamilyOf(it.identifier) }
                .thenBy { it.name },
        )
        .map { voice ->
            IosTtsVoiceInfo(
                identifier = voice.identifier,
                name = voice.name,
                language = voice.language,
                languageDisplayName = voice.languageDisplayName,
                inAppLanguage = voice.languageCode == currentLanguageCode,
                family = voiceFamilyOf(voice.identifier),
            )
        }
}

/**
 * This voice as the shared picker shows it, folded into a sub-list for its provider unless it's
 * one of Apple's own. [otherProviderLabel] is the translated heading for providers we can't name.
 */
internal fun IosTtsVoiceInfo.toVoiceOption(otherProviderLabel: String) = TtsVoiceOption(
    identifier = identifier,
    name = name,
    languageDisplayName = languageDisplayName,
    inAppLanguage = inAppLanguage,
    providerLabel = providerLabel(family, otherProviderLabel),
)

/**
 * What a provider's sub-list is headed with, or null for Apple's own voices, which have no
 * sub-list of their own.
 *
 * "Eloquence" and "eSpeak" are left untranslated on purpose: they are the names of the engines,
 * and they read the same in every language - as they do in iOS's own Voices screen and in the
 * Soundscape community app. Only the catch-all is a translated string, because that one is a
 * description rather than a name.
 */
private fun providerLabel(family: IosVoiceFamily, otherProviderLabel: String): String? =
    when (family) {
        IosVoiceFamily.STANDARD -> null
        IosVoiceFamily.ELOQUENCE -> "Eloquence"
        IosVoiceFamily.ESPEAK -> "eSpeak"
        IosVoiceFamily.OTHER -> otherProviderLabel
        // Never reaches the picker - see shapeVoiceList.
        IosVoiceFamily.NOVELTY -> null
    }
