---
title: Stiler for lydsignal
layout: page
parent: "Bruke Soundscape"
has_toc: true
lang: nb
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Stiler for lydsignal

Soundscape lar deg velge mellom flere forskjellige lydsignaltoner i
**Innstillinger → Lyd → Stiler for lydsignal**. De gjør alle samme jobb –
trekker deg mot en destinasjon ved hjelp av romlig 3D-lyd – men de høres
svært forskjellige ut fra hverandre.

Hvert lydsignal er bygd opp av et lite sett med løkkede lydprøver. Soundscape
velger automatisk den riktige lydprøven for vinkelen mellom retningen du vender
mot og retningen til lydsignalet, slik at lyden endrer seg når du vrir på hodet eller
beveger deg. «På akse»-prøvene spilles av når du peker mot lydsignalet; «bak»-prøvene
spilles av når det er bak deg.

## Slik bruker du denne siden

For hvert lydsignal nedenfor finner du:

- **En løkket lydprøve** som spiller av hver av vinkelvariantene etter tur,
  fra på akse-lyden (foran) og slutter med bak-deg-lyden,
  slik at du kan høre hvordan lydsignalet endrer seg når du roterer.
- **Et frekvensspektrumdiagram** som viser hvor lydsignalets energi ligger i
  det hørbare området. Hvis du har et hørselstap i bestemte frekvenser,
  vil lydsignaler med mesteparten av energien i områdene du hører godt, være
  enklest å følge.

Diagrammene er normalisert slik at den høyeste toppen ligger på 0&nbsp;dB; kurvene er
nyttige for å sammenligne *formen* på hvert lydsignals frekvensinnhold, ikke dets
absolutte lydstyrke.

**Merk:** lydprøvene her er i mono og bruker ikke romlig 3D-gjengivelse.
I selve appen gjengis lyden binauralt, slik at den ser ut til å
komme fra en bestemt retning når du bruker stereohodetelefoner.

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
