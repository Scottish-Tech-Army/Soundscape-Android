---
title: Comment fonctionne Soundscape
layout: page
parent: Using Soundscape
has_toc: false
lang: fr
permalink: /users/how-it-works.html
machine-translated: true
---
# Comment fonctionne Soundscape
L'objectif de cette page est de donner une compréhension générale du fonctionnement interne de l'application Soundscape. Vous n'avez pas besoin de lire ceci pour utiliser l'application, mais elle a été rédigée pour plusieurs raisons :

1. Aider les nouveaux venus intéressés par l'application à comprendre où se situent ses limites
1. Donner aux utilisateurs une idée de ce qui pourrait encore être possible avec de nouvelles fonctionnalités
1. Donner aux développeurs un aperçu du fonctionnement de l'application

Deux technologies rendent l'application possible : le GPS et les données OpenStreetMap. Le GPS nous donne une bonne idée de l'endroit où se trouve le téléphone, et de l'endroit où il est passé. Les données OpenStreetMap peuvent ensuite être utilisées pour trouver ce qui se trouve à proximité, et nous pouvons les utiliser pour le décrire à l'utilisateur.

## Balises sonores
À bien des égards, ce sont les éléments les plus simples à implémenter d'un point de vue technologique. En supposant que nous disposions de l'emplacement du téléphone et d'une direction pour le téléphone, nous pouvons alors modifier l'audio de la balise pour qu'elle semble provenir de cette direction. Nous utilisons une bibliothèque de Steam Audio pour effectuer le positionnement audio, qui utilise des fonctions de transfert relatives à la tête (HRTF) pour offrir le meilleur positionnement sonore possible. La seule autre chose que nous faisons consiste à modifier l'audio de la balise de sorte qu'un son différent soit joué en fonction de l'angle entre la direction de l'utilisateur et l'emplacement de la balise. Les angles varient selon la balise sélectionnée (Tactile, Signal lumineux, Ping, etc.) et certaines disposent d'un plus grand nombre de sons que d'autres. Et voilà les balises sonores dans leur version la plus simple.

La seule complexité supplémentaire réside dans l'hypothèse sur l'emplacement du téléphone et sur la direction dans laquelle l'utilisateur est orienté. Examinons-les tour à tour.

### Emplacement
L'emplacement renvoyé par le GPS peut comporter une erreur assez importante, qui dépend de la portion de ciel visible par le GPS du téléphone, ainsi que du nombre d'arbres et de bâtiments élevés qui réfléchissent le signal GPS sur le chemin vers le téléphone.

L'approche que nous avons adoptée pour filtrer l'emplacement consiste à utiliser ce que l'on appelle l'appariement cartographique (map matching). Cela suppose que l'utilisateur a le plus de chances de se déplacer le long d'un chemin ou d'une route cartographié - nous utilisons le terme « Way » (voie) pour couvrir toutes les routes, pistes et chemins. L'appariement cartographique examine où l'utilisateur est passé et, en utilisant la direction du mouvement ainsi que les données cartographiques locales, il choisit l'emplacement le plus probable sur une Way. Cette approche tient compte non seulement des erreurs du GPS, mais aussi des erreurs dans les données cartographiques. Toutes les Ways ne sont pas cartographiées avec précision et comportent donc elles aussi des erreurs. Afin de déterminer quelle est la Way la plus probable sur laquelle se trouve l'utilisateur, l'algorithme prend en compte :
* La proximité d'une Way par rapport à l'emplacement GPS et aux emplacements GPS précédents
* La direction de déplacement - se déplacent-ils dans la même direction que la Way
* La possibilité de rejoindre le nouvel emplacement à partir du dernier emplacement apparié via le réseau de Ways. Cela est nécessaire pour exclure le passage entre des Ways qui ne sont pas réellement connectées, par exemple lorsque l'une passe au-dessus d'une autre par un pont, ou en dessous par un tunnel.
L'appariement cartographique peut décider qu'il n'y a aucune Way à proximité, ou qu'il n'est pas certain de celle sur laquelle se trouve l'utilisateur, et dans ce cas il attend simplement le prochain emplacement GPS et réessaie jusqu'à ce qu'il soit confiant.

### Direction
Il existe plusieurs directions que nous suivons dans le logiciel :

1. La direction dans laquelle le téléphone est orienté. Nous l'utilisons lorsque le téléphone est déverrouillé et que l'application est en cours d'utilisation, mais aussi lorsque le téléphone est verrouillé, à condition qu'il soit tenu à plat avec l'écran orienté vers le ciel. Il est utile de garder cela à l'esprit lorsque vous rangez votre téléphone dans votre sac. S'il est posé à plat au fond d'un sac tenu à la verticale, la direction aléatoire dans laquelle pointe votre sac serait utilisée par l'application.
1. La direction dans laquelle le téléphone se déplace.
1. La direction provenant d'un casque avec suivi des mouvements de la tête. Nous ne l'utilisons pas actuellement, bien que l'application iOS la prenait en charge. Nous disposons de la technologie nécessaire pour l'ajouter à l'avenir.

Lorsque le téléphone est verrouillé et dans un sac, l'application utilise la direction de déplacement. Cependant, si l'utilisateur ne se déplace pas, aucune direction n'est disponible. Lorsque cela se produit, les balises sonores deviennent plus silencieuses pour indiquer qu'il n'est pas possible de connaître la direction actuelle de la balise - l'utilisateur pourrait pivoter sur lui-même sans changer d'emplacement.

Pour certaines utilisations de la direction dans l'application, la direction est « accrochée » à la direction de la Way appariée, de sorte que si l'utilisateur marche à peu près dans la direction de la Way, la direction réelle de la Way est supposée correcte et est utilisée dans ces calculs.

### Conclusion
Bien qu'à première vue les balises sonores soient simples, l'utilisation de l'appariement cartographique pour tenter d'éliminer les erreurs d'emplacement et de direction introduit une bonne dose de complexité.

## Données cartographiques
Les données cartographiques utilisées par l'application proviennent presque entièrement du projet OpenStreetMap. Nous exploitons un serveur qui contient une carte du monde entier à plusieurs niveaux de zoom. Chaque niveau de zoom est divisé en tuiles. Le niveau de zoom 0 contient 1 tuile, le niveau 1 en contient 4, le niveau 2 en contient 16, et ainsi de suite jusqu'au niveau 14 qui contient environ 268 millions de tuiles pour couvrir la planète. Chaque tuile contient plusieurs couches et chaque couche comporte des points, des lignes et des polygones qui peuvent être dessinés pour constituer une carte graphique. C'est cette carte graphique qui est présentée à l'utilisateur dans l'interface de l'application. Chaque point, ligne et polygone possède des métadonnées qui décrivent ce qu'il est. Cela provient pour l'essentiel directement des données OpenStreetMap : une ligne peut donc être un `footway` qui est un `sidewalk` (trottoir) ou une `road` qui est un chemin `minor`

Les données sont transformées en carte graphique au moyen d'un « style » qui comporte des règles sur la façon de dessiner les différents points, lignes et polygones de chaque couche, par exemple comment dessiner un chemin, comment dessiner une forêt, comment dessiner un arrêt de bus. Les règles peuvent varier selon le niveau de zoom, c'est pourquoi à mesure que vous zoomez, de plus en plus de points et de lignes deviennent visibles, qui ne le sont pas en vue dézoomée, par exemple les arrêts de bus et les chemins.

En modifiant le style, nous pouvons changer l'apparence de la carte de l'interface, et c'est de là que provient la « carte accessible » que nous testons actuellement. Elle vise à offrir un contraste plus élevé ainsi que des lignes et un texte plus marqués. Le style est intégré à l'application, nous n'avons donc pas besoin de modifier la carte sur le serveur pour changer son apparence.

Mais comment utilisons-nous les données cartographiques pour l'audio ?

### Utilisation des données cartographiques pour l'audio
Nous utilisons actuellement une part relativement faible des données cartographiques pour générer l'interface utilisateur audio. La quasi-totalité de l'interface utilisateur audio n'utilise que les tuiles au niveau de zoom maximal. L'application assemble une grille de 2 par 2 tuiles autour de l'endroit où se trouve l'utilisateur, puis n'examine que quelques-unes des couches :

* `transportation` - pour tous les types de Ways, y compris les routes, chemins, voies ferrées et lignes de tramway.
* `poi` - les points d'intérêt, par exemple commerces, centres sportifs, bancs, boîtes aux lettres, arrêts de bus, etc.
* `building` - cela concerne les `poi` qui sont cartographiés autrement que par un simple point, par exemple les grands supermarchés ou les hôtels de ville.

Elle relie les lignes et les polygones au-delà des limites des tuiles et transforme toutes les Ways en segments de Way connectés et en intersections. C'est important car cela nous permet de chercher le long d'une Way pour découvrir où l'on peut aller.

Toutes les données analysées sont également placées dans un format facile à interroger afin que l'application puisse facilement trouver quels éléments de la carte se trouvent à proximité. À ce stade, les données sont classées en catégories. Les catégories actuelles sont :

* Routes
* Routes et chemins (toutes les Ways)
* Intersections - les points où les Ways se croisent
* Entrées - ce sont des points sur un bâtiment qui ont été marqués comme étant une entrée.
* Passages - les passages piétons
* POI - tous les points d'intérêt
* Arrêts de transport - arrêts de bus, gares, arrêts de tramway, etc.
* Sous-catégories de POI :
  * Information
  * Objet
  * Lieu
  * Repère
  * Mobilité
  * Sécurité
* Localités et sous-catégories (voir la section suivante)
  * Villes
  * Communes
  * Villages
  * Hameaux

 Une fois cela en place, pour n'importe quel emplacement l'application peut alors facilement trouver

 * « Tous les arrêts de transport dans un rayon de 50 m » ou
 * « L'intersection la plus proche devant moi » ou
 * « Le hameau, village, commune ou ville le plus proche »
  
Une fois cela en place, la création des notifications audio se résume à interroger les données en fonction de l'emplacement et de la direction actuels. À mesure que l'utilisateur se déplace sur une grille de tuiles, celle-ci est mise à jour pour se centrer autour de l'emplacement actuel.

### Davantage de données
L'un des problèmes de notre grille de données cartographiques très locale est qu'elle signifie que nous ne pouvons « voir » au mieux qu'à environ 1 km dans chaque direction. C'est suffisant pour décrire ce qui se trouve devant nous, mais nous aimerions parfois fournir davantage de contexte. Le principal exemple en est lorsque l'application est utilisée alors que l'utilisateur ne marche pas.

Lorsque l'application détecte que l'utilisateur se déplace à plus de 5 mètres par seconde, elle change sa façon de décrire le monde. Au lieu d'annoncer chaque intersection et chaque POI, elle annonce moins souvent et uniquement les routes proches. Le problème est que connaître le nom d'une route n'est pas très utile si vous ne savez pas dans quelle commune elle se trouve.

Pour tenter de résoudre cela, nous analysons désormais aussi les données cartographiques à un niveau de zoom inférieur et extrayons les données de la couche `place`. Celle-ci contient les noms des communes, villes, quartiers, villages, etc. L'un des problèmes liés à la cartographie des éléments est qu'il n'y a pas toujours de limite évidente entre ces lieux. OpenStreetMap dispose parfois de limites de villes dans sa base de données, mais même dans ce cas, le temps que cela arrive jusqu'à notre carte en tuiles, cette information est souvent perdue. Ce dont nous disposons, c'est de l'emplacement où les noms de lieux sont dessinés sur la carte. Ceux-ci sont catégorisés, puis l'application trouve le hameau, village, commune ou ville le plus proche de l'utilisateur et le signale.

Pour de nombreuses villes, le nom réel de la ville ne sera jamais annoncé, car la plupart des villes sont divisées en subdivisions plus petites comme des quartiers, mais celles-ci apportent un contexte supplémentaire très utile. Souvenez-vous simplement que lorsque l'application signale que vous êtes à proximité d'une rue dans un quartier donné, cela signifie seulement que l'étiquette de ce quartier est le point le plus proche, et elle pourrait être inexacte, voire de l'autre côté d'une rivière.

### Davantage de contexte
Plus on peut ajouter de contexte aux descriptions, mieux c'est, tant que cela reste concis et prévisible. L'un des problèmes que nous avons rencontrés lors de la description des intersections est qu'il y avait souvent des Ways « sans nom » impliquées. Ce sont des Ways qui n'ont pas de nom. Dans les données cartographiques, il peut s'agir simplement d'une piste, d'un chemin ou d'une voie de service, mais sans plus de contexte ce n'est pas très utile dans les descriptions textuelles. Heureusement, nous pouvons faire mieux : ce que l'application fait, c'est que chaque fois qu'elle est sur le point d'annoncer une Way sans nom, elle voit si elle peut en déduire davantage de contexte.

* **Est-ce un trottoir ?**
De nombreuses zones d'OpenStreetMap disposent désormais de trottoirs cartographiés séparément des routes. Ils sont généralement étiquetés `sidewalk`, mais ils n'indiquent normalement pas à quelle route ils correspondent.

    Lorsque l'application rencontre un trottoir sans nom, elle recherche une route qu'elle estime longer celui-ci et l'utilise pour nommer le trottoir. Cela s'avère très important pour nos notifications. Au lieu d'annoncer chaque intersection de trottoir, à mesure que nous avançons le long d'un trottoir, les notifications sont faites comme si nous nous déplacions le long de la route associée. Au lieu de *« Se déplaçant vers l'ouest le long d'un chemin »*, nous avons *« Se déplaçant vers l'ouest le long de Moor Road »*. L'utilisateur se trouve sur le trottoir cartographié, mais la description est plus pertinente.

* **Se termine-t-elle sur une Way nommée ?**
Très souvent, il existe des chemins piétons qui relient deux routes entre elles. En examinant les deux extrémités du chemin, nous pouvons facilement ajouter ce contexte de sorte que dans un sens il puisse s'agir de *« Chemin vers Moor Road »* et, en l'abordant depuis l'autre extrémité, de *« Chemin vers Roselea Drive »*. Cela n'est fait que lorsque le chemin ne se divise pas ; s'il se divise en deux chemins sans nom, nous n'essayons pas d'ajouter ce contexte.

* **Se termine-t-elle près d'un Marqueur ?**
Si une Way sans nom commence ou se termine près d'un Marqueur, celui-ci est utilisé pour la décrire, par exemple *« Chemin vers le carrefour du grand arbre »*. L'utilisateur peut ajouter des Marqueurs où il le souhaite, et en ajoutant des Marqueurs le long des réseaux de chemins, il peut ajouter du contexte à tout un itinéraire.

* **Entre-t-elle dans un POI ou en sort-elle ?**
Si une Way sans nom commence à l'extérieur d'un POI et se termine à l'intérieur (ou inversement), nous pouvons alors ajouter ce contexte, par exemple *« Piste vers Lennox Park »*.

* **Se termine-t-elle près d'une Entrée ?**
Si une Way sans nom commence ou se termine plus près d'une Entrée, nous pouvons alors ajouter ce contexte, par exemple *« Voie de service vers Best Buy »*.

* **Se termine-t-elle près d'un Repère ou d'un Lieu ?**
Si une Way sans nom commence ou se termine plus près d'un Repère, nous pouvons également ajouter ce contexte, par exemple *« Voie de service vers la cathédrale St. Giles »*.

* **Est-ce une impasse ?**
L'application marque comme impasse toute Way sans nom qui ne mène nulle part.

* **Passe-t-elle par des marches ?**
Si la Way sans nom passe par-dessus un pont, à travers un tunnel ou monte/descend des marches, cela est noté et ajouté au contexte. Cela est distinct de l'étiquetage de la destination, de sorte qu'un contexte tel que *« Chemin par le pont vers Lennox Park »* est possible.

Ces contextes sont ajoutés dans l'ordre, il est donc possible d'avoir *« Chemin vers Park Lane via des marches »* dans un sens et *« Chemin vers Lennox Park via des marches »* dans l'autre sens. La rue nommée est prioritaire à la sortie du parc, mais le parc est utilisé à l'entrée.

#### Contexte futur
Il existe divers contextes supplémentaires que nous espérons ajouter à l'avenir, notamment :
* Un contexte pour les Ways longeant des cours d'eau linéaires, par exemple *« Chemin le long de la River Dee »*
* Un contexte pour les Ways longeant le bord de plans d'eau *« Chemin le long du réservoir de Milngavie »*
* Un contexte pour les Ways longeant des voies ferrées, par exemple *« Chemin le long de la voie ferrée »*. Cela pourrait même inclure le nom de la ligne ferroviaire
* Ajouter du contenu supplémentaire aux ponts et tunnels : que surplombent-ils ou que passent-ils en dessous, par exemple *« Chemin via un pont au-dessus de la voie ferrée vers Moor Road »*

## Notifications audio
Maintenant que nous disposons des données cartographiques dans un format que nous pouvons facilement utiliser, générer les notifications est vraiment assez simple.

### Notifications en marchant
En marchant, les notifications audio susceptibles de se produire sont (par ordre de priorité) :

1. Décrire la distance qui sépare de la destination actuelle
1. Décrire une intersection à venir
1. Décrire les 5 points d'intérêt les plus proches

Toutes les notifications sont limitées en débit afin qu'elles ne se répètent pas trop souvent. Si l'utilisateur cesse de se déplacer, les notifications s'arrêteront, et même en mouvement une notification ne se répétera pas à chaque nouvel emplacement GPS. La fréquence, comme dans l'application iOS, est la suivante :

* Toutes les 60 secondes pour la destination actuelle
* Toutes les 30 secondes pour une intersection à venir
* Toutes les 60 secondes pour un point d'intérêt

Les notifications peuvent être filtrées via le menu des réglages, et il y a certainement matière à élargir ce comportement.

### Notifications lors de déplacements plus rapides
Lors de déplacements à plus de 5 mètres par seconde, la notification de la destination actuelle a toujours lieu, mais les notifications d'intersection et de points d'intérêt sont remplacées par une notification décrivant approximativement où se trouve l'utilisateur. Cela indique un arrêt de transport proche, un point d'intérêt qui nous contient, par exemple à l'intérieur d'un grand parc, ou une route et une localité proches. Celles-ci utilisent les données décrites précédemment, et il y a manifestement de la place pour permettre une personnalisation de cela à l'avenir.

## Marqueurs et Itinéraires
Pour l'essentiel, les marqueurs et les itinéraires ne sont qu'une fonctionnalité de l'interface utilisateur qui ne dépend ni du GPS ni vraiment des données cartographiques. Les marqueurs sont des emplacements nommés que l'utilisateur souhaite enregistrer, et les itinéraires sont une liste ordonnée de ces marqueurs. L'interface utilisateur permettant de créer les deux est reprise directement de la version iOS.

### Lecture d'un itinéraire
La lecture d'un itinéraire est le moment où les itinéraires prennent vie. Lorsqu'un itinéraire est lu, une balise sonore est créée au premier marqueur de l'itinéraire. Une fois que l'utilisateur s'approche de ce marqueur, l'itinéraire déplace automatiquement la balise sonore vers le marqueur suivant de l'itinéraire. S'il n'y a plus de marqueurs, la lecture de l'itinéraire se termine.

## Conclusion
J'espère que cela a donné un aperçu du fonctionnement de l'application. L'application évolue en permanence en fonction des retours des utilisateurs, alors n'hésitez pas à nous contacter s'il y a quoi que ce soit que vous pensez pouvoir être ajouté.
