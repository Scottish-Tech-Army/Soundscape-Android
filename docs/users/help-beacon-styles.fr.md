---
title: Styles de balises
layout: page
parent: Using Soundscape
has_toc: true
lang: fr
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# Styles de balises

Soundscape vous permet de choisir parmi plusieurs sons de balise différents dans
**Réglages → Audio → Styles de balises sonores**. Ils remplissent tous le même rôle —
vous attirer vers une destination à l'aide de l'audio spatial 3D — mais ils sonnent
très différemment les uns des autres.

Chaque balise est constituée d'un petit ensemble d'échantillons en boucle. Soundscape
choisit automatiquement le bon échantillon en fonction de l'angle entre la direction
dans laquelle vous êtes orienté et la direction de la balise, de sorte que le son change
lorsque vous tournez la tête ou que vous vous déplacez. Les échantillons « dans l'axe »
sont joués lorsque vous êtes orienté vers la balise ; les échantillons « derrière » sont
joués lorsqu'elle se trouve derrière vous.

## Comment utiliser cette page

Pour chaque balise ci-dessous, vous trouverez :

- **Un échantillon audio en boucle** qui joue tour à tour chacune des variantes d'angle,
  en commençant par le son dans l'axe (devant) et en terminant par le son derrière vous,
  afin que vous puissiez entendre comment la balise change à mesure que vous pivotez.
- **Un graphique du spectre de fréquences** montrant où se situe l'énergie de la balise dans
  la plage audible. Si vous présentez une perte auditive sur certaines fréquences,
  les balises dont l'essentiel de l'énergie se situe dans les régions que vous entendez bien
  seront les plus faciles à suivre.

Les graphiques sont normalisés de sorte que le pic le plus fort se situe à 0&nbsp;dB ; les courbes sont
utiles pour comparer la *forme* du contenu fréquentiel de chaque balise, et non son
volume absolu.

**Remarque :** les échantillons audio présentés ici sont en mono et n'utilisent pas la spatialisation 3D.
Dans l'application elle-même, le son est rendu de manière binaurale, de sorte qu'il semblera
provenir d'une direction précise lorsque vous portez un casque stéréo.

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
