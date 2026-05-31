---
title: Style dźwięku naprowadzającego
layout: page
parent: Using Soundscape
has_toc: true
lang: pl
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Style dźwięku naprowadzającego

Soundscape pozwala wybierać spośród kilku różnych dźwięków naprowadzających w
**Ustawienia → Dźwięk → Style dźwięku naprowadzającego**. Wszystkie pełnią to samo zadanie —
przyciągają Cię do celu za pomocą przestrzennego dźwięku 3D — ale brzmią
bardzo różnie od siebie.

Każdy dźwięk naprowadzający jest zbudowany z niewielkiego zestawu zapętlonych próbek. Soundscape
automatycznie wybiera właściwą próbkę dla kąta między kierunkiem, w którym jesteś zwrócony,
a kierunkiem dźwięku naprowadzającego, więc dźwięk zmienia się, gdy obracasz głowę lub
się poruszasz. Próbki „na osi” odtwarzane są, gdy jesteś zwrócony w stronę dźwięku naprowadzającego; próbki
„za” odtwarzane są, gdy znajduje się on za Tobą.

## Jak korzystać z tej strony

Dla każdego dźwięku naprowadzającego poniżej znajdziesz:

- **Zapętloną próbkę audio**, która odtwarza po kolei każdy z wariantów kątowych,
  zaczynając od dźwięku na osi (z przodu), a kończąc na dźwięku zza Ciebie,
  abyś mógł usłyszeć, jak dźwięk naprowadzający zmienia się, gdy się obracasz.
- **Wykres widma częstotliwości** pokazujący, gdzie w zakresie słyszalnym leży
  energia dźwięku naprowadzającego. Jeśli masz ubytek słuchu w określonych częstotliwościach,
  dźwięki naprowadzające z większością energii w zakresach, które dobrze słyszysz, będą
  najłatwiejsze do podążania za nimi.

Wykresy są znormalizowane tak, że najgłośniejszy szczyt znajduje się na 0&nbsp;dB; krzywe są
przydatne do porównywania *kształtu* zawartości częstotliwościowej każdego dźwięku naprowadzającego, a nie jego
bezwzględnej głośności.

**Uwaga:** próbki audio tutaj są monofoniczne i nie wykorzystują przestrzennego dźwięku 3D.
W samej aplikacji dźwięk jest renderowany binauralnie, więc będzie wydawał się
dochodzić z określonego kierunku, gdy nosisz słuchawki stereo.

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
