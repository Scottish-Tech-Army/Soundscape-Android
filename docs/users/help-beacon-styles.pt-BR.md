---
title: Estilos do Sinalizador Sonoro
layout: page
parent: "Usando o Soundscape"
has_toc: true
lang: pt-BR
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Estilos do Sinalizador Sonoro

O Soundscape permite que você escolha entre vários sons diferentes de sinalizador em
**Ajustes → Áudio → Estilos do Sinalizador Sonoro**. Todos eles fazem o mesmo trabalho —
atrair você em direção a um destino usando áudio espacial 3D — mas soam
muito diferentes uns dos outros.

Cada sinalizador é construído a partir de um pequeno conjunto de amostras em loop. O Soundscape
escolhe automaticamente a amostra certa para o ângulo entre a direção para a qual você está voltado
e a direção do sinalizador, de modo que o som muda à medida que você vira a cabeça ou
se move. As amostras "no eixo" tocam quando você está apontando para o sinalizador; as
amostras "atrás" tocam quando ele está atrás de você.

## Como usar esta página

Para cada sinalizador abaixo, você encontrará:

- **Uma amostra de áudio em loop** que toca cada uma das variantes de ângulo por vez,
  começando pelo som no eixo (à frente) e terminando com o som atrás de você,
  para que você possa ouvir como o sinalizador muda à medida que você gira.
- **Um gráfico de espectro de frequência** que mostra onde a energia do sinalizador se situa na
  faixa audível. Se você tiver perda auditiva em determinadas frequências,
  os sinalizadores com a maior parte de sua energia nas regiões que você ouve bem serão os
  mais fáceis de seguir.

Os gráficos são normalizados para que o pico mais alto fique em 0&nbsp;dB; as curvas são
úteis para comparar o *formato* do conteúdo de frequência de cada sinalizador, não sua
intensidade absoluta.

**Nota:** as amostras de áudio aqui são mono e não usam espacialização 3D.
No próprio aplicativo, o som é renderizado de forma binaural, de modo que parecerá
vir de uma direção específica quando você usar fones de ouvido estéreo.

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
