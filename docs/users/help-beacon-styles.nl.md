---
title: Bakenstijlen
layout: page
parent: "Soundscape gebruiken"
has_toc: true
lang: nl
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Bakenstijlen

Met Soundscape kunt u kiezen uit verschillende bakengeluiden in
**Instellingen → Audio → Audiobakenstijlen**. Ze doen allemaal hetzelfde werk —
u naar een bestemming trekken met behulp van ruimtelijke 3D-audio — maar ze klinken
heel verschillend van elkaar.

Elk baken is opgebouwd uit een kleine set herhalende geluidsfragmenten. Soundscape
kiest automatisch het juiste fragment voor de hoek tussen de richting waarin u kijkt
en de richting van het baken, zodat het geluid verandert wanneer u uw hoofd draait of
beweegt. De "on-axis"-fragmenten spelen wanneer u naar het baken wijst; de
"achter"-fragmenten spelen wanneer het zich achter u bevindt.

## Hoe gebruikt u deze pagina

Voor elk baken hieronder vindt u:

- **Een herhalend geluidsfragment** dat elk van de hoekvarianten om de beurt afspeelt,
  beginnend met het on-axis-geluid (vóór u) en eindigend met het geluid achter u,
  zodat u kunt horen hoe het baken verandert terwijl u draait.
- **Een frequentiespectrumgrafiek** die laat zien waar de energie van het baken
  in het hoorbare bereik ligt. Als u een gehoorverlies hebt bij bepaalde frequenties,
  zijn bakens met het grootste deel van hun energie in de gebieden die u goed hoort het
  gemakkelijkst te volgen.

De grafieken zijn genormaliseerd zodat de luidste piek op 0&nbsp;dB ligt; de curven zijn
nuttig om de *vorm* van de frequentie-inhoud van elk baken te vergelijken, niet de
absolute geluidssterkte ervan.

**Let op:** de geluidsfragmenten hier zijn mono en gebruiken geen ruimtelijke 3D-weergave.
In de app zelf wordt het geluid binauraal weergegeven, zodat het uit een specifieke
richting lijkt te komen wanneer u een stereokoptelefoon draagt.

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
