#!/usr/bin/env python3
"""Generate iosApp/iosApp/InfoPlist.xcstrings from the shared Compose resources.

iOS shows the usage descriptions from Info.plist verbatim in the permission alert. Without
an InfoPlist.strings for the user's language it shows the English, so the first thing a
Soundscape user hears - before any of the app's own UI, and for a screen reader user
before anything they can navigate away from - is a language they may not read. A String
Catalog keyed on the Info.plist keys fixes that, and the wording is already translated in
strings.xml for every language the app ships.

English comes from iosApp/project.yml rather than strings.xml, because Info.plist needs a
value whether or not the catalog has one and xcodegen writes it from there. strings.xml
carries the same English so that Weblate has something to translate; the two drifting
apart would mean shipping translations of wording the app no longer uses, so this script
refuses to run when they disagree.

Run it after translations change, then rebuild:

    python3 scripts/generate-ios-infoplist-strings.py
    cd iosApp && xcodegen generate

Not a build step. The catalog is committed, so a normal build needs neither Python nor
this script, and a stale catalog degrades to English rather than breaking.
"""

import os
import sys

import yaml

from ios_strings import REPO, read_source, read_translations, unit, write_catalog

PROJECT = os.path.join(REPO, "iosApp/project.yml")
OUT = os.path.join(REPO, "iosApp/iosApp/InfoPlist.xcstrings")

# Info.plist key -> strings.xml key. Only the usage descriptions are here: the other
# localizable Info.plist keys are proper nouns (CFBundleDisplayName) or file type names
# that the Files app shows next to the extension, and neither reads better translated.
#
# Both Bluetooth keys share one translation. iOS asks under whichever the deployment
# target and the frameworks in use call for, the user sees only one of them, and two
# wordings for one question is two things to keep in step for no one's benefit.
MAPPING = {
    "NSLocationWhenInUseUsageDescription": "ios_permission_location_when_in_use",
    "NSLocationAlwaysAndWhenInUseUsageDescription": "ios_permission_location_always",
    "NSMotionUsageDescription": "ios_permission_motion",
    "NSBluetoothAlwaysUsageDescription": "ios_permission_bluetooth",
    "NSBluetoothPeripheralUsageDescription": "ios_permission_bluetooth",
}


def main():
    with open(PROJECT, encoding="utf-8") as f:
        properties = yaml.safe_load(f)["targets"]["iosApp"]["info"]["properties"]
    source = read_source()

    english = {}
    problems = []
    for plist_key, key in MAPPING.items():
        if plist_key not in properties:
            problems.append("%s is not in project.yml" % plist_key)
        elif key not in source:
            problems.append("%s is not in values/strings.xml" % key)
        elif properties[plist_key].strip() != source[key].strip():
            problems.append(
                "%s has drifted from %s:\n    project.yml:  %s\n    strings.xml:  %s"
                % (plist_key, key, properties[plist_key].strip(), source[key].strip())
            )
        else:
            english[plist_key] = source[key]
    if problems:
        raise SystemExit("\n".join(problems))

    # Unlike Localizable.xcstrings, the key here is the Info.plist key rather than the
    # English itself, so English has to be spelled out like any other language.
    strings = {
        plist_key: {"localizations": {"en": unit(text)}}
        for plist_key, text in english.items()
    }

    locales = 0
    for locale, translated in read_translations():
        used = False
        for plist_key, key in MAPPING.items():
            value = translated.get(key)
            # Untranslated keys are simply absent from a locale's file; leaving them out
            # here lets iOS fall back to the source language per string.
            if value and value.strip():
                strings[plist_key]["localizations"][locale] = unit(value)
                used = True
        locales += 1 if used else 0

    write_catalog(OUT, strings)
    print("Wrote %s: %d keys across %d locales" % (OUT, len(strings), locales))
    if not locales:
        print("  note: no locale has these strings yet - they reach Weblate with the\n"
              "        strings.xml change, and this needs rerunning once translated.",
              file=sys.stderr)


if __name__ == "__main__":
    main()
