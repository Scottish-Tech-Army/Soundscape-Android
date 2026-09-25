---
title: "Français"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fr/
---

# Traduction française de Soundscape — quelques questions

> Envoyez vos réponses par e-mail à **soundscapeAndroid@scottishtecharmy.support**, en indiquant la langue dans l’objet.


*French translation — questions for native-speaker reviewers. English glosses
in italics are for the maintainer.*

Bonjour, et merci d'avoir accepté d'y jeter un œil.

La traduction française a été faite sans l'aide d'un locuteur natif. Votre avis
compte donc beaucoup pour nous. **Vous n'avez pas besoin de connaître ni
d'installer l'application :** pour chaque question, vous trouverez ici quand le
texte se fait entendre, ce qu'il dit en anglais et comment il sonne aujourd'hui
en français.

Nous n'attendons pas une relecture complète. Ci-dessous se trouvent **douze
questions** sur les choix qui touchent le plus de textes : un seul mot mal
choisi peut se retrouver dans des dizaines de phrases.

## Qu'est-ce que Soundscape ?

Soundscape est une application gratuite pour les personnes aveugles et
malvoyantes. On l'utilise en marchant, avec un casque ou des écouteurs, le
téléphone souvent dans la poche. Elle ne guide pas pas à pas comme un GPS
(« tournez à gauche ») : elle dit à voix haute ce qui se trouve autour, pour que
la personne s'oriente elle-même.

Quelques notions qui reviennent dans les questions :

- **Notification** *(callout)* : un court message parlé sur ce devant quoi on
  passe, par exemple « Boulangerie », « Trottoir à côté de la rue de la
  République » ou « En direction du nord le long de la rue de la République ».
  On l'entend en son 3D, depuis la direction où se trouve l'endroit.
- **Balise sonore** *(audio beacon)* : quand on choisit une destination, un son
  régulier et répété se fait entendre dans les écouteurs depuis la direction de
  cette destination. Quand on se tourne, le son « se déplace », si bien qu'on peut
  marcher vers la destination à l'oreille.
- **Marqueur** et **itinéraire** *(marker, route)* : des lieux enregistrés, et une
  suite de ces lieux que la balise fait parcourir un à un.

Les personnes aveugles utilisent leur téléphone avec un **lecteur d'écran**
(TalkBack sur Android, VoiceOver sur iPhone) : chaque bouton et chaque texte est
lu par une voix de synthèse. Les textes de l'application sont donc presque
toujours *entendus*, pas lus, et souvent dans le bruit de la rue. Ce qui compte,
c'est qu'ils soient courts, clairs et naturels *à l'oreille*.

Ces notions sont décrites plus en détail (en anglais)
[sur cette page]({{ "/developers/translation-terminology.html" | relative_url }}).

## Comment répondre

Répondez simplement par e-mail en citant les numéros (« Q2 : je dirais
plutôt… »). Inutile de répondre à tout : même deux ou trois réponses nous aident
beaucoup. Et si un choix actuel vous convient, dites-le aussi (« Q6 : OK »),
comme ça nous savons qu'il ne faut pas y toucher.

---

## Q1 — « Notification » ou « annonce » ?

**Quand on l'entend :** c'est le nom des courts messages parlés décrits plus
haut, le cœur de l'application. Il apparaît surtout dans les réglages.

**En anglais :** *Callout*, « Automatic Callouts ».

**Ce que dit l'application :** surtout **« notification »** (« Notifications
automatiques », « Autoriser les notifications », « Gérer les notifications »).
Certains textes disent déjà **« annonce »** (« Lieux à annoncer »).

**Ce qui nous fait hésiter :** sur un téléphone, une « notification » désigne
d'habitude un message du système, pas une description parlée des environs.

**La question :** lequel vous semble le plus juste ? Ou autre chose ?

## Q2 — Étapes d'un itinéraire et points de repère

**Quand on l'entend :** en suivant un itinéraire. Quand vous arrivez à l'un de
ses points, l'application vous le signale et passe au suivant.

**En anglais :** *Waypoint*, « Next waypoint ».

**Ce que dit l'application :** **« points de repère »** (« Prochain point de
repère »). Mais elle utilise aussi **« repères »** pour tout autre chose, les
*landmarks* (parcs, églises, monuments), dans le réglage « Lieux et repères ».

**Ce qui nous fait hésiter :** les deux notions risquent de se confondre.

**La question :** est-ce que ça prête à confusion ? Comment votre application de
navigation (Google Maps, Apple Plans…) appelle-t-elle un arrêt intermédiaire sur
un trajet : « étape », « point de passage » ?

## Q3 — Les modes « veille »

**Quand on l'entend :** sur un bouton de l'écran d'accueil, et quand
l'application annonce dans quel mode elle est. Elle a deux façons de
s'interrompre :

- **Sleep** : elle s'arrête complètement jusqu'à ce que vous la réveilliez.
- **Snooze** : elle se met en pause et **se réveille toute seule** quand vous
  quittez l'endroit où vous êtes (par exemple chez vous).

**Ce que dit l'application :** « Mettre en veille » / « En veille » pour le
premier ; « Désactivé temporairement » pour le second. La FAQ parle du « mode
Mettre en veille » et du « mode Désactiver temporairement ».

**Ce qui nous fait hésiter :** « Désactivé temporairement » ne dit pas que
l'application se réveillera seule, et les deux noms ne forment pas une paire.

**La question :** comment nommeriez-vous ces deux modes pour qu'ils soient
naturels et bien distincts ?

## Q4 — « Vous » ou « tu » ?

**Quand on l'entend :** dans toute l'application.

**Ce que dit l'application :** elle vouvoie l'utilisateur (« Appuyez sur… »,
« votre itinéraire »).

**La question :** ça vous convient pour ce type d'application, ou le tutoiement
serait-il plus naturel ?

## Q5 — Les noms de lieux après « de » et « à »

**Quand on l'entend :** dans de nombreuses annonces en marchant.
L'application insère les noms de lieux tels qu'ils sont dans la carte.

**Ce que dit l'application :** elle ne sait pas faire les contractions, ce qui
donne par exemple :

- « Vous approchez de **Le Bon Marché** » (au lieu de « du Bon Marché »)
- « À proximité de **Les Halles** » (au lieu de « des Halles »)

**Ce qui nous fait hésiter :** le corriger demande du travail de programmation ;
nous voulons savoir si ça en vaut la peine.

**La question :** à l'écoute, est-ce gênant au point qu'il faudrait le corriger,
ou est-ce que ça passe ?

## Q6 — « Intersection » ou « carrefour » ?

**Quand on l'entend :** très souvent en marchant, à l'approche d'un croisement.

**Ce que dit l'application :** « Vous approchez d'une intersection »,
« Intersection à 20 mètres ».

**La question :** est-ce le mot que vous utiliseriez à pied, dans la rue ?

## Q7 — « Se déplaçant vers le nord »

**Quand on l'entend :** très souvent, pour dire dans quelle direction on avance.

**En anglais :** « Traveling north », « Traveling east along X Street ».

**Ce que dit l'application :** « Se déplaçant vers le nord », « Se déplaçant
vers l'est le long de la rue X ».

**Ce qui nous fait hésiter :** le participe seul sonne comme une traduction.

**La question :** est-ce naturel ? Sinon, comment le diriez-vous (« Vous avancez
vers le nord » ? « Direction nord » ?)

## Q8 — Les voies sans issue

**Quand on l'entend :** en marchant, quand on passe devant un embranchement.
L'application dit où mène ce chemin.

**En anglais :** « Path to Moor Road », « Path to dead end ».

**Ce que dit l'application :** « Chemin à Moor Road », « Chemin à impasse ».

**Ce qui nous fait hésiter :** nous pensons que ce n'est pas correct.

**La question :** laquelle de ces formulations préférez-vous ?

- « Chemin menant à une impasse »
- « Chemin sans issue »
- autre chose ?

## Q9 — Les quatre niveaux de détail

**Quand on l'entend :** dans les réglages, où l'on choisit à l'oreille, souvent
en marchant, combien l'application en dit.

**En anglais :** « Detailed / Balanced / Quiet / Silent ».

**Ce que dit l'application :** **Détaillé / Équilibré / Discret / Silencieux**.
Détaillé annonce tout ce qui est proche ; Équilibré laisse de côté les petits
chemins et se répète moins ; Discret n'annonce que les rues, les carrefours et
les repères ; Silencieux ne fait aucune annonce automatique.

**La question :** les quatre sont-ils faciles à distinguer et à comprendre quand
on les entend ?

## Q10 — Les commandes Siri (iPhone seulement)

**Quand on l'entend :** jamais ; ces phrases, c'est vous qui les *dites*. Sur
iPhone, on peut piloter Soundscape avec Siri sans toucher le téléphone.

**Ce que dit l'application :**

- « Soundscape **environs** », puis « Autour de moi », « Devant moi »…
- « Soundscape **itinéraire** », puis « Point de repère suivant », « Arrêter »…
- « Soundscape **démarre l'itinéraire** … »
- « Soundscape **balise** … » / « Soundscape **arrête la balise** »
- « Soundscape **liste** … »
- « Soundscape **détails** … »

**La question :** ces phrases vous viendraient-elles naturellement ? Y a-t-il des
mots que vous diriez autrement ?

## Q11 — « Balise sonore »

**Quand on l'entend :** sur les boutons, dans les réglages et dans la visite
guidée, par exemple « Vous pouvez maintenant entendre la balise sonore. Elle est
émise depuis la direction de votre destination. »

**En anglais :** « Audio Beacon ».

**Ce que dit l'application :** « Balise sonore ».

**Ce qui nous fait hésiter :** le terme vient d'une traduction automatique ;
nous ne savons pas s'il évoque naturellement un son qui indique une direction.

**La question :** est-ce naturel ? Sinon, que diriez-vous ?

## Q12 — Autre chose ?

Y a-t-il des phrases qui sonnent « traduit de l'anglais », qui sont trop longues,
ou peu claires ? Tout retour est le bienvenu, même sans numéro.

---

Merci beaucoup pour votre aide !
