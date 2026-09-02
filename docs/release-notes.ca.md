---
title: Notes de la versió
layout: page
nav_order: 5
has_toc: false
lang: ca
permalink: /release-notes.html
machine-translated: true
---

# Notes de la versió

Soundscape 2.0 és una versió important i ara mateix es troba en beta tancada. El canvi principal és
que ara Soundscape té alguna cosa útil a dir quan viatgeu amb cotxe, autobús o tren, i no només quan
aneu a peu. També hi ha molta feina de menor abast sobre com es descriuen els llocs, vint llengües
noves i una llarga llista de correccions.

Les notes de versions anteriors són a la pàgina
[Notes de la versió 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novetats de la 2.0

* **Avisos mentre viatgeu amb cotxe, autobús o tren.** Soundscape reconeix que us desplaceu a certa
  velocitat i descriu el trajecte en comptes de l'entorn immediat.
* **Avís en creuar cursos d'aigua i vies fèrries.** Rius, canals, badies i línies de ferrocarril
  s'anuncien mentre els creueu, tant si camineu com si viatgeu.
* **Millors adreces i noms de lloc.** Els llocs sense adreça pròpia ara reben el carrer i la zona on
  són, els números de porta s'associen al costat correcte del carrer, i les parades d'autobús de la
  Gran Bretanya fan servir els seus noms oficials.
* **Vint llengües noves**, fins a un total de 46. Aquest lloc de documentació també està traduït.
* **Despertar en marxar.** El mode de repòs ara pot despertar Soundscape quan deixeu el lloc on el
  vau adormir.
* **Distàncies més curtes i naturals**, amb unitats més grans quan us desplaceu ràpid.
* **Una sortida més ràpida.** *Sortir de Soundscape* ara és a la part superior del menú principal.
* **Millores als mapes fora de línia**, com ara actualitzar in situ un mapa ja baixat i un mapa de les
  regions disponibles en aquest lloc web.
* **Molta feina d'accessibilitat** amb TalkBack, sobretot al voltant de les pantalles inicials.
* **Moltíssimes correccions de tancaments inesperats i d'estabilitat.**

A la 2.0 s'han **eliminat** dues coses: el control per veu i el menú d'idioma dins l'aplicació. Vegeu
[Funcions eliminades](#things-that-have-been-removed) més avall per saber què podeu fer al seu lloc.

---

## Amb més detall

### Viatjar amb cotxe, autobús o tren

És la novetat més gran per a qui ja fa servir l'aplicació. Abans, Soundscape tenia molt poc a dir tan
bon punt pujàveu a un vehicle: continuava descrivint l'entorn immediat, cosa que a certa velocitat es
convertia en un degoteig de coses que ja havíeu deixat enrere.

Ara Soundscape s'adona que us desplaceu més ràpid que a pas de vianant i canvia el que us diu. No cal
activar res, i tot torna a la normalitat tot sol quan alenteixi o baixeu i comenceu a caminar.

Durant el trajecte sentireu:

* **On sou**, de tant en tant: la carretera per on aneu i la direcció, per exemple «Circulant cap al
  nord per la M8». Les carreteres amb número s'anuncien pel número, i Soundscape no torna a anunciar
  la mateixa carretera cada cop que en canvia el nom del carrer.
* **Pobles i ciutats** cap on aneu, amb la distància, així com aquells dels quals us allunyeu o
  simplement passeu a prop.
* **Enllaços i sortides d'autopista** quan hi arribeu.
* **Grans punts de referència** en passar-hi, com ara parcs, hospitals, estadis i centres comercials.
* **Parades d'autobús, de tramvia i estacions de tren** en passar-hi. Soundscape només esmenta les
  parades del vostre costat de la via, ja que les del costat contrari serveixen el sentit oposat.
* **Rius, canals i vies fèrries que creueu.**
* **Túnels**, cosa que sobretot explica per què Soundscape està a punt de quedar en silenci: a dins no
  hi ha senyal de GPS.

En **tren**, Soundscape dedueix que sou en una via fèrria i no en una carretera, i us diu per quines
poblacions passeu i quina distància heu fet des de l'última estació. Esbrinar-ho és més difícil del
que sembla, perquè les autopistes i les línies de tren sovint es construeixen de costat durant
quilòmetres, de manera que bona part de la feina d'aquesta versió ha anat a no confondre l'una amb
l'altra.

Els avisos habituals per a vianants —botigues properes, passos de vianants, etc.— es retenen a posta
mentre viatgeu, i les distàncies a què s'anuncien les coses s'han ampliat força perquè en tingueu
notícia abans d'haver-les passades.

### Creuar cursos d'aigua i vies fèrries

Soundscape ara us diu quan creueu un riu, un canal, una badia, una cala o una línia de ferrocarril.
Funciona tant caminant com viatjant, i cobreix igualment passar per sota i per damunt, de manera que
es descriuen tant una passarel·la com un pas soterrani.

### Millors adreces i noms de lloc

S'ha treballat molt perquè Soundscape descrigui els llocs com ho faria una persona:

* Els llocs sense adreça pròpia ara es descriuen pel carrer i la zona on són, en comptes de quedar
  imprecisos.
* Els números de porta s'associen al costat correcte del carrer. Abans es podia informar d'una adreça
  des de la vorera del davant.
* L'adreça d'un lloc ja no repeteix el nom del lloc mateix.
* Les parades d'autobús de la Gran Bretanya fan servir els noms oficials del transport públic,
  normalment els que apareixen a l'horari i al pal de la parada.
* Els camins sense nom que segueixen un riu o un canal ara reben el nom de l'aigua que acompanyen.
* Els camins i les carreteres sense nom es descriuen amb més sentit, i les paraules que s'hi fan
  servir estan ben traduïdes en comptes d'aparèixer en anglès.

### Llengües

A la 2.0 s'han afegit vint llengües noves: àrab, bengalí, búlgar, català, croat, txec, haussa,
hongarès, indonesi, coreà, marathi, serbi, eslovac, eslovè, suahili, tàmil, telugu, tai, urdú i
vietnamita. Totes aquestes llengües són en fase alfa i ens interessa molt rebre comentaris sobre la
seva exactitud. En total, Soundscape ja està disponible en 46 llengües, i aquest lloc de documentació
també s'ha traduït.

L'àrab egipci s'ha integrat a l'àrab i el luganda s'ha retirat, ja que cap dels dos tenia prou text
traduït per ser útil.

Les traduccions són feina de la comunitat i agraïm la vostra ajuda, o les correccions allà on alguna
cosa es llegeixi malament. Qualsevol text es pot millorar a
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Mode de repòs

El mode de repòs ha guanyat el **despertar en marxar**. Quan poseu Soundscape en repòs, podeu
demanar-li que es desperti tan bon punt deixeu la zona, cosa útil quan arribeu a un lloc i voleu
silenci fins que torneu a sortir.

### Distàncies i veu

Les distàncies dites s'han escurçat i s'han fet més naturals, i Soundscape ara passa a unitats més
grans quan us desplaceu ràpid: milles o quilòmetres en comptes d'un recompte llarg en peus o metres.
Cada llengua decideix per si mateixa com dir una distància fraccionària, cosa que abans estava forçada
a un patró de forma anglesa.

### Mapes fora de línia

Els mapes fora de línia van arribar amb la 1.0 i s'han anat millorant:

* Un mapa baixat ara es pot actualitzar in situ quan hi ha una versió més recent, des de la pantalla
  de detalls de l'extracte.
* Els mapes que no es poden fer servir —per exemple una baixada malmesa— ara es marquen clarament en
  comptes de fallar en silenci.
* Les baixades són més fiables i la pantalla mostra què passa mentre s'obté la llista de mapes
  disponibles, en lloc d'un indicador de càrrega a pantalla completa.
* Una baixada acabada només apareix com a acabada quan realment està a punt per fer-se servir.
* En aquest lloc hi ha un
  [mapa de les regions disponibles]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Accessibilitat

S'ha treballat moltíssim en el comportament dels lectors de pantalla, sobretot a les pantalles inicials
on abans el focus saltava al lloc equivocat. Altres millores inclouen una millor lectura de mides de
fitxer i de nombres decimals, indicacions correctes del tipus «toqueu dos cops per...» en llengües que
posen el verb al final, i indicacions sensates allà on no n'hi havia cap.

### Menús i navegació

* **Sortir de Soundscape** ara és el primer element del menú principal, en comptes d'estar més avall.
* El menú principal ja no deixa veure una franja de pantalla en un costat, cosa que donava a qui fa
  servir lector de pantalla una àrea addicional confusa per tocar.
* El gest de tornada del sistema ja no salta un nivell quan navegueu per categories a Llocs propers.
* El *tutorial d'àudio* ha passat a dir-se **tutorial guiat**.
* La configuració s'ha endreçat, i *Restablir els valors per defecte* ara ho neteja tot correctament.

### Estabilitat

La 2.0 inclou una llarga llista de correccions de tancaments i bloqueigs, entre els quals el bloqueig
de l'aplicació a la pantalla d'inici, bloqueigs en restablir la configuració, tancaments amb un mapa
baixat malmès, tancaments en obrir els detalls d'una ruta des de la pantalla principal, tancaments en
canviar d'idioma i diversos problemes informats automàticament a través de Play Store. El comportament
relatiu a la bateria i a l'inici també s'ha fet més robust en telèfons que tanquen agressivament les
aplicacions en segon pla.

### Funcions eliminades
{: #things-that-have-been-removed }

* **El control per veu** s'ha eliminat. Mai no va funcionar prou bé per mantenir-lo, i els botons
  multimèdia dels auriculars cobreixen en gran part el mateix: vegeu
  [Ajuda sobre l'ús dels controls multimèdia]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **El menú d'idioma dins l'aplicació** ha desaparegut. Soundscape ara segueix l'idioma que teniu
  configurat al telèfon, que és el que la majoria esperava. Per canviar-lo, canvieu l'idioma del
  telèfon o definiu un idioma per aplicació a la seva configuració, si l'ofereix.

## Com informar-nos de problemes

Si alguna cosa no va bé, ens agradaria saber-ho. Escriviu al Help Desk a
<soundscapeAndroid@scottishtecharmy.support>, o pregunteu al Slack si sou membre de l'STA.

Si un avís va ser incorrecte o no es va produir, un enregistrament del vostre trajecte ens ajuda
moltíssim: el podem reproduir i veure exactament amb què treballava Soundscape. Trobareu les
instruccions a
[Proporcionar un registre d'ubicació per a depuració]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Una nota sobre l'iPhone

Tot això de dalt fa referència a l'aplicació d'Android, però val la pena saber on ha anat la resta de
la feina d'aquesta versió. Soundscape ara també funciona a l'iPhone, i totes dues aplicacions es
construeixen a partir del mateix codi compartit: les mateixes pantalles, les mateixes expressions i els
mateixos avisos. Així, una novetat com els avisos de trajecte de més amunt arriba a totes dues alhora
en comptes d'escriure's dues vegades. Aquesta base comuna explica per què la 2.0 ha trigat tant, i és
el que hauria de fer que les properes versions arribin més de pressa a totes dues plataformes.
L'aplicació per a iPhone està disponible ara mateix a través de TestFlight per invitació: pregunteu al
Slack si sou membre de l'STA, o escriviu al Help Desk.
