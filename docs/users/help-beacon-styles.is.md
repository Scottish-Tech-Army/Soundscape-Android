---
title: Stílar hljóðvita
layout: page
parent: "Að nota Soundscape"
has_toc: true
lang: is
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Stílar hljóðvita

Soundscape gerir þér kleift að velja á milli nokkurra mismunandi hljóðvitahljóða í
**Stillingar → Hljóð → Stílar hljóðvita**. Þau gegna öll sama hlutverki —
að draga þig að áfangastað með þrívíðu rúmhljóði — en þau hljóma
mjög ólíkt hvert öðru.

Hver hljóðviti er byggður upp úr litlu safni af endurteknum hljóðbútum. Soundscape
velur sjálfkrafa réttan hljóðbút fyrir hornið milli þess sem þú snýrð að
og áttar hljóðvitans, svo hljóðið breytist þegar þú snýrð höfðinu eða
hreyfir þig. „Á-ás“ hljóðbútarnir spilast þegar þú beinir þér að hljóðvitanum;
„fyrir aftan“ hljóðbútarnir spilast þegar hann er fyrir aftan þig.

## Hvernig á að nota þessa síðu

Fyrir hvern hljóðvita hér að neðan finnur þú:

- **Endurtekinn hljóðbút** sem spilar hvert hornafbrigði í röð,
  byrjar á á-ás (fyrir framan) hljóðinu og endar á fyrir-aftan-þig
  hljóðinu, svo þú getir heyrt hvernig hljóðvitinn breytist þegar þú snýrð þér.
- **Tíðnirófsrit** sem sýnir hvar orka hljóðvitans liggur á
  heyranlega sviðinu. Ef þú ert með heyrnartap á tilteknum tíðnum verða
  hljóðvitar sem hafa mestu orkuna á þeim svæðum sem þú heyrir vel auðveldastir að fylgja.

Ritin eru stöðluð þannig að hæsti toppurinn situr við 0&nbsp;dB; ferlarnir eru
gagnlegir til að bera saman *lögun* tíðniinnihalds hvers hljóðvita, ekki
algildan styrk hans.

**Athugið:** hljóðbútarnir hér eru í mónó og nota ekki þrívíða rúmstaðsetningu.
Í forritinu sjálfu er hljóðið unnið tvíheyrt (binaurally), svo það mun virðast
koma úr tiltekinni átt þegar þú notar steríóheyrnartól.

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
