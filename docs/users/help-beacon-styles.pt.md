---
title: Estilos do Sinal de Áudio
layout: page
parent: "Utilizar o Soundscape"
has_toc: true
lang: pt
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Estilos do Sinal de Áudio

O Soundscape permite-lhe escolher entre vários sons de sinal diferentes em
**Definições → Áudio → Estilos do Sinal de Áudio**. Todos fazem o mesmo trabalho —
atraí-lo na direção de um destino utilizando áudio espacial 3D — mas soam
muito diferentes uns dos outros.

Cada sinal é construído a partir de um pequeno conjunto de amostras em ciclo. O Soundscape
escolhe automaticamente a amostra certa para o ângulo entre a direção em que está virado
e a direção do sinal, pelo que o som muda à medida que vira a cabeça ou
se move. As amostras "no eixo" são reproduzidas quando está virado para o sinal; as
amostras "atrás" são reproduzidas quando o sinal está atrás de si.

## Como utilizar esta página

Para cada sinal abaixo encontrará:

- **Uma amostra de áudio em ciclo** que reproduz cada uma das variantes de ângulo por sua vez,
  começando pelo som no eixo (à frente) e terminando com o som atrás de si,
  para que possa ouvir como o sinal muda à medida que roda.
- **Um gráfico do espetro de frequências** que mostra onde se situa a energia do sinal na
  gama audível. Se tiver perda de audição em determinadas frequências,
  os sinais com a maior parte da sua energia nas regiões que ouve bem serão
  mais fáceis de seguir.

Os gráficos são normalizados para que o pico mais alto se situe a 0&nbsp;dB; as curvas são
úteis para comparar a *forma* do conteúdo de frequências de cada sinal, e não a sua
intensidade sonora absoluta.

**Nota:** as amostras de áudio aqui apresentadas são mono e não utilizam espacialização 3D.
Na própria aplicação, o som é renderizado de forma binaural, pelo que parecerá
provir de uma direção específica quando usa auscultadores estéreo.

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
