---
title: Sesli İşaret Stilleri
layout: page
parent: "Soundscape Kullanımı"
has_toc: true
lang: tr
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Sesli İşaret Stilleri

Soundscape, **Ayarlar → Ses → Sesli işaret stilleri** bölümünden birkaç
farklı sesli işaret sesi arasında seçim yapmanıza olanak tanır. Hepsi aynı
işi yapar — sizi 3 boyutlu uzamsal ses kullanarak bir hedefe doğru çeker —
ancak birbirlerinden oldukça farklı seslere sahiptirler.

Her sesli işaret, küçük bir döngülü örnek kümesinden oluşturulur. Soundscape,
baktığınız yön ile sesli işaretin yönü arasındaki açıya uygun örneği otomatik
olarak seçer; böylece başınızı çevirdiğinizde veya hareket ettiğinizde ses
değişir. "Eksen üzeri" örnekler sesli işarete doğru baktığınızda çalar;
"arkada" örnekler ise sesli işaret arkanızdayken çalar.

## Bu sayfa nasıl kullanılır

Aşağıdaki her sesli işaret için şunları bulacaksınız:

- **Döngülü bir ses örneği**, eksen üzeri (önünüzdeki) sesten başlayıp
  arkanızdaki sesle biterek her açı varyantını sırayla çalar; böylece
  döndüğünüzde sesli işaretin nasıl değiştiğini duyabilirsiniz.
- **Bir frekans spektrumu grafiği**, sesli işaretin enerjisinin duyulabilir
  aralığın neresinde olduğunu gösterir. Belirli frekanslarda işitme kaybınız
  varsa, enerjisinin çoğu iyi duyduğunuz bölgelerde olan sesli işaretleri
  takip etmek en kolay olacaktır.

Grafikler, en yüksek tepe 0&nbsp;dB'de olacak şekilde normalize edilmiştir;
eğriler, her sesli işaretin frekans içeriğinin mutlak yüksekliğini değil,
*şeklini* karşılaştırmak için kullanışlıdır.

**Not:** buradaki ses örnekleri mono'dur ve 3 boyutlu uzamsallaştırma
kullanmaz. Uygulamanın kendisinde ses binaural olarak işlenir; bu nedenle
stereo kulaklık taktığınızda belirli bir yönden geliyormuş gibi görünecektir.

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
