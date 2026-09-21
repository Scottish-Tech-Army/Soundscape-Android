---
title: Notes de version
layout: page
nav_order: 5
has_toc: false
lang: fr
permalink: /release-notes.html
machine-translated: true
---

# Notes de version

Soundscape 2.0 est une version importante, actuellement en bêta fermée. Le changement principal est
que Soundscape a désormais quelque chose d'utile à dire lorsque vous voyagez en voiture, en bus ou
en train, et plus seulement lorsque vous marchez. S'y ajoutent de nombreux travaux plus modestes sur
la description des lieux, vingt nouvelles langues et une longue liste de corrections.

Les notes des versions précédentes se trouvent sur la page
[Notes de version pour la 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nouveautés de la version 2.0

* **Annonces pendant les trajets en voiture, en bus ou en train.** Soundscape détecte que vous vous
  déplacez à vitesse élevée et décrit votre trajet plutôt que votre environnement immédiat.
* **Annonce des cours d'eau et des voies ferrées franchis.** Rivières, canaux, estuaires et lignes
  de chemin de fer sont annoncés lorsque vous les traversez, à pied comme en déplacement.
* **De meilleures adresses et de meilleurs noms de lieux.** Les lieux sans adresse propre reçoivent
  désormais la rue et le quartier où ils se trouvent, les numéros de rue sont rattachés au bon côté
  de la chaussée, et les arrêts de bus en Grande-Bretagne utilisent leur nom officiel.
* **Vingt nouvelles langues**, portant le total à 46. Ce site de documentation est également
  traduit.
* **Réveil au départ.** Le mode veille peut désormais réveiller Soundscape lorsque vous quittez
  l'endroit où vous l'avez mis en veille.
* **Des distances plus courtes et plus naturelles**, avec des unités plus grandes lorsque vous vous
  déplacez rapidement.
* **Une sortie plus rapide.** *Quitter Soundscape* figure désormais en haut du menu principal.
* **Améliorations des cartes hors ligne**, notamment la mise à jour d'une carte déjà téléchargée et
  une carte des régions disponibles sur ce site.
* **De nombreux travaux d'accessibilité** sur TalkBack, en particulier autour des écrans de
  démarrage.
* **Un très grand nombre de corrections de plantages et de stabilité.**

Deux éléments ont été **supprimés** dans la version 2.0 : la commande vocale et le menu de langue
dans l'application. Voyez [Éléments supprimés](#things-that-have-been-removed) ci-dessous pour
savoir quoi faire à la place.

---

## Plus en détail

### Voyager en voiture, en bus ou en train

Il s'agit de la principale nouveauté pour les personnes qui utilisent déjà l'application. Auparavant,
Soundscape n'avait presque rien à dire une fois que vous montiez dans un véhicule : il continuait à
décrire votre environnement immédiat, ce qui, à vitesse élevée, se traduisait par un flot de choses
déjà dépassées.

Soundscape remarque désormais que vous vous déplacez plus vite qu'au pas et modifie ce qu'il vous
annonce. Il n'y a rien à activer, et tout revient à la normale de lui-même dès que vous ralentissez
ou que vous descendez pour marcher.

Pendant le trajet, vous entendrez :

* **Où vous êtes**, de temps à autre : la route sur laquelle vous circulez et votre direction, par
  exemple « En direction du nord sur la M8 ». Les routes numérotées sont annoncées par leur numéro,
  et Soundscape ne réannonce pas la même route à chaque changement de nom de rue.
* **Les villes et les villages** vers lesquels vous vous dirigez, avec la distance, ainsi que ceux
  dont vous vous éloignez ou devant lesquels vous passez simplement.
* **Les échangeurs et les sorties d'autoroute** au moment où vous les atteignez.
* **Les grands points de repère** devant lesquels vous passez : parcs, hôpitaux, stades et centres
  commerciaux.
* **Les arrêts de bus, de tram et les gares** devant lesquels vous passez. Soundscape ne mentionne
  que les arrêts situés de votre côté de la route, ceux d'en face desservant le sens inverse.
* **Les rivières, canaux et voies ferrées que vous franchissez.**
* **Les tunnels**, ce qui explique surtout pourquoi Soundscape va se taire : il n'y a pas de signal
  GPS à l'intérieur.

Dans un **train**, Soundscape comprend que vous êtes sur une voie ferrée et non sur une route, et
vous indique les localités devant lesquelles vous passez ainsi que la distance parcourue depuis la
dernière gare. C'est plus difficile qu'il n'y paraît, car autoroutes et voies ferrées sont souvent
construites côte à côte sur des kilomètres ; une bonne part du travail de cette version a donc
consisté à ne pas confondre l'une avec l'autre.

Les annonces habituelles pour la marche – commerces à proximité, traversées de rues, etc. – sont
volontairement mises en retrait pendant le trajet, et les distances auxquelles les éléments sont
annoncés sont nettement allongées afin que vous en soyez informé avant de les avoir dépassés.

### Franchissement des cours d'eau et des voies ferrées

Soundscape vous indique désormais lorsque vous franchissez une rivière, un canal, un estuaire, une
baie ou une ligne de chemin de fer. Cela fonctionne à pied comme en déplacement, et couvre aussi
bien le passage en dessous qu'au-dessus : une passerelle et un passage souterrain sont donc tous
deux décrits.

### De meilleures adresses et de meilleurs noms de lieux

Beaucoup de travail a été consacré à ce que Soundscape décrive les lieux comme le ferait une
personne :

* Les lieux sans adresse propre sont désormais décrits par la rue et le quartier où ils se trouvent,
  au lieu de rester vagues.
* Les numéros de rue sont rattachés au bon côté de la chaussée. Auparavant, une adresse pouvait être
  annoncée depuis le trottoir d'en face.
* L'adresse d'un lieu ne répète plus le nom du lieu lui-même.
* Les arrêts de bus en Grande-Bretagne utilisent leur nom officiel de transport public, en général
  celui qui figure sur les horaires et sur le panneau de l'arrêt.
* Les sentiers sans nom qui longent une rivière ou un canal portent désormais le nom du cours d'eau
  qu'ils suivent.
* Les chemins et les routes sans nom sont décrits de façon plus sensée, et les mots employés sont
  correctement traduits au lieu d'apparaître en anglais.

### Langues

Vingt nouvelles langues ont été ajoutées dans la version 2.0 : arabe, bengali, bulgare, catalan,
croate, tchèque, haoussa, hongrois, indonésien, coréen, marathi, serbe, slovaque, slovène, swahili,
tamoul, télougou, thaï, ourdou et vietnamien. Ces langues sont toutes au stade alpha et nous
souhaitons vivement recevoir des retours sur leur exactitude. Au total, Soundscape est désormais
disponible en 46 langues, et ce site de documentation a également été traduit.

L'arabe égyptien a été fusionné avec l'arabe, et le luganda a été retiré : ni l'un ni l'autre ne
disposait d'assez de texte traduit pour être utile.

Les traductions sont un travail collectif et nous accueillons volontiers votre aide, ou vos
corrections lorsqu'une formulation se lit mal. Chaque chaîne peut être améliorée sur
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Mode veille

Le mode veille dispose désormais du **réveil au départ**. Lorsque vous mettez Soundscape en veille,
vous pouvez lui demander de se réveiller dès que vous quittez les lieux, ce qui est utile quand vous
arrivez quelque part et souhaitez le silence jusqu'à votre prochain départ.

### Distances et parole

Les distances énoncées ont été raccourcies et rendues plus naturelles, et Soundscape passe désormais
à des unités plus grandes lorsque vous vous déplacez vite : des miles ou des kilomètres plutôt qu'un
long décompte en pieds ou en mètres. Chaque langue décide elle-même de la façon d'exprimer une
distance fractionnaire, ce qui était auparavant contraint dans un moule anglais.

### Cartes hors ligne

Les cartes hors ligne sont arrivées avec la version 1.0 et n'ont cessé d'être améliorées :

* Une carte téléchargée peut désormais être mise à jour sur place lorsqu'une version plus récente
  est disponible, depuis l'écran de détail de l'extrait.
* Les cartes inutilisables – un téléchargement corrompu, par exemple – sont désormais clairement
  signalées au lieu d'échouer en silence.
* Les téléchargements sont plus fiables, et l'écran indique ce qui se passe pendant la récupération
  de la liste des cartes disponibles, au lieu d'un indicateur de chargement plein écran.
* Un téléchargement terminé n'apparaît comme terminé qu'une fois réellement prêt à l'emploi.
* Une [carte des régions disponibles]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  est proposée sur ce site.

### Accessibilité

Un travail considérable a été mené sur le comportement des lecteurs d'écran, en particulier dans les
écrans de démarrage où le focus se plaçait auparavant au mauvais endroit. Parmi les autres
améliorations : une meilleure lecture des tailles de fichiers et des nombres décimaux, des
indications « appuyez deux fois pour... » correctes dans les langues où le verbe est en fin de
phrase, et des indications pertinentes là où il n'y en avait aucune.

### Menus et navigation

* **Quitter Soundscape** est désormais le premier élément du menu principal au lieu de figurer plus
  bas.
* Le menu principal ne laisse plus apparaître une bande de l'écran sur le côté, qui offrait aux
  personnes utilisant un lecteur d'écran une zone supplémentaire déroutante.
* Le geste de retour du système ne saute plus un niveau lorsque vous parcourez les catégories dans
  Lieux à proximité.
* Le *tutoriel audio* a été renommé **tutoriel guidé**.
* Les réglages ont été réorganisés, et *Réinitialiser les valeurs par défaut* efface désormais
  correctement tout.

### Stabilité

La version 2.0 comprend une longue liste de corrections de plantages et de blocages : blocage de
l'application sur l'écran de démarrage, blocages lors de la réinitialisation des réglages, plantages
en cas de carte téléchargée endommagée, plantages à l'ouverture des détails d'un itinéraire depuis
l'écran d'accueil, plantages lors du changement de langue, ainsi que plusieurs problèmes signalés
automatiquement via le Play Store. Le comportement au démarrage et vis-à-vis de la batterie a aussi
été rendu plus robuste sur les téléphones qui ferment agressivement les applications en arrière-plan.

### Éléments supprimés
{: #things-that-have-been-removed }

* **La commande vocale** a été supprimée. Elle n'a jamais fonctionné de façon assez fiable pour être
  conservée, et les boutons de contrôle multimédia des écouteurs couvrent l'essentiel du même
  besoin – voyez
  [Aide sur l'utilisation des commandes multimédias]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Le menu de langue dans l'application** a disparu. Soundscape suit désormais la langue définie
  sur votre téléphone, ce que la plupart des gens attendaient. Pour la changer, modifiez la langue
  de votre téléphone ou définissez une langue par application dans ses réglages, si cette option est
  proposée.

## Nous signaler un problème

Si quelque chose ne va pas, nous aimerions le savoir. Écrivez au Help Desk à l'adresse
<soundscapeAndroid@scottishtecharmy.support>, ou demandez sur Slack si vous êtes membre de la STA.

Si une annonce était erronée ou n'a pas eu lieu, un enregistrement de votre trajet nous aide
énormément : nous pouvons le rejouer et voir exactement sur quoi Soundscape s'appuyait. Les
instructions figurent sous
[Fournir un relevé de position pour le débogage]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Une note à propos de l'iPhone

Tout ce qui précède concerne l'application Android, mais il est utile de savoir où est passé le
reste du travail de cette version. Soundscape fonctionne désormais aussi sur iPhone, et les deux
applications sont construites à partir du même code partagé : mêmes écrans, mêmes formulations et
mêmes annonces. Une nouveauté comme les annonces de trajet décrites plus haut arrive donc sur les
deux à la fois, au lieu d'être écrite deux fois. C'est cette base commune qui explique la durée de
développement de la version 2.0, et c'est elle qui devrait permettre aux prochaines versions
d'arriver plus vite sur les deux plateformes. L'application iPhone est actuellement disponible via
TestFlight sur invitation : demandez sur Slack si vous êtes membre de la STA, ou écrivez au Help
Desk.
