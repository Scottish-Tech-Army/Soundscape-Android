---
title: Beacon-Stile
layout: page
parent: "Soundscape verwenden"
has_toc: true
lang: de
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Beacon-Stile

Mit Soundscape können Sie unter
**Einstellungen → Audio → Audiobeacon-Stile** zwischen mehreren verschiedenen Beacon-Tönen wählen.
Sie alle erfüllen denselben Zweck –
sie ziehen Sie mithilfe von räumlichem 3D-Audio zu einem Ziel –, aber sie klingen
sehr unterschiedlich.

Jedes Beacon ist aus einer kleinen Anzahl von Endlossamples aufgebaut. Soundscape
wählt automatisch das richtige Sample für den Winkel zwischen Ihrer Blickrichtung
und der Richtung des Beacons aus, sodass sich der Ton ändert, wenn Sie Ihren Kopf drehen oder
sich bewegen. Die „Achsen“-Samples werden abgespielt, wenn Sie auf das Beacon zeigen; die
„Hinter“-Samples werden abgespielt, wenn es sich hinter Ihnen befindet.

## So verwenden Sie diese Seite

Für jedes Beacon unten finden Sie:

- **Ein Endlos-Audiosample**, das nacheinander jede der Winkelvarianten abspielt,
  beginnend mit dem Achsen-Ton (von vorne) und endend mit dem Ton von hinten,
  sodass Sie hören können, wie sich das Beacon ändert, während Sie sich drehen.
- **Ein Frequenzspektrum-Diagramm**, das zeigt, wo die Energie des Beacons im
  hörbaren Bereich liegt. Wenn Sie einen Hörverlust in bestimmten Frequenzen haben,
  sind Beacons, deren Energie hauptsächlich in den Bereichen liegt, die Sie gut hören,
  am einfachsten zu folgen.

Die Diagramme sind so normalisiert, dass die lauteste Spitze bei 0&nbsp;dB liegt; die Kurven sind
nützlich, um die *Form* des Frequenzinhalts jedes Beacons zu vergleichen, nicht seine
absolute Lautstärke.

**Hinweis:** Die Audiosamples hier sind monaural und verwenden keine 3D-Räumlichkeit.
In der App selbst wird der Ton binaural gerendert, sodass er aus einer bestimmten
Richtung zu kommen scheint, wenn Sie Stereokopfhörer tragen.

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
