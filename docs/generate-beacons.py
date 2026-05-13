#!/usr/bin/env python3
"""Generate combined looping audio samples and averaged frequency-spectrum plots
for each beacon style in app/src/main/cpp/AudioEngine.cpp (msc_BeaconDescriptors).

Outputs go to docs/assets/beacons/ and are referenced from
docs/users/help-beacon-styles.md.

Re-run whenever beacon WAV assets or the descriptor list change:

    python3 docs/generate-beacons.py

Requires: numpy, scipy, matplotlib, oggenc (vorbis-tools) on PATH.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from scipy.io import wavfile
from scipy.signal import welch


REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))
ASSETS_ROOT = os.path.join(REPO_ROOT, "app", "src", "main", "assets")
AUDIO_ENGINE_CPP = os.path.join(REPO_ROOT, "app", "src", "main", "cpp", "AudioEngine.cpp")
OUT_DIR = os.path.join(os.path.dirname(__file__), "assets", "beacons")
INCLUDE_PATH = os.path.join(os.path.dirname(__file__), "_includes", "beacon-styles.md")

# Prefix on every WAV path in the C++ descriptors; stripped to get a path
# relative to ASSETS_ROOT.
ANDROID_ASSET_PREFIX = "file:///android_asset/"


@dataclass
class Angle:
    wav: str        # path relative to ASSETS_ROOT
    max_angle: float  # degrees; this sample plays for |bearing| <= max_angle


@dataclass
class Beacon:
    name: str
    beats_in_phrase: int
    angles: list[Angle]


def _extract_braced(text: str, open_idx: int) -> tuple[str, int]:
    """Given the index of an opening '{', return (inner_content, close_idx).

    Beacon names and WAV paths never contain braces, so a simple depth
    counter is sufficient — no need to skip over string literals.
    """
    assert text[open_idx] == "{"
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[open_idx + 1 : i], i
    raise RuntimeError("Unbalanced braces while parsing AudioEngine.cpp")


def _parse_beacon_block(block: str) -> Beacon:
    """Parse one `{ "Name", beats, { angles... } }` descriptor block."""
    name_match = re.search(r'"([^"]*)"', block)
    if not name_match:
        raise RuntimeError(f"No beacon name found in block: {block!r}")
    name = name_match.group(1)

    rest = block[name_match.end():]
    beats_match = re.search(r"\d+", rest)
    if not beats_match:
        raise RuntimeError(f"No beats-in-phrase found for beacon {name!r}")
    beats = int(beats_match.group(0))

    angles_body, _ = _extract_braced(rest, rest.index("{", beats_match.end()))
    angles: list[Angle] = []
    for m in re.finditer(r'"([^"]*)"\s*,\s*([0-9]+(?:\.[0-9]+)?)', angles_body):
        wav = m.group(1)
        if wav.startswith(ANDROID_ASSET_PREFIX):
            wav = wav[len(ANDROID_ASSET_PREFIX):]
        angles.append(Angle(wav, float(m.group(2))))
    if not angles:
        raise RuntimeError(f"No angle variants found for beacon {name!r}")
    return Beacon(name, beats, angles)


def parse_beacons(cpp_path: str) -> list[Beacon]:
    """Parse msc_BeaconDescriptors out of AudioEngine.cpp.

    Reading the C++ source directly keeps this script in sync with any
    beacons added to the descriptor list.
    """
    with open(cpp_path, encoding="utf-8") as f:
        src = f.read()

    marker = "msc_BeaconDescriptors[] ="
    idx = src.find(marker)
    if idx == -1:
        raise RuntimeError(f"Could not find msc_BeaconDescriptors in {cpp_path}")

    body, _ = _extract_braced(src, src.index("{", idx + len(marker)))

    beacons: list[Beacon] = []
    pos = 0
    while True:
        open_idx = body.find("{", pos)
        if open_idx == -1:
            break
        block, close_idx = _extract_braced(body, open_idx)
        beacons.append(_parse_beacon_block(block))
        pos = close_idx + 1
    return beacons

# How many times to play each angle's loop sample in the combined audio.
LOOPS_PER_ANGLE = 2
# Silence inserted between angle sections, in seconds.
GAP_SECONDS = 0.4


def slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def load_wav_mono_float(path: str) -> tuple[int, np.ndarray]:
    rate, data = wavfile.read(path)
    if data.ndim > 1:
        data = data.mean(axis=1)
    if np.issubdtype(data.dtype, np.integer):
        max_val = float(np.iinfo(data.dtype).max)
        data = data.astype(np.float32) / max_val
    else:
        data = data.astype(np.float32)
    return rate, data


def build_combined(beacon: Beacon) -> tuple[int, np.ndarray]:
    """Concatenate LOOPS_PER_ANGLE plays of each angle sample, with a gap between."""
    samples: list[np.ndarray] = []
    rate = None
    for i, angle in enumerate(beacon.angles):
        path = os.path.join(ASSETS_ROOT, angle.wav)
        sr, audio = load_wav_mono_float(path)
        if rate is None:
            rate = sr
        elif sr != rate:
            # Defensive: every shipped beacon WAV is 44.1 kHz, but resample if not.
            ratio = rate / sr
            new_len = int(round(len(audio) * ratio))
            audio = np.interp(
                np.linspace(0, len(audio) - 1, new_len, dtype=np.float64),
                np.arange(len(audio)),
                audio,
            ).astype(np.float32)
        for _ in range(LOOPS_PER_ANGLE):
            samples.append(audio)
        if i != len(beacon.angles) - 1:
            samples.append(np.zeros(int(GAP_SECONDS * rate), dtype=np.float32))
    combined = np.concatenate(samples)
    peak = float(np.max(np.abs(combined))) or 1.0
    if peak > 1.0:
        combined = combined / peak
    return rate, combined


def write_ogg(rate: int, audio: np.ndarray, out_path: str) -> None:
    """Write WAV to a temp file, encode to OGG Vorbis with oggenc."""
    int16 = np.clip(audio * 32767.0, -32768, 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        tmp_wav = tmp.name
    try:
        wavfile.write(tmp_wav, rate, int16)
        subprocess.run(
            ["oggenc", "-Q", "-q", "5", "-o", out_path, tmp_wav],
            check=True,
        )
    finally:
        os.unlink(tmp_wav)


def plot_spectrum(beacon: Beacon, out_path: str) -> None:
    """Average Welch PSD across all angle samples and write a PNG."""
    psd_sum: np.ndarray | None = None
    freqs: np.ndarray | None = None
    rate = None
    for angle in beacon.angles:
        path = os.path.join(ASSETS_ROOT, angle.wav)
        sr, audio = load_wav_mono_float(path)
        if rate is None:
            rate = sr
        nperseg = min(4096, len(audio))
        f, pxx = welch(audio, fs=sr, nperseg=nperseg)
        if psd_sum is None:
            psd_sum = pxx
            freqs = f
        else:
            # Resample if different length (shouldn't happen with matching rates)
            if len(pxx) != len(psd_sum):
                pxx = np.interp(freqs, f, pxx)
            psd_sum = psd_sum + pxx
    assert psd_sum is not None and freqs is not None and rate is not None
    psd = psd_sum / len(beacon.angles)
    # Convert to dB, with a floor to avoid log(0).
    psd_db = 10.0 * np.log10(np.maximum(psd, 1e-12))
    # Normalize so peak = 0 dB.
    psd_db -= np.max(psd_db)

    fig, ax = plt.subplots(figsize=(7.0, 3.2))
    # Only show audible range, starting at 20 Hz.
    mask = freqs >= 20.0
    ax.semilogx(freqs[mask], psd_db[mask], color="#1e6091", linewidth=1.4)
    ax.fill_between(freqs[mask], psd_db[mask], -80, color="#1e6091", alpha=0.15)
    ax.set_xlim(20, min(20000, rate / 2))
    ax.set_ylim(-80, 3)
    ax.set_xlabel("Frequency (Hz)")
    ax.set_ylabel("Relative level (dB)")
    ax.set_title(f"{beacon.name} — averaged spectrum")
    ax.grid(True, which="both", linestyle=":", alpha=0.5)
    # Human-friendly tick labels at common audiology frequencies.
    ticks = [20, 50, 100, 250, 500, 1000, 2000, 4000, 8000, 16000]
    ticks = [t for t in ticks if t <= rate / 2]
    ax.set_xticks(ticks)
    ax.set_xticklabels([f"{t/1000:g}k" if t >= 1000 else str(t) for t in ticks])
    fig.tight_layout()
    fig.savefig(out_path, dpi=120)
    plt.close(fig)


def write_include(beacons: list[Beacon]) -> None:
    """Write the Jekyll include used by docs/users/help-beacon-styles.md."""
    lines: list[str] = [
        "<!-- Generated by docs/generate-beacons.py — do not edit by hand. -->",
        "",
    ]
    for beacon in beacons:
        s = slug(beacon.name)
        lines.append(f"## {beacon.name}")
        lines.append("")
        lines.append(f"**Beats per phrase:** {beacon.beats_in_phrase}  ")
        lines.append(f"**Angle variants:** {len(beacon.angles)}")
        lines.append("")
        lines.append("| Audio sample | Plays when bearing to beacon is |")
        lines.append("|---|---|")
        prev = 0.0
        for angle in beacon.angles:
            variant_name = os.path.splitext(os.path.basename(angle.wav))[0]
            lines.append(
                f"| `{variant_name}` | {prev:g}° – {angle.max_angle:g}° |"
            )
            prev = angle.max_angle
        lines.append("")
        lines.append(
            "<audio controls loop preload=\"none\" "
            f"src=\"{{{{ \"/assets/beacons/{s}.ogg\" | relative_url }}}}\">"
            "Your browser does not support the audio element.</audio>"
        )
        lines.append("")
        lines.append(
            f"![{beacon.name} averaged frequency spectrum]"
            f"({{{{ \"/assets/beacons/{s}-spectrum.png\" | relative_url }}}})"
        )
        lines.append("")
    os.makedirs(os.path.dirname(INCLUDE_PATH), exist_ok=True)
    with open(INCLUDE_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def main() -> int:
    if shutil.which("oggenc") is None:
        print("oggenc not found on PATH (install vorbis-tools)", file=sys.stderr)
        return 1
    beacons = parse_beacons(AUDIO_ENGINE_CPP)
    os.makedirs(OUT_DIR, exist_ok=True)
    for beacon in beacons:
        s = slug(beacon.name)
        ogg_path = os.path.join(OUT_DIR, f"{s}.ogg")
        png_path = os.path.join(OUT_DIR, f"{s}-spectrum.png")
        print(f"  {beacon.name:20s} -> {os.path.relpath(ogg_path, REPO_ROOT)}, {os.path.relpath(png_path, REPO_ROOT)}")
        rate, combined = build_combined(beacon)
        write_ogg(rate, combined, ogg_path)
        plot_spectrum(beacon, png_path)
    write_include(beacons)
    print(f"Wrote {len(beacons)} beacons to {os.path.relpath(OUT_DIR, REPO_ROOT)}")
    print(f"Wrote include {os.path.relpath(INCLUDE_PATH, REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
