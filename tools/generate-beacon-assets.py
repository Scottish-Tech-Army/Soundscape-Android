#!/usr/bin/env python3
"""Synthesise the .wav assets for the hearing-impairment beacon styles.

Generates nine mono 16-bit PCM files into app/src/main/assets/Sounds/:

    Bass_{A+,A,Behind}.wav        — low-frequency beacon for high-frequency
                                    hearing loss (presbycusis).
    Rhythm_{A+,A,Behind}.wav      — direction conveyed by rhythmic pattern
                                    rather than panning, for single-sided
                                    deafness / asymmetric loss.
    Broadband_{A+,A,Behind}.wav   — pink-noise burst beacon, for tinnitus
                                    and notched audiometric losses.

The existing beacons in app/src/main/assets/Sounds/ are 44.1 kHz / mono /
16-bit PCM with these loop durations:

    6  beats in phrase → 2.416 s   (e.g. Mallet, Drop, Signal)
    12 beats in phrase → 4.832 s   (e.g. Mallet_Slow)
    18 beats in phrase → 7.248 s

That works out to ~0.4027 s per beat (~149 BPM). The new beacons use the
same grid so they play in time with the rest of the engine.

Usage:
    python3 tools/generate-beacon-assets.py

Requires: numpy.
"""

from __future__ import annotations

import os
import wave

import numpy as np


SAMPLE_RATE = 44100
SECONDS_PER_BEAT = 2.416 / 6  # match the existing 6-beat loop length

OUT_DIR = os.path.normpath(
    os.path.join(
        os.path.dirname(__file__), os.pardir, "app", "src", "main", "assets", "Sounds"
    )
)


# --- WAV writing ------------------------------------------------------------


def write_wav(filename: str, samples: np.ndarray) -> None:
    """Write a mono 16-bit PCM .wav at SAMPLE_RATE.

    Samples are expected as float in roughly [-1, 1]; anything outside the
    range is hard-clipped. A tiny TPDF dither is applied before quantising so
    quiet decays don't sound granular.
    """
    peak = np.max(np.abs(samples))
    if peak > 0.99:
        samples = samples * (0.99 / peak)
    dither = (np.random.random(samples.shape) - np.random.random(samples.shape)) / 32768.0
    samples = np.clip(samples + dither, -1.0, 1.0)
    pcm = (samples * 32767).astype(np.int16)
    path = os.path.join(OUT_DIR, filename)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(pcm.tobytes())
    print(f"  wrote {filename}  ({len(pcm)} samples, {len(pcm)/SAMPLE_RATE:.3f}s)")


def beats_to_samples(beats: float) -> int:
    return int(round(beats * SECONDS_PER_BEAT * SAMPLE_RATE))


def make_buffer(beats_in_phrase: int) -> np.ndarray:
    return np.zeros(beats_to_samples(beats_in_phrase), dtype=np.float64)


def fade_in_out(buf: np.ndarray, fade_ms: float = 2.0) -> None:
    """Apply a short cosine fade at both edges to avoid loop-point clicks."""
    n = int(SAMPLE_RATE * fade_ms / 1000.0)
    if n <= 0 or 2 * n >= len(buf):
        return
    fade = 0.5 * (1.0 - np.cos(np.linspace(0.0, np.pi, n)))
    buf[:n] *= fade
    buf[-n:] *= fade[::-1]


# --- Beacon 1: Bass ---------------------------------------------------------
#
# Kick-drum-like attack + sustained low sine body. Each variant gets a lower
# fundamental and longer decay so the rear assets sound duller, supporting
# the front/back distinction even when the spatialiser is degraded.


def _bass_hit(freq: float, decay_s: float, transient_hz: float) -> np.ndarray:
    """One kick-drum-like hit: short pitched transient + decaying body."""
    duration = max(0.6, decay_s + 0.05)
    n = int(SAMPLE_RATE * duration)
    t = np.arange(n) / SAMPLE_RATE
    env = np.exp(-t / decay_s)
    # Body: sine at the target fundamental.
    body = np.sin(2 * np.pi * freq * t)
    # Pitched transient: descending sine that lands near the body fundamental,
    # gives the click without dumping much high-frequency energy.
    transient_env = np.exp(-t / 0.020)
    sweep = transient_hz * np.exp(-t / 0.010) + freq
    transient = np.sin(2 * np.pi * np.cumsum(sweep) / SAMPLE_RATE)
    return env * (body + 0.5 * transient_env * transient)


def make_bass(variant: str) -> np.ndarray:
    params = {
        # (fundamental_hz, body_decay_s, transient_start_hz)
        "A+": (110.0, 0.150, 220.0),
        "A": (90.0, 0.180, 160.0),
        "Behind": (70.0, 0.220, 110.0),
    }
    freq, decay, transient = params[variant]
    buf = make_buffer(6)
    # Two hits per phrase, on beats 0 and 3, matches Mallet's cadence.
    for beat in (0, 3):
        hit = _bass_hit(freq, decay, transient)
        start = beats_to_samples(beat)
        end = min(start + len(hit), len(buf))
        buf[start:end] += hit[: end - start]
    fade_in_out(buf)
    return buf


# --- Beacon 2: Rhythm -------------------------------------------------------
#
# A single broadband tick is the only timbre; direction is coded entirely by
# the *pattern* of ticks. A 12-beat phrase gives each pattern enough room to
# repeat audibly.


def _tick(pitch_hz: float = 1000.0, length_s: float = 0.060) -> np.ndarray:
    n = int(SAMPLE_RATE * length_s)
    t = np.arange(n) / SAMPLE_RATE
    env = np.exp(-t / 0.012)
    # Slightly inharmonic stack so the tick is broadband rather than tonal.
    sig = (
        np.sin(2 * np.pi * pitch_hz * t)
        + 0.6 * np.sin(2 * np.pi * pitch_hz * 1.7 * t)
        + 0.4 * np.sin(2 * np.pi * pitch_hz * 2.9 * t)
    )
    # Burst of pink-ish noise at the very front for the "click" attack.
    noise = np.random.randn(n) * np.exp(-t / 0.002)
    return env * (sig * 0.7 + noise * 0.6)


def _place(buf: np.ndarray, sample: np.ndarray, beat_offset: float) -> None:
    start = beats_to_samples(beat_offset)
    end = min(start + len(sample), len(buf))
    if end > start:
        buf[start:end] += sample[: end - start]


def make_rhythm(variant: str) -> np.ndarray:
    buf = make_buffer(12)
    tick_hi = _tick(1000.0)
    tick_lo = _tick(550.0, 0.080)
    if variant == "A+":
        # Steady fast pulses on every beat.
        for beat in range(12):
            _place(buf, tick_hi, beat)
    elif variant == "A":
        # Gallop: pairs of ticks every 3 beats.
        for start in (0, 3, 6, 9):
            _place(buf, tick_hi, start)
            _place(buf, tick_hi, start + 0.5)
    elif variant == "Behind":
        # Slow irregular thumps using the lower-pitched tick.
        _place(buf, tick_lo, 0)
        _place(buf, tick_lo, 4)
        _place(buf, tick_lo, 4.5)
        _place(buf, tick_lo, 9)
    else:
        raise ValueError(variant)
    fade_in_out(buf)
    return buf


# --- Beacon 3: Broadband ----------------------------------------------------
#
# Pink-noise burst beacon. Spectrum changes per zone:
#   A+      full band                   100 Hz – 8 kHz
#   A       high shelf rolled off above 3 kHz
#   Behind  low-pass below 1 kHz, doubled decay


def _pink_noise(n: int) -> np.ndarray:
    """Pink (1/f) noise via FFT shaping of white noise."""
    white = np.random.randn(n)
    spectrum = np.fft.rfft(white)
    freqs = np.fft.rfftfreq(n, d=1.0 / SAMPLE_RATE)
    # 1/sqrt(f) magnitude → -3 dB/octave power slope = pink. DC bin is zeroed.
    with np.errstate(divide="ignore"):
        scaling = np.where(freqs > 0, 1.0 / np.sqrt(freqs), 0.0)
    shaped = np.fft.irfft(spectrum * scaling, n=n)
    shaped -= shaped.mean()
    peak = np.max(np.abs(shaped))
    if peak > 0:
        shaped /= peak
    return shaped


def _one_pole_lowpass(x: np.ndarray, cutoff_hz: float) -> np.ndarray:
    if cutoff_hz >= SAMPLE_RATE / 2:
        return x.copy()
    rc = 1.0 / (2 * np.pi * cutoff_hz)
    dt = 1.0 / SAMPLE_RATE
    alpha = dt / (rc + dt)
    y = np.empty_like(x)
    y[0] = x[0] * alpha
    for i in range(1, len(x)):
        y[i] = y[i - 1] + alpha * (x[i] - y[i - 1])
    return y


def _one_pole_highpass(x: np.ndarray, cutoff_hz: float) -> np.ndarray:
    if cutoff_hz <= 0:
        return x.copy()
    rc = 1.0 / (2 * np.pi * cutoff_hz)
    dt = 1.0 / SAMPLE_RATE
    alpha = rc / (rc + dt)
    y = np.empty_like(x)
    y[0] = x[0]
    for i in range(1, len(x)):
        y[i] = alpha * (y[i - 1] + x[i] - x[i - 1])
    return y


def _broadband_burst(variant: str) -> np.ndarray:
    if variant == "Behind":
        decay_s, length_s = 0.160, 0.40
    else:
        decay_s, length_s = 0.080, 0.25
    n = int(SAMPLE_RATE * length_s)
    t = np.arange(n) / SAMPLE_RATE
    env = np.exp(-t / decay_s)
    # Sharp attack ramp (1 ms) so the click is preserved after filtering.
    attack_n = int(0.001 * SAMPLE_RATE)
    env[:attack_n] *= np.linspace(0.0, 1.0, attack_n)
    noise = _pink_noise(n)
    if variant == "A+":
        shaped = _one_pole_highpass(noise, 100.0)
        shaped = _one_pole_lowpass(shaped, 8000.0)
    elif variant == "A":
        shaped = _one_pole_highpass(noise, 100.0)
        shaped = _one_pole_lowpass(shaped, 3000.0)
    elif variant == "Behind":
        # Roll off the sub-audible rumble too; otherwise the burst dumps a lot
        # of <30 Hz energy that's inaudible but rattles phone speakers.
        shaped = _one_pole_highpass(noise, 80.0)
        shaped = _one_pole_lowpass(shaped, 1000.0)
    else:
        raise ValueError(variant)
    return env * shaped


def make_broadband(variant: str) -> np.ndarray:
    buf = make_buffer(6)
    # Single burst on the downbeat, like the percussive beacons.
    _place(buf, _broadband_burst(variant), 0)
    fade_in_out(buf)
    return buf


# --- Driver -----------------------------------------------------------------


def main() -> None:
    if not os.path.isdir(OUT_DIR):
        raise SystemExit(f"output dir not found: {OUT_DIR}")
    print(f"writing to {OUT_DIR}")
    np.random.seed(0)  # deterministic dither / noise for reproducible builds

    print("Bass:")
    for v in ("A+", "A", "Behind"):
        write_wav(f"Bass_{v}.wav", make_bass(v))

    print("Rhythm:")
    for v in ("A+", "A", "Behind"):
        write_wav(f"Rhythm_{v}.wav", make_rhythm(v))

    print("Broadband:")
    for v in ("A+", "A", "Behind"):
        write_wav(f"Broadband_{v}.wav", make_broadband(v))


if __name__ == "__main__":
    main()
