---
title: Stilarter for lydfyr
layout: page
parent: "Brug af Soundscape"
has_toc: true
lang: da
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Stilarter for lydfyr

Soundscape giver dig mulighed for at vælge mellem flere forskellige lydfyrlyde i
**Indstillinger → Lyd → Stilarter for lydfyr**. De udfører alle den samme opgave —
de trækker dig mod en destination ved hjælp af 3D rumlig lyd — men de lyder
meget forskelligt fra hinanden.

Hvert lydfyr er opbygget af et lille sæt loopende lydprøver. Soundscape
vælger automatisk den rigtige lydprøve for vinklen mellem den retning, du vender,
og retningen til lydfyret, så lyden ændrer sig, når du drejer hovedet eller
bevæger dig. "On-axis"-lydprøverne afspilles, når du peger mod lydfyret; de
"bag-dig"-lydprøver afspilles, når det er bag dig.

## Sådan bruger du denne side

For hvert lydfyr nedenfor finder du:

- **En loopende lydprøve**, der afspiller hver af vinkelvarianterne efter tur,
  startende fra on-axis-lyden (foran) og sluttende med bag-dig-lyden,
  så du kan høre, hvordan lydfyret ændrer sig, når du roterer.
- **Et frekvensspektrumdiagram**, der viser, hvor lydfyrets energi ligger i
  det hørbare område. Hvis du har et høretab i bestemte frekvenser, vil
  lydfyr med det meste af deres energi i de områder, du hører godt, være
  lettest at følge.

Diagrammerne er normaliseret, så den højeste top ligger ved 0&nbsp;dB; kurverne er
nyttige til at sammenligne *formen* af hvert lydfyrs frekvensindhold, ikke dets
absolutte lydstyrke.

**Bemærk:** lydprøverne her er mono og bruger ikke 3D-rumliggørelse.
I selve appen gengives lyden binauralt, så den vil synes at
komme fra en bestemt retning, når du bærer stereohovedtelefoner.

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
