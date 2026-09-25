#!/usr/bin/env python3
"""Generate iosApp/iosApp/Localizable.xcstrings from the shared Compose resources.

The Siri surface — intent titles, the choices Siri offers, entity type names, shortcut
descriptions — is written in Swift as plain English literals. Because String Catalogs key
on the source literal, those same literals can be given translations without touching a
line of Swift, and most of them already exist translated in strings.xml for the app's own
UI. This script copies them across.

Two things it deliberately does not do:

  * Spoken phrases. Those live in <locale>.lproj/AppShortcuts.strings and cannot be
    produced by translating English, because they have to be what a speaker would
    actually say.
  * Anything not listed in MAPPING. Silence beats guessing at a key whose wording happens
    to match today.

Run it after translations change, then rebuild:

    python3 scripts/generate-ios-siri-strings.py
    cd iosApp && xcodegen generate

Not a build step. The catalog is committed, so a normal build needs neither Python nor
this script, and a stale catalog degrades to English rather than breaking.
"""

import os

from ios_strings import REPO, read_source, read_translations, unit, write_catalog

OUT = os.path.join(REPO, "iosApp/iosApp/Localizable.xcstrings")

# English literal as written in Swift -> strings.xml key.
#
# Where the app already says the same thing, its existing key is reused rather than a new
# siri_* one being added: "Around Me" is the same words on the button and in Siri, and one
# translation serving both is one fewer to keep in step.
MAPPING = {
    # Callout choices
    "My Location": "directions_my_location",
    "Around Me": "help_orient_page_title",
    "Ahead of Me": "help_explore_page_title",
    "Nearby Markers": "callouts_nearby_markers",
    "Call out Beacon": "beacon_action_callout_beacon",
    "Beacon Info": "menu_beacon_info",
    # Route control choices
    "Next Waypoint": "menu_route_next_waypoint",
    "Previous Waypoint": "menu_route_previous_waypoint",
    "Mute Beacon": "beacon_action_mute_beacon",
    "Stop": "siri_choice_stop",
    # List choices
    "Routes": "routes_title",
    "Markers": "markers_title",
    "Commands": "siri_choice_commands",
    # Amount of detail choices
    "Silent": "callouts_verbosity_level_silent",
    "Quiet": "callouts_verbosity_level_quiet",
    "Balanced": "callouts_verbosity_level_balanced",
    "Detailed": "callouts_verbosity_level_detailed",
    # Shortcut and intent names
    "Hear My Surroundings": "callouts_panel_title",
    "Control Route": "siri_title_control_route",
    "Start Route": "route_detail_action_start_route",
    "Start Beacon": "siri_title_start_beacon",
    "Stop Beacon": "siri_title_stop_beacon",
    "List": "siri_type_list",
    "Surroundings": "siri_short_surroundings",
    "Beacon": "siri_short_beacon",
    "Callout Detail": "callouts_verbosity",
    "Detail": "siri_short_detail",
    # Parameter and type names
    "Callout": "siri_type_callout",
    "Route Command": "siri_type_route_command",
    "Command": "siri_param_command",
    "Route": "siri_param_route",
    "Marker": "markers_generic_name",
    "Level": "siri_param_level",
    # Descriptions
    "Describes where you are and what is around you.": "siri_desc_surroundings",
    "Skips waypoints, mutes the beacon, or stops the route.": "siri_desc_route_control",
    "Starts one of your saved routes.": "siri_desc_start_route",
    "Sets an audio beacon on one of your saved markers.": "siri_desc_start_beacon",
    "Switches the audio beacon off.": "siri_desc_stop_beacon",
    "Reads back your saved routes, your saved markers, or the commands you can say.":
        "siri_desc_list",
    "Sets how much Soundscape says as you walk.": "siri_desc_callout_detail",
    # Spoken failures and confirmations
    "Done": "general_alert_done",
    "Open Soundscape to finish that.": "siri_error_needs_app",
    "Soundscape couldn't do that.": "siri_error_generic",
}

# Strings whose content recites the spoken phrases, so they cannot be translated from
# English like the rest. A locale appears here only once its phrases have been authored
# in <locale>.lproj/AppShortcuts.strings, and the wording must name that locale's group
# words; otherwise it would tell the user to say commands that do not exist. Locales with
# no entry fall back to the English source string, which is correct for them: their Siri
# has no phrases of its own and still answers to the English ones.
PHRASE_COUPLED = {
    "You can say: Soundscape surroundings, Soundscape route, Soundscape start route, "
    "Soundscape beacon, Soundscape stop beacon, Soundscape detail, or Soundscape list.": {
        "ar": "يمكنك أن تقول: Soundscape المحيط، Soundscape المسار، "
              "Soundscape ابدأ المسار، Soundscape المنارة، Soundscape أوقف المنارة، Soundscape التفاصيل، "
              "أو Soundscape قائمة.",
        "da": "Du kan sige: Soundscape omgivelser, Soundscape rute, "
              "Soundscape start rute, Soundscape lydfyr, Soundscape stop lydfyr, Soundscape detaljer "
              "eller Soundscape liste.",
        "de": "Sie können sagen: Soundscape Umgebung, Soundscape Route, "
              "Soundscape starte Route, Soundscape Beacon, Soundscape stoppe Beacon, Soundscape Details "
              "oder Soundscape Liste.",
        "es": "Puedes decir: Soundscape entorno, Soundscape ruta, "
              "Soundscape iniciar ruta, Soundscape señal, Soundscape detener señal, Soundscape detalle "
              "o Soundscape lista.",
        "fi": "Voit sanoa: Soundscape ympäristö, Soundscape reitti, "
              "Soundscape aloita reitti, Soundscape majakka, "
              "Soundscape pysäytä majakka, Soundscape yksityiskohdat tai Soundscape luettelo.",
        "fr": "Vous pouvez dire : Soundscape environs, Soundscape itinéraire, "
              "Soundscape démarre l'itinéraire, Soundscape balise, "
              "Soundscape arrête la balise, Soundscape détails ou Soundscape liste.",
        "it": "Puoi dire: Soundscape dintorni, Soundscape percorso, "
              "Soundscape avvia percorso, Soundscape audiofaro, "
              "Soundscape ferma audiofaro, Soundscape dettagli o Soundscape elenco.",
        "ja": "「Soundscape 周辺」「Soundscape ルート」「Soundscape ルート開始」"
              "「Soundscape ビーコン」「Soundscape ビーコン停止」「Soundscape 詳細」「Soundscape リスト」"
              "と言えます。",
        "ko": "다음과 같이 말할 수 있습니다: Soundscape 주변, Soundscape 경로, "
              "Soundscape 경로 시작, Soundscape 비콘, Soundscape 비콘 중지, Soundscape 상세, "
              "Soundscape 목록.",
        "nb": "Du kan si: Soundscape omgivelser, Soundscape rute, "
              "Soundscape start rute, Soundscape lydsignal, "
              "Soundscape stopp lydsignal, Soundscape detaljer eller Soundscape liste.",
        "nl": "U kunt zeggen: Soundscape omgeving, Soundscape route, "
              "Soundscape start route, Soundscape baken, Soundscape stop baken, Soundscape detail "
              "of Soundscape lijst.",
        "pt": "Pode dizer: Soundscape arredores, Soundscape rota, "
              "Soundscape iniciar rota, Soundscape sinal, Soundscape parar sinal, Soundscape detalhe "
              "ou Soundscape lista.",
        "pt-BR": "Você pode dizer: Soundscape arredores, Soundscape rota, "
                 "Soundscape iniciar rota, Soundscape sinalizador, "
                 "Soundscape parar sinalizador, Soundscape detalhe ou Soundscape lista.",
        "ru": "Можно сказать: Soundscape окружение, Soundscape маршрут, "
              "Soundscape запусти маршрут, Soundscape маяк, "
              "Soundscape выключи маяк, Soundscape детали или Soundscape список.",
        "sv": "Du kan säga: Soundscape omgivning, Soundscape rutt, "
              "Soundscape starta rutt, Soundscape ljudfyr, "
              "Soundscape stoppa ljudfyr, Soundscape detaljer eller Soundscape lista.",
        "th": "คุณสามารถพูดว่า: Soundscape รอบตัว, Soundscape เส้นทาง, "
              "Soundscape เริ่มเส้นทาง, Soundscape บีคอน, Soundscape หยุดบีคอน, Soundscape รายละเอียด "
              "หรือ Soundscape รายการ",
        "tr": "Şunları diyebilirsiniz: Soundscape çevre, Soundscape rota, "
              "Soundscape rota başlat, Soundscape işaret, Soundscape işareti durdur, Soundscape ayrıntı "
              "veya Soundscape liste.",
        "zh-Hans": "你可以说：Soundscape 周围、Soundscape 路线、Soundscape 开始路线、"
                   "Soundscape 信标、Soundscape 停止信标、Soundscape 详细，或 Soundscape 列表。",
    },
}


def main():
    source = read_source()

    missing = [k for k in MAPPING.values() if k not in source]
    if missing:
        raise SystemExit("Keys missing from values/strings.xml: " + ", ".join(missing))

    # Warn where the English in Swift has drifted from the English in strings.xml. Not
    # fatal: the literal stays the key and English still works, but the translations being
    # pulled in are then for subtly different wording.
    for literal, key in MAPPING.items():
        if source[key].strip() != literal:
            print("  note: '%s' <- %s = '%s'" % (literal, key, source[key].strip()))

    strings = {literal: {"localizations": {}} for literal in MAPPING}
    for literal, localized in PHRASE_COUPLED.items():
        strings[literal] = {
            "comment": "Recites the spoken phrases. Translate only alongside that "
                       "language's phrases in <locale>.lproj/AppShortcuts.strings.",
            "localizations": {
                locale: unit(text) for locale, text in sorted(localized.items())
            },
        }

    locales = 0
    for locale, translated in read_translations():
        used = False
        for literal, key in MAPPING.items():
            value = translated.get(key)
            # Untranslated keys are simply absent from a locale's file; leaving them out
            # here lets iOS fall back to the source language per string.
            if value and value.strip():
                strings[literal]["localizations"][locale] = unit(value)
                used = True
        locales += 1 if used else 0

    write_catalog(OUT, strings)
    print("Wrote %s: %d strings across %d locales" % (OUT, len(strings), locales))


if __name__ == "__main__":
    main()
