---
title: Stili audiofaro
layout: page
parent: "Usare Soundscape"
has_toc: true
lang: it
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Stili audiofaro

Soundscape ti permette di scegliere tra diversi suoni dell'audiofaro in
**Impostazioni → Audio → Stili audiofaro**. Svolgono tutti lo stesso compito —
guidarti verso una destinazione utilizzando l'audio spaziale 3D — ma hanno un suono
molto diverso l'uno dall'altro.

Ogni audiofaro è costruito a partire da un piccolo insieme di campioni in loop. Soundscape
sceglie automaticamente il campione giusto per l'angolo tra la direzione verso cui sei rivolto
e la direzione dell'audiofaro, in modo che il suono cambi mentre giri la testa o
ti muovi. I campioni "in asse" vengono riprodotti quando sei rivolto verso l'audiofaro; i
campioni "dietro" vengono riprodotti quando si trova dietro di te.

## Come usare questa pagina

Per ciascun audiofaro qui sotto troverai:

- **Un campione audio in loop** che riproduce in sequenza ciascuna delle varianti di angolo,
  partendo dal suono in asse (di fronte) e terminando con il suono
  dietro di te, così puoi sentire come l'audiofaro cambia mentre ruoti.
- **Un grafico dello spettro di frequenza** che mostra dove si colloca l'energia dell'audiofaro
  all'interno della gamma udibile. Se hai una perdita uditiva in particolari frequenze,
  gli audiofari con la maggior parte della loro energia nelle regioni che senti bene saranno
  i più facili da seguire.

I grafici sono normalizzati in modo che il picco più alto si trovi a 0&nbsp;dB; le curve sono
utili per confrontare la *forma* del contenuto in frequenza di ciascun audiofaro, non la sua
intensità assoluta.

**Nota:** i campioni audio qui presenti sono mono e non utilizzano la spazializzazione 3D.
Nell'app stessa il suono viene reso in modo binaurale, quindi sembrerà
provenire da una direzione specifica quando indossi cuffie stereo.

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
