---
title: "Français"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fr/
---

# Traduction française de Soundscape — quelques questions

> Envoyez vos réponses par e-mail à **soundscapeAndroid@scottishtecharmy.support**, en indiquant la langue dans l’objet.


*French translation, round 2 — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Bonjour, et encore merci pour vos réponses à la première série de questions.
Elles sont toutes intégrées à l'application (voir « Ce que nous avons décidé »
ci-dessous). Il reste quelques petites questions nées de ces changements. **Vous
n'avez pas besoin de connaître ni d'installer l'application :** pour chaque
question, vous trouverez ici quand le texte se fait entendre, ce qu'il dit en
anglais et comment il sonne aujourd'hui en français.

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
- **Étape** pour les points d'un itinéraire (« Étape suivante »). « Repères » reste
  réservé aux points de repère.
- **Croisement** remplace « intersection ».
- **Vous** est conservé.
- Les **articles devant les noms de lieux** restent comme ils sont.
- **Balise sonore** est conservé, mais toujours en entier : jamais « balise » seul.
- En voiture ou en bus : **« Vous vous déplacez vers le nord »** (« Vous marchez »
  ne convenait pas, car on ne marche pas en voiture).
- Voies sans issue : **« Sentier menant à une impasse »**.

## Comment répondre

Répondez simplement par e-mail en citant les numéros (« Q2 : je dirais
plutôt… »). Inutile de répondre à tout, et un simple « OK » nous aide aussi.

---

## Q1 — « Étape » *(Waypoint)*

**Quand on l'entend :** en suivant un itinéraire, et sur les boutons pour le
modifier.

**En anglais :** « Next Waypoint », « Add Waypoints ».

**Comment ça sonne aujourd'hui :** « Étape suivante », « Ajouter des étapes ».

**Ce qui nous fait hésiter :** vous nous aviez proposé « étape de parcours ».
Nous avons gardé seulement « étape », plus court, car on l'entend souvent.

**La question :** « étape » seul est-il assez clair, ou faut-il « étape de
parcours » ?

## Q2 — Les deux modes de pause *(Sleep and Snooze)*

**Quand on l'entend :** sur le bouton de l'écran d'accueil et quand
l'application dit dans quel état elle est.

**En anglais :** « Sleep », « Snoozing », « Wake On Leave », « Wake Up Now ».

**Comment ça sonne aujourd'hui :** « Mettre en pause », « En pause » ; « En pause
jusqu'au départ », « Reprendre quand je pars » ; « Reprendre maintenant ».

**Ce qui nous fait hésiter :** vous proposiez « Désactiver » et « Suspendre
jusqu'au prochain lieu ». Nous avons adapté : « Désactiver » ressemble à
n'importe quel interrupteur des réglages, et le second mode se réveille quand on
**quitte** l'endroit où l'on est, pas quand on arrive au suivant.

**La question :** ces libellés disent-ils clairement ce que fait chaque mode ?

## Q3 — « Balise sonore » en entier *(Audio beacon, always in full)*

**Quand on l'entend :** très souvent : sur les boutons, dans les réglages et dans
l'aide.

**En anglais :** « Mute Beacon », « Beacon is currently 105 metres away ».

**Comment ça sonne aujourd'hui :** « Désactiver le son de la balise sonore »,
« La balise sonore se trouve actuellement à 105 mètres ».

**Ce qui nous fait hésiter :** « repère sonore » aurait créé une confusion avec
les « repères » (les lieux connus), donc nous avons gardé « balise sonore ». Mais
toujours en entier, puisque « balise » seul vous faisait penser à une balise de
détresse. Dans les longs textes d'aide, cela fait beaucoup de répétitions.

**La question :** est-ce trop lourd ? Peut-on dire simplement « la balise » une
fois qu'elle a été nommée ?

## Q4 — Les quatre niveaux de détail *(Four detail levels, renamed)*

**Quand on l'entend :** dans les réglages, où l'on choisit à l'oreille ce que
l'application dit en chemin.

**En anglais :** « Detailed / Simplified / Essential / Silent ». Les noms anglais
ont changé, car des personnes de plusieurs pays avaient du mal avec les
anciens.

**Comment ça sonne aujourd'hui :** Détaillé / Simplifié / Essentiel / Silencieux.

**Ce qui nous fait hésiter :** vous aviez proposé « Synthétique » et « Simplifié ».
Avec les nouveaux noms anglais, « Simplifié » est remonté d'un niveau et le
troisième niveau s'appelle « Essentiel ».

**La question :** cet ensemble vous convient-il ? « Essentiel » se distingue-t-il
bien de « Silencieux » à l'oreille ?

## Q5 — Autre chose ? *(Anything else)*

Y a-t-il des phrases qui sonnent comme une traduction de l'anglais, qui sont trop
longues ou pas claires ? Tout commentaire est bienvenu, même sans numéro.

---

Merci beaucoup pour votre aide !
