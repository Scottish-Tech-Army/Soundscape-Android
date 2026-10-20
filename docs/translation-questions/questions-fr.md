---
title: "Français"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fr/
---

# Traduction française de Soundscape — quelques questions

> Envoyez vos réponses par e-mail à **soundscapeAndroid@scottishtecharmy.support**, en indiquant la langue dans l’objet.


*French translation, round 5 — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Bonjour, et merci encore pour vos réponses aux séries précédentes. Elles sont
intégrées à l'application (voir « Ce que nous avons décidé » ci-dessous). Il ne
reste que deux petites vérifications. **Vous n'avez pas besoin de connaître ni
d'installer l'application :** pour chaque question, vous trouverez ici quand le
texte se fait entendre, ce qu'il dit en anglais et ce que ça donne aujourd'hui en
français.

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
- **Étape** pour les points d'un itinéraire (« Étape suivante »). « Repères »
  reste réservé aux points de repère.
- **Intersection** (« l'intersection suivante », « Rues et intersections ») :
  c'est le terme général, qui couvre aussi les intersections en T.
- **Vous** est conservé.
- Les **articles devant les noms de lieux** restent comme ils sont.
- **Balise sonore**, jamais « balise audio ». Ensuite, et avec une action
  (« Désactiver le son de la balise », « Réactiver le son de la balise »),
  simplement **balise**.
- Pour créer une balise sur un lieu : **« Placer la balise sonore »**. Nous
  avons retiré « ici », car le même texte sert aussi de nom de menu dans les
  commandes des écouteurs, avant qu'aucun lieu ne soit choisi.
- Pause : **« Mettre en pause »**, **« En pause »**, **« Reprendre »** ; le
  second mode : **« En pause à l'arrêt »**, **« Reprendre au déplacement »**.
- Niveaux de détail : **Détaillé / Simplifié / Essentiel / Silencieux**.
- En voiture ou en bus : **« Vous vous déplacez vers le nord »**.
- Voies sans issue : **« Chemin sans issue »**.
- Les **points cardinaux** s'écrivent en minuscules : « nord », « nord-ouest ».

## Comment répondre

Répondez simplement par e-mail en citant les numéros (« Q1 : je dirais
plutôt… »). Inutile de répondre à tout, et un simple « OK » nous aide aussi.

---

## Q1 — « Placer la balise sonore » dans d'autres textes *(Start Audio Beacon, derived forms)*

**Quand on l'entend :** dans la liste des marqueurs, dans les commandes vocales,
et sur le dernier écran de la première utilisation.

**En anglais :** « Start audio beacon at this marker », « Start audio beacon at
%1$s » (%1$s est le nom d'un marqueur), « …choose a nearby destination and
start the audio beacon — you’ll hear it playing in the direction of your
destination. »

**Ce que ça donne aujourd'hui :** « Placer la balise sonore sur ce marqueur »,
« Placer la balise sonore sur Maison », « …choisissez une destination proche et
placez-y la balise sonore : vous l'entendrez retentir dans la direction de
votre destination. »

**Ce qui nous fait hésiter :** vous avez choisi « Placer la balise sonore »
pour le bouton principal. Ces trois textes, nous les avons adaptés nous-mêmes.

**La question :** sonnent-ils naturellement, en particulier « sur ce
marqueur » et « placez-y » ?

## Q2 — « Chemin sans issue via Marches » *(Dead end, with what the way passes)*

**Quand on l'entend :** en marchant, pour un chemin sans nom qui ne mène nulle part
et passe par des marches, un pont ou un tunnel.

**En anglais :** « Path to dead end via Steps ».

**Ce que ça donne aujourd'hui :** « Chemin sans issue via Marches » (ou « via
Pont », « via Tunnel »).

**Ce qui nous fait hésiter :** la forme courte « Chemin sans issue » est la
vôtre. Pour la forme longue, nous avons simplement ajouté « via … » à la fin,
et le mot ajouté garde sa majuscule.

**La question :** est-ce compréhensible ainsi ? Faudrait-il plutôt « Chemin sans
issue, par des marches », ou une autre tournure ?

## Q3 — Autre chose ? *(Anything else)*

Y a-t-il des phrases qui sonnent comme une traduction de l'anglais, qui sont trop
longues ou pas claires ? Tout commentaire est bienvenu, même sans numéro.

---

Merci beaucoup pour votre aide !
