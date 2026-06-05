---
title: Beacon Styles
layout: page
parent: "Using Soundscape"
has_toc: true
lang: en-GB
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Beacon Styles

Soundscape lets you choose between several different beacon sounds in
**Settings → Audio → Audio Beacon Styles**. They all do the same job —
pulling you towards a destination using 3D spatial audio — but they sound
very different from each other.

Each beacon is built from a small set of looping samples. Soundscape
automatically picks the right sample for the angle between where you're facing
and the direction of the beacon, so the sound changes as you turn your head or
move. The "on-axis" samples play when you're pointing at the beacon; the
"behind" samples play when it's behind you.

## How to use this page

For each beacon below you'll find:

- **A looping audio sample** that plays each of the angle variants in turn,
  starting from the on-axis (in-front) sound and ending with the behind-you
  sound, so you can hear how the beacon changes as you rotate.
- **A frequency spectrum plot** showing where the beacon's energy sits in
  the audible range. If you have a hearing loss in particular frequencies,
  beacons with most of their energy in the regions you hear well will be
  easiest to follow.

The plots are normalised so the loudest peak sits at 0&nbsp;dB; the curves are
useful for comparing the *shape* of each beacon's frequency content, not its
absolute loudness.

**Note:** the audio samples here are mono and do not use 3D spatialisation.
In the app itself the sound is rendered binaurally, so it will appear to
come from a specific direction when you wear stereo headphones.

---

{% include beacon-styles.md %}

<script>
  /* Only allow one beacon audio sample to play at a time. Kramdown strips
     newlines inside <script> blocks, so a // comment would swallow the rest
     of the script — use a block comment. */
  document.addEventListener("DOMContentLoaded", function () {
    var players = document.querySelectorAll("audio");
    players.forEach(function (player) {
      player.addEventListener("play", function () {
        players.forEach(function (other) {
          if (other !== player) {
            other.pause();
          }
        });
      });
    });
  });
</script>
