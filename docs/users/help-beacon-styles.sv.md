---
title: Ljudfyrstilar
layout: page
parent: "Använda Soundscape"
has_toc: true
lang: sv
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Ljudfyrstilar

Soundscape låter dig välja mellan flera olika ljudfyrsljud i
**Inställningar → Ljud → Ljudfyrstilar**. De gör alla samma jobb —
drar dig mot ett mål med hjälp av 3D-rumsljud — men de låter
mycket olika varandra.

Varje ljudfyr är uppbyggd av en liten uppsättning loopande ljudprov. Soundscape
väljer automatiskt rätt ljudprov för vinkeln mellan den riktning du är vänd mot
och ljudfyrens riktning, så ljudet ändras när du vrider på huvudet eller
rör dig. Ljudproven "i linje" spelas upp när du pekar mot ljudfyren;
ljudproven "bakom" spelas upp när den är bakom dig.

## Hur du använder den här sidan

För varje ljudfyr nedan hittar du:

- **Ett loopande ljudprov** som spelar upp var och en av vinkelvarianterna i tur och ordning,
  med början från ljudet i linje (framför) och avslutas med ljudet bakom dig,
  så att du kan höra hur ljudfyren ändras när du roterar.
- **Ett frekvensspektrumdiagram** som visar var ljudfyrens energi ligger i
  det hörbara området. Om du har en hörselnedsättning vid vissa frekvenser
  är ljudfyrar som har det mesta av sin energi i de områden du hör väl
  enklast att följa.

Diagrammen är normaliserade så att den högsta toppen ligger vid 0&nbsp;dB; kurvorna är
användbara för att jämföra *formen* på varje ljudfyrs frekvensinnehåll, inte dess
absoluta ljudstyrka.

**Obs:** ljudproven här är mono och använder inte 3D-rumslighet.
I själva appen återges ljudet binauralt, så det kommer att verka
komma från en specifik riktning när du bär stereohörlurar.

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
