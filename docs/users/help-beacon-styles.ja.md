---
title: ビーコンのスタイル
layout: page
parent: Using Soundscape
has_toc: true
lang: ja
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# ビーコンのスタイル

Soundscape では、**設定 → オーディオ → 音声ビーコンのスタイル** で、いくつかの異なるビーコン音から選べます。これらはすべて同じ役割を果たします。つまり、3D 空間オーディオを使って目的地へと引き寄せます。しかし、それぞれの音はまったく異なって聞こえます。

各ビーコンは、ループ再生される少数のサンプルで構成されています。Soundscape は、自分が向いている方向とビーコンの方向との間の角度に応じて適切なサンプルを自動的に選びます。そのため、頭を回したり移動したりすると音が変化します。「軸上 (on-axis)」のサンプルはビーコンの方向を向いているときに再生され、「後方 (behind)」のサンプルはビーコンが自分の後ろにあるときに再生されます。

## このページの使い方

以下の各ビーコンには次のものがあります。

- **ループ再生される音声サンプル**。各角度のバリエーションを順番に再生し、軸上 (正面) の音から始まり後方の音で終わるので、自分が回転したときにビーコンがどのように変化するかを聞くことができます。
- **周波数スペクトルのプロット**。ビーコンのエネルギーが可聴域のどこに位置するかを示します。特定の周波数に聴力の低下がある場合は、よく聞こえる帯域にエネルギーの大半があるビーコンが最も追いやすいでしょう。

プロットは、最も大きいピークが 0&nbsp;dB になるように正規化されています。これらの曲線は、各ビーコンの周波数成分の*形状*を比較するのに役立つもので、絶対的な音量を表すものではありません。

**注:** ここでの音声サンプルはモノラルであり、3D 空間化は使用していません。アプリ本体では音はバイノーラルでレンダリングされるため、ステレオ ヘッドホンを着用すると特定の方向から聞こえるように感じられます。

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
