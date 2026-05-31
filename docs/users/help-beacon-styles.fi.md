---
title: Majakan tyylit
layout: page
parent: Using Soundscape
has_toc: true
lang: fi
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Majakan tyylit

Soundscapessa voit valita useiden erilaisten majakkaäänien välillä kohdassa
**Asetukset → Ääni → Äänimajakan tyylit**. Ne kaikki tekevät saman työn —
vetävät sinua määränpäätä kohti 3D-tilaäänen avulla — mutta ne kuulostavat
hyvin erilaisilta toisistaan.

Jokainen majakka rakentuu pienestä joukosta toistuvia näytteitä. Soundscape
valitsee automaattisesti oikean näytteen sen kulman perusteella, joka on sinun
suuntasi ja majakan suunnan välillä, joten ääni muuttuu, kun käännät päätäsi tai
liikut. "Akselin suuntaiset" näytteet soivat, kun osoitat majakkaa kohti;
"takana"-näytteet soivat, kun se on takanasi.

## Kuinka tätä sivua käytetään

Jokaisesta alla olevasta majakasta löydät:

- **Toistuvan ääninäytteen**, joka toistaa kunkin kulmavariaation vuorollaan
  alkaen akselin suuntaisesta (edessä olevasta) äänestä ja päättyen takana
  olevaan ääneen, jotta voit kuulla, kuinka majakka muuttuu, kun käännyt.
- **Taajuusspektrikaavion**, joka näyttää, missä majakan energia sijaitsee
  kuuloalueella. Jos sinulla on kuulonalenema tietyillä taajuuksilla,
  majakat, joiden energiasta suurin osa sijaitsee alueilla, jotka kuulet hyvin, ovat
  helpoimpia seurata.

Kaaviot on normalisoitu niin, että voimakkain piikki sijaitsee kohdassa 0&nbsp;dB; käyrät ovat
hyödyllisiä kunkin majakan taajuussisällön *muodon* vertailuun, ei sen
absoluuttisen äänenvoimakkuuden.

**Huomautus:** tässä olevat ääninäytteet ovat monoa eivätkä käytä 3D-tilaäänitystä.
Itse sovelluksessa ääni renderöidään binauraalisesti, joten se vaikuttaa tulevan
tietystä suunnasta, kun käytät stereokuulokkeita.

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
