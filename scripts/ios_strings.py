"""Shared plumbing for the scripts that build iOS String Catalogs.

Both catalogs in iosApp/iosApp are generated rather than edited: the translations they
carry already exist in the shared Compose resources, put there by Weblate, and copying
them across beats maintaining a second set by hand. What the two generators share is the
reading of those resources and the mapping from Android's locale qualifiers to the BCP-47
tags Apple expects - the mapping in particular is the kind of table that goes wrong when
it exists twice.
"""

import json
import os
import re
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "shared/src/commonMain/composeResources")

# Android resource qualifiers to the BCP-47 tags Apple expects. Only the ones that differ
# need listing; everything else passes through unchanged.
LOCALE_OVERRIDES = {
    "in": "id",       # Android's legacy code for Indonesian
    "iw": "he",       # ...and for Hebrew
    "zh-rCN": "zh-Hans",
    "pt-rBR": "pt-BR",
    "fr-rCA": "fr-CA",
    "en-rGB": "en-GB",
}


def ios_locale(qualifier):
    if qualifier in LOCALE_OVERRIDES:
        return LOCALE_OVERRIDES[qualifier]
    # e.g. "es-rMX" -> "es-MX"
    return re.sub(r"-r([A-Z]{2})$", r"-\1", qualifier)


def read_strings(path):
    """Key -> text for one strings.xml, flattening any inline markup."""
    root = ET.parse(path).getroot()
    out = {}
    for node in root.findall("string"):
        name = node.get("name")
        if name:
            out[name] = "".join(node.itertext())
    return out


def read_source():
    """Key -> English text."""
    return read_strings(os.path.join(RES, "values/strings.xml"))


def read_translations():
    """(locale, {key: text}) for every translated locale, in locale order."""
    for entry in sorted(os.listdir(RES)):
        if not entry.startswith("values-"):
            continue
        path = os.path.join(RES, entry, "strings.xml")
        if not os.path.exists(path):
            continue
        yield ios_locale(entry[len("values-"):]), read_strings(path)


def unit(value):
    return {"stringUnit": {"state": "translated", "value": value}}


def write_catalog(path, strings):
    catalog = {"sourceLanguage": "en", "strings": strings, "version": "1.0"}
    with open(path, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write("\n")
