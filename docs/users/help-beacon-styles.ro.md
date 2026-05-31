---
title: Stiluri de baliză
layout: page
parent: Using Soundscape
has_toc: true
lang: ro
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Stiluri de baliză

Soundscape îți permite să alegi dintre mai multe sunete de baliză diferite în
**Setări → Audio → Stiluri de baliză audio**. Toate îndeplinesc aceeași sarcină —
te atrag spre o destinație folosind audio spațial 3D — dar sună
foarte diferit unele de altele.

Fiecare baliză este construită dintr-un set mic de mostre care se repetă în buclă. Soundscape
alege automat mostra potrivită pentru unghiul dintre direcția în care ești orientat
și direcția balizei, astfel încât sunetul se schimbă pe măsură ce îți întorci capul sau
te miști. Mostrele „pe axă" se redau când ești orientat spre baliză; mostrele
„din spate" se redau când aceasta se află în spatele tău.

## Cum se folosește această pagină

Pentru fiecare baliză de mai jos vei găsi:

- **O mostră audio în buclă** care redă pe rând fiecare dintre variantele de unghi,
  începând cu sunetul pe axă (din față) și terminând cu sunetul din spatele tău,
  astfel încât să poți auzi cum se schimbă baliza pe măsură ce te rotești.
- **Un grafic al spectrului de frecvențe** care arată unde se situează energia balizei în
  intervalul audibil. Dacă ai o pierdere de auz la anumite frecvențe,
  balizele cu cea mai mare parte a energiei lor în zonele pe care le auzi bine vor fi
  cel mai ușor de urmărit.

Graficele sunt normalizate astfel încât vârful cel mai puternic să se situeze la 0&nbsp;dB; curbele sunt
utile pentru a compara *forma* conținutului de frecvențe al fiecărei balize, nu
volumul său absolut.

**Notă:** mostrele audio de aici sunt mono și nu folosesc spațializare 3D.
În aplicația propriu-zisă sunetul este redat binaural, astfel încât va părea că
vine dintr-o direcție specifică atunci când porți căști stereo.

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
