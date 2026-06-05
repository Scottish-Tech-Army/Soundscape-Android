---
title: Estilos de señal
layout: page
parent: "Usar Soundscape"
has_toc: true
lang: es
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Estilos de señal

Soundscape le permite elegir entre varios sonidos de señal distintos en
**Ajustes → Audio → Estilos de señal de audio**. Todos hacen el mismo trabajo —
atraerle hacia un destino mediante audio espacial 3D — pero suenan
muy distintos entre sí.

Cada señal se construye a partir de un pequeño conjunto de muestras en bucle. Soundscape
elige automáticamente la muestra adecuada para el ángulo entre la dirección en la que mira
y la dirección de la señal, de modo que el sonido cambia a medida que gira la cabeza o se
mueve. Las muestras "en el eje" se reproducen cuando apunta a la señal; las
muestras "detrás" se reproducen cuando está detrás de usted.

## Cómo usar esta página

Para cada señal a continuación encontrará:

- **Una muestra de audio en bucle** que reproduce por turnos cada una de las variantes de ángulo,
  empezando por el sonido en el eje (de frente) y terminando con el sonido de detrás,
  para que pueda oír cómo cambia la señal a medida que rota.
- **Un gráfico del espectro de frecuencias** que muestra dónde se sitúa la energía de la señal en
  el rango audible. Si tiene una pérdida auditiva en frecuencias concretas,
  las señales con la mayor parte de su energía en las regiones que oye bien serán
  las más fáciles de seguir.

Los gráficos están normalizados para que el pico más fuerte se sitúe en 0&nbsp;dB; las curvas son
útiles para comparar la *forma* del contenido de frecuencia de cada señal, no su
volumen absoluto.

**Nota:** las muestras de audio aquí son mono y no usan espacialización 3D.
En la propia aplicación el sonido se renderiza de forma binaural, por lo que parecerá
provenir de una dirección específica cuando lleve auriculares estéreo.

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
