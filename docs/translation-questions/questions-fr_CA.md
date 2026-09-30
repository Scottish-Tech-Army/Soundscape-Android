---
title: "Français (Canada)"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fr_CA/
---

# Traduction québécoise de Soundscape — quelques questions

> Envoyez vos réponses par courriel à **soundscapeAndroid@scottishtecharmy.support**, en indiquant la langue dans l’objet.


*Canadian French translation — questions for native-speaker reviewers.
English glosses in italics are for the maintainer.*

Bonjour, et merci d'avoir accepté d'y jeter un coup d'œil.

La traduction canadienne vient en partie de l'application originale de
Microsoft, mais beaucoup de textes sont nouveaux et n'ont jamais été relus par
une personne dont c'est la langue maternelle. **Vous n'avez pas besoin de
connaître ni d'installer l'application :** pour chaque question, vous trouverez
ici quand le texte se fait entendre, ce qu'il dit en anglais et comment il
sonne en français à l'heure actuelle.

## Qu'est-ce que Soundscape?

Soundscape est une application gratuite pour les personnes aveugles et
malvoyantes. On l'utilise en marchant, avec des écouteurs, le cellulaire
souvent dans la poche. Elle ne donne pas d'itinéraire pas à pas comme un GPS
(« tournez à gauche ») : elle dit à voix haute ce qui se trouve autour, pour que
la personne s'oriente elle-même.

Quelques notions qui reviennent dans les questions :

- **Annonce** *(callout)* : un court message parlé sur ce devant quoi on
  passe, par exemple « Café », « Trottoir à côté de la rue Principale » ou « En
  direction du nord le long de la rue Principale ». On l'entend en son 3D, depuis
  la direction où se trouve l'endroit.
- **Balise sonore** *(audio beacon)* : quand on choisit une destination, un son
  régulier et répété se fait entendre dans les écouteurs depuis la direction de
  cette destination. Quand on se tourne, le son « se déplace », si bien qu'on peut
  marcher vers la destination à l'oreille.
- **Marqueur** et **itinéraire** *(marker, route)* : des lieux enregistrés, et une
  suite de ces lieux que la balise fait parcourir un à un.

Les personnes aveugles utilisent leur téléphone avec un **lecteur d'écran**
(TalkBack sur Android, VoiceOver sur iPhone) : chaque bouton et chaque texte est
lu par une voix de synthèse. Les textes de l'application sont donc presque
toujours *entendus*, pas lus, et souvent dans le bruit de la rue. Le plus
important, c'est qu'ils soient courts, clairs et naturels à l'écoute.

Ces notions sont décrites plus en détail (en anglais)
[sur cette page]({{ "/developers/translation-terminology.html" | relative_url }}).

## Comment répondre

Répondez simplement par courriel en citant le numéro de la question (« Q1 : je
dirais… »). Pas besoin de tout couvrir. Si quelque chose est déjà bien, un « OK »
aide aussi.

---

### Q1 — « Annonce » *(Callout, changed from «notification»)*

**Quand on l'entend :** c'est le nom des courts messages parlés décrits
plus haut. Il apparaît surtout dans les réglages, par exemple
« Annonces automatiques ».

**En anglais :** « Callout », « Automatic Callouts ».

**Ce que dit l'application :** « Annonce », « Annonces automatiques ». Avant,
c'était « Notification ».

**Ce qui nous fait hésiter :** une personne de langue maternelle française
(de France) a choisi « annonce », parce que « notification » désigne aussi les
notifications du téléphone. Nous l'avons repris pour le Canada, ainsi que
« étape » pour les points d'un itinéraire et « croisement » pour
« intersection ».

**La question :** ces mots conviennent-ils au Québec, ou diriez-vous autre chose?

### Q2 — « Balise sonore » *(Audio Beacon)*

**Quand on l'entend :** sur les boutons, dans les réglages et dans la visite
guidée, par exemple « Vous pouvez maintenant entendre la balise sonore. Elle est
émise depuis la direction de votre destination. »

**En anglais :** « Audio Beacon ».

**Ce que dit l'application :** « Balise sonore ».

**Ce qui nous fait hésiter :** le terme vient d'une traduction automatique; nous
ne savons pas s'il évoque naturellement un son qui indique une direction.

**La question :** est-ce naturel? Sinon, que diriez-vous?

### Q3 — « Sentier vers une impasse » *(Dead-end way description)*

**Quand on l'entend :** en marchant, quand on passe devant un embranchement.
L'application dit où mène ce chemin.

**En anglais :** « Path to Moor Road », « Path to dead end ».

**Ce que dit l'application :** « Sentier vers Moor Road », « Sentier vers
une impasse ».

**Ce qui nous fait hésiter :** en France, on a retenu « Sentier menant à une
impasse », et « sans issue » a été jugé un peu anxiogène.

**La question :** lequel préférez-vous ?

### Q4 — « Vous êtes prêt! » *(Gendered "you're ready")*

**Quand on l'entend :** à la fin de la configuration, au premier lancement.

**En anglais :** « You're ready! ».

**Ce que dit l'application :** « Vous êtes prêt! » (au masculin).

**Ce qui nous fait hésiter :** l'application ne connaît pas le genre de la
personne, et le masculin ne convient pas à tout le monde.

**La question :** « Tout est prêt! » serait-il mieux?

### Q5 — Les articles devant les noms de lieux *(Articles now added in code)*

**Quand on l'entend :** dans de nombreuses annonces en marchant.
L'application insère les noms tels qu'ils sont dans la carte.

**Ce que dit l'application :** elle ajoute maintenant l'article elle-même :
« Sur **la rue** Sainte-Catherine », « le long **du boulevard** Saint-Laurent »,
« près **de l’avenue** du Parc », « Trottoir à côté **du chemin** de la
Côte-des-Neiges ». Un nom qui a son propre article le contracte (« près **du**
Vieux-Port » pour « Le Vieux-Port »).

**Ce qui nous fait hésiter :** les règles viennent d'une liste de types de voies
faite surtout avec des noms de France ; des termes québécois (rang, montée, côte,
croissant) y sont, mais il en manque peut-être.

**La question :** est-ce que ça sonne juste au Québec ? Voyez-vous un type de voie
qui sort mal ?

### Q6 — Les quatre niveaux de détail *(Four detail levels, renamed 2026-09-30)*

**Quand on l'entend :** dans les réglages, où l'on choisit à l'oreille ce que l'application dit en chemin.

**En anglais :** « Detailed / Simplified / Essential / Silent »

**Ce que dit l'application :** Détaillé / Simplifié / Essentiel / Silencieux. Le Simplifié laisse de côté les petits chemins et se répète moins; l'Essentiel n'annonce que les rues, les intersections et les repères.

**Ce qui nous fait hésiter :** nous venons de renommer les deux niveaux du milieu. Avant, c'était « Équilibré » et « Discret », mais des locuteurs natifs d'autres langues trouvaient les anciens noms anglais peu clairs : « Quiet » faisait penser au volume. Les nouveaux noms ont été choisis par nous, pas par un locuteur natif du français canadien.

**La question :** se distinguent-ils bien à l'oreille? Nommeriez-vous autrement les deux du milieu?

### Q7 — Autre chose? *(Anything else)*

Si une phrase sonne comme une traduction de l'anglais, est trop longue ou peu
claire, dites-le-nous.

Merci beaucoup!
