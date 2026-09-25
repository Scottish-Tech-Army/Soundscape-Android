#!/usr/bin/env python3
"""Print everything needed to write or refresh one language's questionnaire.

Usage: questionnaire_context.py <weblate-code> [extra string keys...]

Prints, for the language:
  - STRINGS: key | English | current translation, for the terms and example
    strings every sheet quotes (plus any extra keys given);
  - GUIDANCE OPEN / GLOSSARY: from translations/guidance/<code>.md;
  - SHEET: the current docs/translation-questions/questions-<code>.md.

Quote only what this prints (or what you grep from the same strings.xml):
never paraphrase an app string into a sheet as if it were the real text.
"""
import os
import re
import subprocess
import sys

ROOT = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
RES = f"{ROOT}/shared/src/commonMain/composeResources"

# Weblate code -> Compose Resources values-* suffix, where they differ.
VALUES_DIR = {"en_GB": "en-rGB", "fr_CA": "fr-rCA", "pt_BR": "pt-rBR",
              "zh_Hans": "zh-rCN", "nb_NO": "nb", "id": "in"}

KEYS = [
    # the terms the "What is Soundscape" section explains
    "beacon_audio_beacon", "callouts_automatic_callouts", "markers_title", "routes_title",
    # example callouts (map names go in UNDECLINED, exactly as the template gets them)
    "confect_name_pavement_next_to", "directions_along_heading_n", "confect_name_to",
    "confect_name_dead_end", "osm_path", "directions_name_goes_left",
    # things questions commonly quote
    "tour_beacon_demo", "first_launch_prompt_title", "first_launch_welcome_title",
    "callouts_verbosity_level_detailed", "callouts_verbosity_level_balanced",
    "callouts_verbosity_level_quiet", "callouts_verbosity_level_silent",
    "sleep_sleep", "sleep_sleeping", "sleep_snoozing", "sleep_wake_on_leave",
    "menu_route_next_waypoint", "search_bar_hint", "callouts_places_and_landmarks",
    "talkback_double_tap_template", "beacon_action_mute_beacon_acc_hint",
]


def load(values_dir):
    with open(f"{RES}/{values_dir}/strings.xml", encoding="utf-8") as f:
        return dict(re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>', f.read(), re.S))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    code = sys.argv[1]
    en = load("values")
    tg = load("values-" + VALUES_DIR.get(code, code))
    print("### STRINGS")
    for k in KEYS + sys.argv[2:]:
        print(f"{k} | {en.get(k, '')[:120]} | {tg.get(k, '')[:300]}")

    g = f"{ROOT}/translations/guidance/{code}.md"
    if os.path.exists(g):
        s = open(g, encoding="utf-8").read()
        m = re.search(r"## Open questions.*?(?=\n## |\Z)", s, re.S)
        print("### GUIDANCE OPEN\n" + (m.group(0) if m else "(none)"))
        m = re.search(r"## Glossary.*?(?=\n## )", s, re.S)
        print("### GLOSSARY\n" + (m.group(0)[:3000] if m else "(none)"))
    else:
        print(f"### GUIDANCE\n(no {g})")

    sheet = f"{ROOT}/docs/translation-questions/questions-{code}.md"
    print("### SHEET")
    print(open(sheet, encoding="utf-8").read() if os.path.exists(sheet) else f"(no {sheet} yet)")


if __name__ == "__main__":
    main()
