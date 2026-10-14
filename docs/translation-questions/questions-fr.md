---
title: "Français"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fr/
---

# Traduction française de Soundscape — quelques questions

> Envoyez vos réponses par e-mail à **soundscapeAndroid@scottishtecharmy.support**, en indiquant la langue dans l’objet.


*French translation, round 3 — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Bonjour, et merci encore pour vos réponses à la deuxième série de questions.
Elles sont intégrées à l'application (voir « Ce que nous avons décidé »
ci-dessous). Il ne reste que deux petites questions. **Vous n'avez pas besoin
de connaître ni d'installer l'application :** pour chaque question, vous
trouverez ici quand le texte se fait entendre, ce qu'il dit en anglais et ce
que ça donne aujourd'hui en français.

## Qu'est-ce que Soundscape ?

Soundscape est une application gratuite pour les personnes aveugles et
malvoyantes. On l'utilise en marchant, avec un casque ou des écouteurs, le
téléphone souvent dans la poche. Elle ne guide pas pas à pas comme un GPS
(« tournez à gauche ») : elle dit à voix haute ce qui se trouve autour, pour que
la personne s'oriente elle-même.

Quelques notions qui reviennent dans les questions :

- **Annonce** *(callout)* : un court message parlé sur ce devant quoi on passe,
  par exemple « Boulangerie », « Trottoir à côté de la rue de la République » ou
  « En direction du nord le long de la rue de la République ». On l'entend en son
  3D, depuis la direction où se trouve l'endroit.
- **Balise sonore** *(audio beacon)* : quand on choisit une destination, un son
  régulier et répété se fait entendre dans les écouteurs depuis la direction de
  cette destination. Quand on se tourne, le son « se déplace », si bien qu'on peut
  marcher vers la destination à l'oreille.
- **Marqueur** et **itinéraire** *(marker, route)* : des lieux enregistrés, et une
  suite de ces lieux (les **étapes**) que la balise sonore fait parcourir une à
  une.

Les personnes aveugles utilisent leur téléphone avec un **lecteur d'écran**
(TalkBack sur Android, VoiceOver sur iPhone) : chaque bouton et chaque texte est
lu par une voix de synthèse. Les textes de l'application sont donc presque
toujours *entendus*, pas lus, et souvent dans le bruit de la rue.

Ces notions sont décrites plus en détail (en anglais)
[sur cette page]({{ "/developers/translation-terminology.html" | relative_url }}).

## Ce que nous avons décidé

- **Annonce** remplace « notification » partout.
- **Étape** pour les points d'un itinéraire (« Étape suivante »), sans « de
  parcours ». « Repères » reste réservé aux points de repère.
- **Croisement** remplace « intersection ».
- **Vous** est conservé.
- Les **articles devant les noms de lieux** restent comme ils sont.
- **Balise sonore** à la première mention, dans les titres et quand rien
  d'autre n'indique qu'il s'agit d'un son. Ensuite, et avec une action
  (« Désactiver le son de la balise », « Démarrer la balise »), simplement
  **balise**.
- Pause : **« Mettre en pause »**, **« En pause »**, **« Reprendre maintenant »** ;
  le second mode : **« En pause jusqu'au départ »**, **« Reprendre au
  déplacement »**.
- Niveaux de détail : **Détaillé / Simplifié / Minimal / Silencieux**.
- En voiture ou en bus : **« Vous vous déplacez vers le nord »** (« Vous marchez »
  ne convenait pas, car on ne marche pas en voiture).
- Voies sans issue : **« Sentier menant à une impasse »**.

## Comment répondre

Répondez simplement par e-mail en citant les numéros (« Q1 : je dirais
plutôt… »). Inutile de répondre à tout, et un simple « OK » nous aide aussi.

---

## Q1 — « Balise sonore » ou « balise audio » ? *(Audio beacon, first mention)*

**Quand on l'entend :** dans les réglages et au début des textes d'aide, là où
la balise est nommée pour la première fois.

**En anglais :** « Audio Beacon », « No beacon active ».

**Ce que ça donne aujourd'hui :** « Balise sonore », « Aucune balise sonore
active ».

**Ce qui nous fait hésiter :** vous avez écrit qu'il faut dire au moins une fois
« balise audio » en introduction. L'application dit « balise sonore » depuis le
début (c'est le terme de la version d'origine).

**La question :** « balise sonore » vous convient-il pour cette première
mention, ou préférez-vous « balise audio » ?

## Q2 — « Minimal » *(Detail level 3)*

**Quand on l'entend :** dans les réglages, où l'on choisit à l'oreille ce que
l'application dit en chemin.

**En anglais :** « Detailed / Simplified / Essential / Silent ».

**Ce que ça donne aujourd'hui :** Détaillé / Simplifié / Minimal / Silencieux.

**Ce qui nous fait hésiter :** nous avons suivi votre proposition. Le niveau
« Minimal » annonce seulement les rues, les croisements et les repères.

**La question :** l'ordre des quatre niveaux est-il maintenant clair à
l'oreille ?

## Q3 — Autre chose ? *(Anything else)*

Y a-t-il des phrases qui sonnent comme une traduction de l'anglais, qui sont trop
longues ou pas claires ? Tout commentaire est bienvenu, même sans numéro.

---

Merci beaucoup pour votre aide !
