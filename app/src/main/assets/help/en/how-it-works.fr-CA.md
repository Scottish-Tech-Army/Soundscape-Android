---
title: Comment fonctionne Soundscape
layout: page
parent: "Utiliser Soundscape"
has_toc: false
lang: fr-CA
permalink: /users/how-it-works.html
machine-translated: true
---
# Comment fonctionne Soundscape
Le but de cette page est de donner une compréhension générale du fonctionnement interne de l'application Soundscape. Vous n'avez pas besoin de la lire pour utiliser l'application, mais elle a été rédigée pour quelques raisons :

1. Pour aider les nouveaux venus intéressés par l'application à comprendre où se situent ses limites
1. Pour donner aux utilisateurs une idée de ce qui pourrait être possible avec de nouvelles fonctionnalités
1. Pour donner aux développeurs un aperçu du fonctionnement de l'application

Deux technologies rendent l'application possible : le GPS et les données d'OpenStreetMap. Le GPS nous donne une bonne idée de l'endroit où se trouve le téléphone et d'où il est passé. Les données d'OpenStreetMap peuvent ensuite être utilisées pour trouver ce qui se trouve à proximité et nous pouvons les utiliser pour le décrire à l'utilisateur.

## Balises audio
À bien des égards, ce sont les éléments les plus simples à mettre en œuvre d'un point de vue technologique. En supposant que nous disposions de l'emplacement du téléphone et d'une direction pour le téléphone, nous pouvons alors modifier l'audio de la balise afin qu'il semble provenir de cette direction. Nous utilisons une bibliothèque de Steam Audio pour effectuer le positionnement audio, qui se sert des fonctions de transfert relatives à la tête (HRTF) pour offrir le meilleur positionnement sonore possible. La seule autre chose que nous faisons consiste à modifier l'audio de la balise de sorte qu'un son différent soit joué selon l'angle entre la direction de l'utilisateur et l'emplacement de la balise. Les angles varient selon la balise sélectionnée (Tactile, Signal lumineux, Ping, etc.) et certaines comportent un plus grand nombre de sons que d'autres. Et voilà les balises audio à leur niveau le plus simple.

La seule complexité supplémentaire concerne l'hypothèse sur l'emplacement du téléphone et la direction dans laquelle l'utilisateur est orienté. Examinons-les tour à tour.

### Emplacement
L'emplacement renvoyé par le GPS peut comporter une erreur assez importante, et cela dépend de la portion de ciel visible par le GPS du téléphone, ainsi que du nombre d'arbres et de grands bâtiments qui réfléchissent le signal GPS sur son chemin vers le téléphone.

L'approche que nous avons adoptée pour filtrer l'emplacement consiste à utiliser ce que l'on appelle le map matching (mise en correspondance avec la carte). On suppose que l'utilisateur est le plus susceptible de se déplacer le long d'un chemin ou d'une route cartographié - nous utilisons le terme « Way » (voie) pour désigner l'ensemble des routes, sentiers et chemins. Le map matching examine où l'utilisateur est passé et, en utilisant la direction du mouvement ainsi que les données cartographiques locales, il choisit l'emplacement le plus probable sur une Way. Cette approche tient compte non seulement des erreurs du GPS, mais aussi des erreurs dans les données cartographiques. Toutes les Way ne sont pas cartographiées avec précision et comportent donc elles aussi des erreurs. Afin de déterminer quelle est la Way la plus probable sur laquelle l'utilisateur se trouve, l'algorithme prend en compte :
* La proximité d'une Way par rapport à l'emplacement GPS et aux emplacements GPS précédents
* La direction de déplacement - se déplacent-ils dans la même direction que la Way
* La possibilité de passer du dernier emplacement mis en correspondance au nouvel emplacement via le réseau de Way. Cela est nécessaire pour exclure le passage entre des Way qui ne sont pas réellement connectées, par exemple lorsque l'une passe au-dessus de l'autre par un pont ou en dessous par un tunnel.
Le map matching peut décider qu'il n'y a aucune Way à proximité, ou qu'il n'est pas certain de celle sur laquelle se trouve l'utilisateur, et dans ce cas il attend simplement le prochain emplacement GPS et réessaie jusqu'à ce qu'il soit confiant.

### Direction
Plusieurs directions sont suivies par le logiciel :

1. La direction dans laquelle pointe le téléphone. Nous l'utilisons lorsque le téléphone est déverrouillé et que l'application est en cours d'utilisation, mais aussi lorsque le téléphone est verrouillé, à condition qu'il soit tenu à plat avec l'écran pointé vers le ciel. Il est utile de garder cela à l'esprit lorsque vous rangez votre téléphone dans votre sac. S'il est placé à plat au fond d'un sac maintenu à la verticale, la direction aléatoire dans laquelle pointe votre sac serait utilisée par l'application.
1. La direction dans laquelle le téléphone se déplace.
1. La direction provenant des écouteurs avec suivi des mouvements de la tête. Nous ne l'utilisons pas actuellement, bien que l'application iOS la prenait en charge. Nous disposons de la technologie nécessaire pour l'ajouter à l'avenir.

Lorsque le téléphone est verrouillé et dans un sac, l'application utilise la direction de déplacement. Cependant, si l'utilisateur ne bouge pas, aucune direction n'est disponible. Lorsque cela se produit, les balises audio deviennent plus discrètes pour indiquer qu'il n'est pas possible de connaître la direction actuelle de la balise - l'utilisateur pourrait être en train de pivoter sans changer d'emplacement.

Pour certaines utilisations de la direction dans l'application, la direction est « alignée » sur la direction de la Way mise en correspondance, de sorte que si l'utilisateur marche à peu près dans la direction de la Way, la direction réelle de la Way est supposée correcte et est utilisée dans ces calculs.

### Conclusion
Bien qu'à première vue les balises audio soient simples, l'utilisation du map matching pour tenter d'éliminer les erreurs d'emplacement et de direction introduit une bonne dose de complexité.

## Données cartographiques
Les données cartographiques utilisées par l'application proviennent presque entièrement du projet OpenStreetMap. Nous exploitons un serveur qui contient une carte du monde entier à plusieurs niveaux de zoom. Chaque niveau de zoom est divisé en tuiles. Le niveau de zoom 0 contient 1 tuile, le niveau 1 contient 4 tuiles, le niveau 2 contient 16 tuiles et ainsi de suite jusqu'au niveau 14 qui contient environ 268 millions de tuiles pour couvrir la planète. Chaque tuile contient plusieurs couches et chaque couche comporte des points, des lignes et des polygones qui peuvent être dessinés pour créer une carte graphique. Cette carte graphique est ce qui est montré à l'utilisateur dans l'interface graphique de l'application. Chaque point, ligne et polygone possède des métadonnées qui décrivent ce dont il s'agit. Cela provient principalement directement des données d'OpenStreetMap, de sorte qu'une ligne peut être un `footway` qui est un `sidewalk` ou une `road` qui est `minor`

Les données sont transformées en carte graphique au moyen d'un « style » qui comporte des règles sur la façon de dessiner les différents points, lignes et polygones de chaque couche, par exemple comment dessiner un sentier, comment dessiner une forêt, comment dessiner un arrêt de bus. Les règles peuvent varier selon le niveau de zoom, ce qui explique pourquoi, à mesure que vous faites un zoom avant, de plus en plus de points et de lignes deviennent visibles alors qu'ils ne le sont pas en zoom arrière, par exemple les arrêts de bus et les sentiers.

En modifiant le style, nous pouvons changer l'apparence de la carte de l'interface graphique, et c'est de là que vient la « carte accessible » que nous mettons à l'essai. Elle vise à offrir un plus grand contraste ainsi que des lignes et un texte plus marqués. Le style est intégré à l'application, de sorte que nous n'avons pas à modifier la carte sur le serveur pour changer son apparence.

Mais comment utilisons-nous les données cartographiques pour l'audio?

### Utilisation des données cartographiques pour l'audio
Nous utilisons actuellement une quantité relativement faible de données cartographiques pour générer l'interface utilisateur audio. La quasi-totalité de l'interface audio n'utilise que les tuiles au niveau de zoom maximal. L'application assemble une grille de 2 sur 2 tuiles autour de l'endroit où se trouve l'utilisateur, puis elle examine seulement quelques-unes des couches :

* `transportation` - pour tous les types de Way, y compris les routes, sentiers, voies ferrées et lignes de tramway.
* `poi` - points d'intérêt, par exemple les commerces, centres sportifs, bancs, boîtes aux lettres, arrêts de bus, etc.
* `building` - cela concerne les `poi` qui sont cartographiés comme plus qu'un simple point, par exemple les grands supermarchés ou les hôtels de ville.

Elle relie les lignes et les polygones à travers les frontières des tuiles et transforme toutes les Way en segments de Way connectés et en intersections. C'est important, car cela nous permet de chercher le long d'une Way pour découvrir où nous pouvons aller.

Toutes les données analysées sont également placées dans un format facile à interroger afin que l'application puisse facilement trouver quels éléments de la carte se trouvent à proximité. À ce stade, les données sont classées en catégories. Les catégories actuelles sont :

* Routes
* Routes et sentiers (toutes les Way)
* Intersections - les points où les Way se croisent
* Entrées - ce sont des points d'un bâtiment qui ont été marqués comme une entrée.
* Passages pour piétons - traversées de routes
* Points d'intérêt - tous les points d'intérêt
* Arrêts de transport - arrêts de bus, gares ferroviaires, arrêts de tramway, etc.
* Sous-catégories des points d'intérêt :
  * Information
  * Objet
  * Lieu
  * Repère
  * Mobilité
  * Sécurité
* Agglomérations et sous-catégories (voir la section suivante)
  * Villes
  * Bourgs
  * Villages
  * Hameaux

 Une fois cela en place, pour tout emplacement l'application peut alors facilement trouver

 * « Tous les arrêts de transport à moins de 50 m » ou
 * « L'intersection la plus proche devant moi » ou
 * « Le hameau, village, bourg ou ville le plus proche »
  
Une fois cela en place, créer les notifications audio se résume à interroger les données en fonction de l'emplacement et de la direction actuels. À mesure que l'utilisateur se déplace sur une grille de tuiles, celle-ci est mise à jour afin de rester centrée sur l'emplacement actuel.

### Plus de données
L'un des problèmes de notre grille de données cartographiques très locale est qu'elle nous permet de « voir » au mieux environ 1 km dans n'importe quelle direction. C'est suffisant pour décrire ce qui se trouve devant nous, mais nous aimerions parfois donner davantage de contexte. Le principal exemple en est lorsque l'application est utilisée et que l'utilisateur ne marche pas.

Lorsque l'application détecte que l'utilisateur se déplace à plus de 5 mètres par seconde, elle change sa façon de décrire le monde. Au lieu d'annoncer chaque intersection et chaque point d'intérêt, elle annonce moins souvent et seulement les routes à proximité. Le problème, c'est que connaître le nom d'une route n'est pas très utile si l'on ne sait pas dans quel bourg elle se trouve.

Pour tenter de résoudre ce problème, nous analysons désormais aussi les données cartographiques à un niveau de zoom inférieur et extrayons les données de la couche `place`. Celle-ci contient les noms de bourgs, villes, quartiers, villages, etc. L'un des problèmes de la cartographie est qu'il n'y a pas toujours de limite évidente entre ces lieux. OpenStreetMap comporte parfois des limites de villes dans sa base de données, mais même dans ce cas, le temps qu'elle arrive jusqu'à notre carte en tuiles, cette information est souvent perdue. Ce dont nous disposons, c'est de l'emplacement où les noms de lieux sont dessinés sur la carte. Ceux-ci sont catégorisés, puis l'application trouve le hameau, village, bourg ou ville le plus proche de l'utilisateur et le signale.

Pour de nombreuses villes, le nom réel de la ville ne sera jamais annoncé, car la plupart des villes sont divisées en divisions plus petites comme des quartiers, mais ceux-ci donnent un contexte supplémentaire très utile. Rappelez-vous simplement que si l'application signale que vous êtes à proximité d'une rue dans un quartier particulier, cela signifie seulement que l'étiquette de ce quartier est le point le plus proche et qu'elle pourrait être incorrecte, voire de l'autre côté d'une rivière.

### Plus de contexte
Plus on peut ajouter de contexte aux descriptions, mieux c'est, à condition que cela reste concis et prévisible. L'un des problèmes que nous avons constatés en décrivant les intersections est qu'il y avait souvent des Way « sans nom » impliquées. Ce sont des Way qui n'ont aucun nom. Dans les données cartographiques, il peut s'agir simplement d'une piste, d'un sentier ou d'une ruelle, mais sans plus de contexte, ce n'est pas très utile dans les descriptions textuelles. Heureusement, nous pouvons faire mieux; ainsi, chaque fois que l'application est sur le point d'annoncer une Way sans nom, elle vérifie si elle peut trouver un peu plus de contexte à son sujet.

* **Est-ce un trottoir?**
De nombreux secteurs d'OpenStreetMap ont maintenant des trottoirs cartographiés séparément des routes. Ceux-ci sont généralement étiquetés `sidewalk`, mais ils n'indiquent normalement pas à quelle route ils correspondent.

    Lorsque l'application rencontre un trottoir sans nom, elle recherche une route qu'elle pense longer et l'utilise pour nommer le trottoir. Cela s'avère très important pour nos notifications. Au lieu d'annoncer chaque intersection de trottoir, à mesure que nous avançons le long d'un trottoir, les notifications sont faites comme si nous avancions le long de la route associée. Au lieu de *« Se déplaçant vers l'ouest le long d'un sentier »*, nous avons *« Se déplaçant vers l'ouest le long de Moor Road »*. L'utilisateur se trouve sur le trottoir cartographié, mais la description a plus de sens.

* **Se termine-t-elle à une Way nommée?**
Très souvent, il existe des sentiers piétonniers qui relient deux routes entre elles. En examinant les deux extrémités du sentier, nous pouvons facilement ajouter ce contexte de sorte que, dans un sens, cela pourrait être *« Sentier vers Moor Road »* et, en approchant de l'autre extrémité, cela pourrait être *« Sentier vers Roselea Drive »*. Cela n'est fait que lorsque le sentier ne se divise pas; s'il se divise en deux sentiers sans nom, nous n'essayons pas d'ajouter ce contexte.

* **Se termine-t-elle près d'un marqueur?**
Si une Way sans nom commence ou se termine près d'un marqueur, celui-ci est utilisé pour la décrire, par exemple *« Sentier vers le carrefour du grand arbre »*. L'utilisateur peut ajouter des marqueurs où il le souhaite et, en ajoutant des marqueurs le long des réseaux de sentiers, il peut ajouter du contexte à tout un itinéraire.

* **Entre-t-elle ou sort-elle d'un point d'intérêt?**
Si une Way sans nom commence à l'extérieur d'un point d'intérêt et se termine à l'intérieur (ou vice versa), nous pouvons alors ajouter ce contexte, par exemple *« Piste vers Lennox Park »*. 

* **Se termine-t-elle près d'une entrée?**
Si une Way sans nom commence ou se termine plus près d'une entrée, nous pouvons alors ajouter ce contexte, par exemple *« Ruelle vers Best Buy »*.

* **Se termine-t-elle près d'un repère ou d'un lieu?**
Si une Way sans nom commence ou se termine plus près d'un repère, nous pouvons également ajouter ce contexte, par exemple *« Ruelle vers la cathédrale St. Giles »*.

* **Est-ce une impasse?**
L'application marque comme impasse toute Way sans nom qui ne mène nulle part.

* **Passe-t-elle par des marches?**
Si la Way sans nom passe sur un pont, traverse un tunnel ou monte/descend des marches, cela est noté et ajouté au contexte. Cela est distinct de l'étiquetage de la destination, de sorte qu'un contexte tel que *« Sentier par-dessus le pont vers Lennox Park »* est possible.

Ces contextes sont ajoutés dans l'ordre et il est donc possible d'avoir *« Sentier vers Park Lane par des marches »* dans un sens et *« Sentier vers Lennox Park par des marches »* dans l'autre sens. La rue nommée a la priorité en quittant le parc, mais le parc est utilisé lorsqu'on y entre.

#### Contexte futur
Il existe divers contextes supplémentaires que nous espérons ajouter à l'avenir, notamment :
* Le contexte des Way longeant des cours d'eau linéaires, par exemple *« Sentier le long de la rivière Dee »*
* Le contexte des Way longeant le bord de plans d'eau *« Sentier le long du réservoir de Milngavie »*
* Le contexte des Way longeant des voies ferrées, par exemple *« Sentier le long de la voie ferrée »*. Cela pourrait même inclure le nom de la ligne ferroviaire
* L'ajout de contenu supplémentaire aux ponts et tunnels, ce qu'ils enjambent ou ce sous quoi ils passent, par exemple *« Sentier par un pont au-dessus de la voie ferrée vers Moor Road »*

## Notifications audio
Maintenant que nous disposons des données cartographiques dans un format facile à utiliser, générer les notifications est vraiment assez simple.

### Notifications en marchant
En marchant, les notifications audio qui peuvent se produire sont (par ordre de priorité) :

1. Décrire à quelle distance se trouve la destination actuelle
1. Décrire une intersection à venir
1. Décrire les 5 points d'intérêt les plus proches

Toutes les notifications sont limitées en fréquence afin qu'elles ne se répètent pas trop souvent. Si l'utilisateur cesse de bouger, les notifications s'arrêtent et, même en mouvement, une notification ne se répétera pas à chaque nouvel emplacement GPS. La fréquence, identique à celle de l'application iOS, est :

* Toutes les 60 secondes pour la destination actuelle
* Toutes les 30 secondes pour une intersection à venir
* Toutes les 60 secondes pour un point d'intérêt

Les notifications peuvent être filtrées dans le menu des réglages, et il y a certainement de la marge pour élargir ce comportement.

### Notifications en se déplaçant plus vite
Lorsque vous vous déplacez à plus de 5 mètres par seconde, la notification de la destination actuelle a toujours lieu, mais les notifications d'intersection et de points d'intérêt sont remplacées par une notification décrivant approximativement où se trouve l'utilisateur. Cela donne un arrêt de transport à proximité, un point d'intérêt qui nous contient, par exemple à l'intérieur d'un grand parc, ou une route et une agglomération à proximité. Celles-ci utilisent les données décrites précédemment, et il y a manifestement de la place pour permettre une personnalisation de cela à l'avenir.

## Marqueurs et itinéraires
Pour l'essentiel, les marqueurs et les itinéraires ne sont qu'une fonctionnalité de l'interface utilisateur qui ne dépend ni du GPS ni vraiment des données cartographiques. Les marqueurs sont des emplacements nommés que l'utilisateur souhaite stocker, et les itinéraires sont une liste ordonnée de ces marqueurs. L'interface utilisateur pour créer les deux est tirée directement de la version iOS.

### Lecture d'itinéraire
La lecture d'itinéraire est le moment où les itinéraires prennent vie. Lorsqu'un itinéraire est lu, une balise audio est créée au premier marqueur de l'itinéraire. Une fois que l'utilisateur s'approche de ce marqueur, l'itinéraire déplace automatiquement la balise audio vers le marqueur suivant de l'itinéraire. S'il n'y a plus de marqueurs, la lecture de l'itinéraire se termine.

## Conclusion
Nous espérons que cela vous a donné un aperçu du fonctionnement de l'application. L'application évolue constamment en fonction des commentaires des utilisateurs, alors n'hésitez pas à nous contacter si vous pensez qu'il y a quelque chose à ajouter.
