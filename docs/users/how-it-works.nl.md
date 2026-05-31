---
title: Hoe Soundscape werkt
layout: page
parent: Using Soundscape
has_toc: false
lang: nl
permalink: /users/how-it-works.html
machine-translated: true
---
# Hoe Soundscape werkt
Het doel van deze pagina is om een algemeen begrip te geven van hoe de Soundscape-app onder de motorkap werkt. U hoeft dit niet te lezen om de app te gebruiken, maar er zijn een paar redenen waarom het is geschreven:

1. Om geïnteresseerde nieuwkomers van de app te helpen begrijpen waar de beperkingen ervan liggen
1. Om gebruikers een idee te geven van wat er nog meer mogelijk zou kunnen zijn met nieuwe functies
1. Om ontwikkelaars een overzicht te geven van de werking van de app

Er zijn twee stukken technologie die de app mogelijk maken: GPS en OpenStreetMap-gegevens. De GPS geeft ons een goed idee van waar de telefoon zich bevindt en waar hij is geweest. OpenStreetMap-gegevens kunnen vervolgens worden gebruikt om te vinden wat zich in de buurt bevindt en die kunnen we gebruiken om dat aan de gebruiker te beschrijven.

## Audiobakens
In de meeste opzichten zijn dit vanuit technologisch oogpunt de eenvoudigste dingen om te implementeren. Ervan uitgaande dat we de locatie van de telefoon en een richting voor de telefoon hebben, kunnen we de audio voor het baken zo aanpassen dat het klinkt alsof het uit die richting komt. We gebruiken een bibliotheek van Steam Audio om de audiopositionering uit te voeren, die head-related transfer functions gebruikt om de best klinkende positionering te bieden die mogelijk is. Het enige andere dat we doen, is de bakenaudio veranderen zodat er een ander geluid wordt afgespeeld afhankelijk van de hoek tussen de richting van de gebruiker en de locatie van het baken. De hoeken variëren afhankelijk van het geselecteerde baken (Tactiel, Flits, Ping enzovoort) en sommige hebben een groter aantal geluiden dan andere. En dat zijn audiobakens op het eenvoudigste niveau.

De enige aanvullende complexiteit is de aanname over de locatie van de telefoon en de richting waarin de gebruiker wijst. Laten we deze één voor één bekijken.

### Locatie
De locatie die GPS teruggeeft, kan een vrij grote fout hebben, en dit hangt af van hoeveel van de hemel zichtbaar is voor de GPS van de telefoon, en hoeveel bomen en hoge gebouwen het GPS-signaal weerkaatsen op weg naar de telefoon.

De aanpak die we hebben gekozen voor het filteren van de locatie is het gebruik van wat bekend staat als map matching. Dit gaat ervan uit dat de gebruiker zich het meest waarschijnlijk langs een in kaart gebracht pad of weg verplaatst - we gebruiken de term 'Way' (route) om alle wegen, paden en sporen aan te duiden. Map matching kijkt naar waar de gebruiker is geweest en kiest met behulp van de bewegingsrichting samen met lokale kaartgegevens de meest waarschijnlijke locatie op een Way. Deze aanpak houdt niet alleen rekening met fouten van de GPS, maar ook met fouten in de kaartgegevens. Niet alle Ways zijn nauwkeurig in kaart gebracht en dus hebben ook zij fouten. Om te bepalen welke Way de gebruiker het meest waarschijnlijk op zit, houdt het algoritme rekening met:
* Hoe dicht een Way bij de GPS-locatie en eerdere GPS-locaties ligt
* De bewegingsrichting - bewegen ze in dezelfde richting als de Way
* Of het mogelijk is om van de laatste map-matched locatie naar de nieuwe locatie te komen via het netwerk van Ways. Dit is nodig om te voorkomen dat er wordt geschakeld tussen Ways die niet daadwerkelijk verbonden zijn, bijv. de ene loopt over de andere via een brug, of eronder via een tunnel.
De map matching kan beslissen dat er geen Ways in de buurt zijn, of dat het niet zeker weet op welke de gebruiker zit, en in dat geval wacht het eenvoudigweg op de volgende GPS-locatie en probeert het opnieuw totdat het zeker is.

### Richting
Er zijn verschillende richtingen die we in de software volgen:

1. De richting waarin de telefoon wijst. We gebruiken dit wanneer de telefoon ontgrendeld is en de app in gebruik is, maar ook wanneer de telefoon vergrendeld is, zolang hij plat wordt gehouden met het scherm naar de lucht gericht. Het is nuttig om dit in gedachten te houden wanneer u uw telefoon in uw tas stopt. Als hij plat onderin een tas wordt gelegd die rechtop wordt gehouden, zou de willekeurige richting waarin uw tas wijst door de app worden gebruikt.
1. De richting waarin de telefoon zich verplaatst.
1. De richting van een koptelefoon met head tracking. Dit gebruiken we momenteel niet, hoewel de iOS-app het wel ondersteunde. We hebben de technologie wel beschikbaar om het in de toekomst toe te voegen.

Wanneer de telefoon vergrendeld is en in een tas zit, gebruikt de app de bewegingsrichting. Als de gebruiker echter niet beweegt, is er geen richting beschikbaar. Wanneer dit gebeurt, worden de audiobakens stiller om aan te geven dat het niet mogelijk is om de huidige richting van het baken te kennen - de gebruiker zou zich kunnen omdraaien zonder van locatie te veranderen.

Voor sommige toepassingen van richting in de app wordt de richting 'vastgeklikt' aan de richting van de map-matched Way, dus als de gebruiker ongeveer in de richting van de Way loopt, wordt aangenomen dat de werkelijke richting van de Way correct is en wordt deze in die berekeningen gebruikt.

### Conclusie
Hoewel de audiobakens op het eerste gezicht eenvoudig zijn, introduceert het gebruik van map matching om locatie- en richtingsfouten te proberen te verwijderen een behoorlijke mate van complexiteit.

## Kaartgegevens
De kaartgegevens die door de app worden gebruikt, zijn vrijwel allemaal afkomstig uit het OpenStreetMap-project. We draaien een server die een kaart van de hele wereld bevat op meerdere zoomniveaus. Elk zoomniveau is opgedeeld in tegels. Zoomniveau 0 bevat 1 tegel, niveau 1 bevat 4 tegels, niveau 2 bevat 16 tegels enzovoort tot niveau 14, dat ongeveer 268 miljoen tegels bevat om de planeet te bedekken. Elke tegel bevat meerdere lagen en elke laag heeft punten, lijnen en polygonen die kunnen worden getekend om een grafische kaart te maken. Die grafische kaart is wat aan de gebruiker wordt getoond in de GUI van de app. Elk punt, elke lijn en elke polygoon heeft metadata die beschrijft wat het is. Dit komt meestal rechtstreeks uit de OpenStreetMap-gegevens, dus een lijn kan een `footway` zijn die een `sidewalk` is, of een `road` die een `minor` is

De gegevens worden omgezet in de grafische kaart via een 'stijl' die regels bevat over hoe de verschillende punten, lijnen en polygonen in elke laag moeten worden getekend, bijv. hoe een pad te tekenen, hoe een bos te tekenen, hoe een bushalte te tekenen. De regels kunnen variëren per zoomniveau, en daarom worden er, naarmate u inzoomt, steeds meer punten en lijnen zichtbaar die niet zichtbaar zijn wanneer uitgezoomd, bijv. bushaltes en paden.

Door de stijl te wijzigen kunnen we veranderen hoe de GUI-kaart eruitziet, en daar komt de 'toegankelijke kaart' vandaan die we uitproberen. Deze is bedoeld om een groter contrast en vettere lijnen en tekst te hebben. De stijl is in de app ingebouwd, zodat we de kaart op de server niet hoeven te veranderen om te veranderen hoe deze eruitziet.

Maar hoe gebruiken we de kaartgegevens voor audio?

### De kaartgegevens gebruiken voor audio
We gebruiken momenteel een relatief kleine hoeveelheid van de kaartgegevens om de audio-gebruikersinterface te genereren. Bijna de hele audio-UI gebruikt alleen de tegels op het maximale zoomniveau. De app voegt een raster van 2 bij 2 tegels samen rond de plek waar de gebruiker zich bevindt en kijkt vervolgens slechts naar enkele van de lagen:

* `transportation` - voor alle soorten Ways, waaronder wegen, paden, spoorwegen en tramwegen.
* `poi` - interessante punten, bijv. winkels, sportcentra, banken, brievenbussen, bushaltes enzovoort.
* `building` - dit is voor `poi` die als meer dan alleen een punt in kaart zijn gebracht, bijv. grote supermarkten of stadhuizen.

Het verbindt lijnen en polygonen over de tegelgrenzen heen en zet alle Ways om in verbonden Way-segmenten en kruispunten. Dit is belangrijk omdat het ons in staat stelt langs een Way te zoeken om uit te vinden waar we kunnen komen.

Alle geparseerde gegevens worden ook in een gemakkelijk doorzoekbaar formaat geplaatst, zodat de app gemakkelijk kan vinden welke kenmerken van de kaart zich in de buurt bevinden. Op dit punt worden de gegevens ingedeeld in categorieën. De huidige categorieën zijn:

* Wegen
* Wegen en paden (alle Ways)
* Kruispunten - de punten waar Ways elkaar kruisen
* Ingangen - dit zijn punten op een gebouw die als ingang zijn gemarkeerd.
* Oversteekplaatsen - oversteekplaatsen op wegen
* POI's - alle interessante punten
* Haltes openbaar vervoer - bushaltes, treinstations, tramhaltes enzovoort.
* Subcategorieën van POI's:
  * Informatie
  * Object
  * Plaats
  * Herkenningspunt
  * Mobiliteit
  * Veiligheid
* Nederzettingen en subcategorieën (zie volgende sectie)
  * Steden
  * Gemeenten
  * Dorpen
  * Gehuchten

 Met dit op zijn plaats kan de app voor elke locatie vervolgens gemakkelijk vinden

 * "Alle haltes van het openbaar vervoer binnen 50m" of
 * "Het dichtstbijzijnde kruispunt vóór mij" of
 * "Het dichtstbijzijnde gehucht, dorp, gemeente of stad"
  
Met dit op zijn plaats is het maken van de audiowaarschuwingen slechts een kwestie van het opvragen van de gegevens op basis van de huidige locatie en richting. Terwijl de gebruiker zich over een tegelraster verplaatst, werkt het dit bij zodat het rond de huidige locatie gecentreerd is.

### Meer gegevens
Een van de problemen met ons zeer lokale kaartgegevensraster is dat we daardoor hooguit ongeveer 1km in elke richting kunnen 'zien'. Dat is prima wanneer we beschrijven wat zich vóór ons bevindt, maar soms willen we graag meer context geven. Het belangrijkste voorbeeld hiervan is wanneer de app wordt gebruikt en de gebruiker niet loopt.

Wanneer de app detecteert dat de gebruiker met meer dan 5 meter per seconde reist, schakelt het over op hoe het de wereld beschrijft. In plaats van elk kruispunt en elke POI aan te kondigen, kondigt het minder vaak aan en alleen nabijgelegen wegen. Het probleem hiermee is dat het kennen van een wegnaam niet erg nuttig is als u niet weet in welke gemeente die ligt.

Om dit te proberen aan te pakken, parseren we nu ook de kaartgegevens op een lager zoomniveau en halen we gegevens op uit de `place`-laag. Deze bevat de namen van gemeenten, steden, buurten, dorpen enzovoort. Een probleem met het in kaart brengen van dingen is dat er niet altijd een duidelijke grens tussen deze plaatsen is. OpenStreetMap heeft soms stadsgrenzen in zijn database, maar zelfs wanneer dat het geval is, gaat die informatie vaak verloren tegen de tijd dat het onze getegelde kaart bereikt. Wat we wél hebben, is de locatie waar de plaatsnamen op de kaart worden getekend. Deze worden gecategoriseerd en vervolgens vindt de app het dichtstbijzijnde gehucht, dorp, gemeente of stad bij de gebruiker en rapporteert dat.

Voor veel steden zal de eigenlijke stadsnaam nooit worden aangekondigd, omdat de meeste steden zijn opgedeeld in kleinere onderverdelingen zoals buurten, maar die geven extra context en zijn erg nuttig. Onthoud alleen dat wanneer de app meldt dat u zich in de buurt van een straat in een bepaalde buurt bevindt, dit alleen betekent dat het label voor die buurt het dichtstbijzijnde punt is en dat het onjuist kan zijn of zelfs aan de overkant van een rivier kan liggen.

### Meer context
Hoe meer context er aan beschrijvingen kan worden toegevoegd, hoe beter, zolang het beknopt en voorspelbaar blijft. Een van de problemen die we zagen bij het beschrijven van kruispunten, is dat er vaak 'naamloze' Ways bij betrokken waren. Dit zijn Ways die geen naam hebben. In de kaartgegevens kunnen dit slechts een spoor, een pad of een ventweg zijn, maar zonder meer context is het niet erg nuttig in de tekstbeschrijvingen. Gelukkig kunnen we het beter doen, dus wat de app doet is dat het, wanneer het op het punt staat een naamloze Way aan te kondigen, kijkt of het wat meer context ervoor kan achterhalen.

* **Is het een stoep?**
Veel gebieden van OpenStreetMap hebben nu stoepen apart van wegen in kaart gebracht. Deze worden meestal getagd als `sidewalk`, maar ze geven normaal gesproken niet aan welke weg het is waar ze de stoep van zijn.

    Wanneer de app een naamloze stoep tegenkomt, zoekt het naar een weg waarvan het denkt dat die ernaast loopt en gebruikt het die om de stoep te benoemen. Dit blijkt erg belangrijk te zijn voor onze waarschuwingen. In plaats van elk stoepkruispunt aan te kondigen, worden de waarschuwingen terwijl we langs een stoep lopen gemaakt alsof we langs de bijbehorende weg liepen. In plaats van *"U loopt naar het westen langs pad"* hebben we *"U loopt naar het westen langs Moor Road"*. De gebruiker bevindt zich op de in kaart gebrachte stoep, maar de beschrijving is logischer.

* **Eindigt het bij een benoemde Way?**
Heel vaak zijn er voetpaden die twee wegen met elkaar verbinden. Door naar beide uiteinden van het pad te kijken, kunnen we die context gemakkelijk toevoegen, zodat het in de ene richting *"Pad naar Moor Road"* kan zijn en wanneer u vanaf het andere uiteinde nadert *"Pad naar Roselea Drive"*. Dit gebeurt alleen waar het pad zich niet splitst; als het zich splitst in twee naamloze paden, dan proberen we deze context niet toe te voegen.

* **Eindigt het in de buurt van een markering?**
Als een naamloze Way begint of eindigt in de buurt van een markering, wordt die gebruikt om hem te beschrijven, bijv. *"Pad naar kruising grote boom"*. De gebruiker kan markeringen toevoegen waar hij maar wil, en door markeringen langs padnetwerken toe te voegen, kan hij context toevoegen aan een hele route.

* **Gaat het een POI in of uit?**
Als een naamloze Way buiten een POI begint en erbinnen eindigt (of omgekeerd), dan kunnen we die context toevoegen, bijv. *"Spoor naar Lennox Park"*. 

* **Eindigt het in de buurt van een ingang?**
Als een naamloze Way begint of eindigt dichter bij een ingang, dan kunnen we die context toevoegen, bijv. *"Ventweg naar Best Buy"*.

* **Eindigt het in de buurt van een herkenningspunt of plaats?**
Als een naamloze Way begint of eindigt dichter bij een herkenningspunt, dan kunnen we ook die context toevoegen, bijv. *"Ventweg naar St. Giles Cathedral"*.

* **Is het een doodlopende weg?**
De app markeert als doodlopend elke naamloze Way die nergens heen leidt.

* **Passeert het trappen?**
Als de naamloze Way over een brug, door een tunnel of op/af trappen gaat, dan wordt dit genoteerd en aan de context toegevoegd. Dit staat los van de bestemmingstagging, dus context zoals *"Pad over brug naar Lennox Park"* is mogelijk.

Deze contexten worden in volgorde toegevoegd en daarom is het mogelijk om *"Pad naar Park Lane via trappen"* in de ene richting en *"Pad naar Lennox Park via trappen"* in de andere richting te hebben. De benoemde straat krijgt prioriteit bij het verlaten van het park, maar het park wordt gebruikt bij het betreden ervan.

#### Toekomstige context
Er zijn verschillende aanvullende contexten die we in de toekomst hopen toe te voegen, waaronder:
* Context voor Ways die lineaire waterelementen volgen, bijv. *"Pad naast River Dee"*
* Context voor Ways die de rand van waterlichamen volgen *"Pad naast Milngavie-reservoir "*
* Context voor Ways die spoorwegen volgen, bijv. *"Pad naast spoorweg"*. Dit zou zelfs de naam van de spoorlijn kunnen bevatten
* Extra inhoud toevoegen aan bruggen en tunnels, waar gaan ze over- of onderheen, bijv. *"Pad via brug over spoorweg naar Moor Road"*

## Audiowaarschuwingen
Nu we de kaartgegevens in een formaat hebben dat we gemakkelijk kunnen gebruiken, is het genereren van waarschuwingen werkelijk vrij eenvoudig.

### Waarschuwingen tijdens het lopen
Tijdens het lopen zijn de audiowaarschuwingen die kunnen plaatsvinden (in volgorde van prioriteit):

1. Beschrijven hoe ver de huidige bestemming weg is
1. Een naderend kruispunt beschrijven
1. De 5 dichtstbijzijnde interessante punten beschrijven

Alle waarschuwingen zijn gelimiteerd in frequentie zodat ze niet te vaak worden herhaald. Als de gebruiker stopt met bewegen, stoppen de waarschuwingen, en zelfs tijdens het bewegen wordt een waarschuwing niet bij elke nieuwe GPS-locatie herhaald. De frequentie is, net als bij de iOS-app:

* Elke 60 seconden voor de huidige bestemming
* Elke 30 seconden voor een naderend kruispunt
* Elke 60 seconden voor een interessant punt

Waarschuwingen kunnen worden gefilterd via het instellingenmenu, en er is zeker ruimte om dit gedrag uit te breiden.

### Waarschuwingen tijdens sneller reizen
Wanneer u met meer dan 5 meter per seconde reist, vindt de waarschuwing voor de huidige bestemming nog steeds plaats, maar de waarschuwingen voor kruispunten en interessante punten worden vervangen door een waarschuwing die ongeveer beschrijft waar de gebruiker zich bevindt. Dit geeft een nabijgelegen halte van het openbaar vervoer, een interessant punt waar we ons in bevinden, bijv. in een groot park, of een nabijgelegen weg en nederzetting. Deze gebruiken de eerder beschreven gegevens, en er is duidelijk ruimte om aanpassing hiervan in de toekomst toe te staan.

## Markeringen en routes
Voor het grootste deel zijn markeringen en routes slechts een gebruikersinterfacefunctie die niet afhangt van GPS en ook niet echt van kaartgegevens. Markeringen zijn benoemde locaties die de gebruiker wil opslaan, en routes zijn een geordende lijst van die markeringen. De gebruikersinterface om beide te maken, is rechtstreeks overgenomen van de iOS-versie.

### Route afspelen
Het afspelen van routes is waar routes tot leven komen. Wanneer een route wordt afgespeeld, wordt er een audiobaken gemaakt bij de eerste markering in de route. Zodra de gebruiker dicht bij die markering komt, verplaatst de route het audiobaken automatisch naar de volgende markering in de route. Als er geen markeringen meer zijn, dan eindigt het afspelen van de route.

## Conclusie
Hopelijk heeft dit enig inzicht gegeven in hoe de app functioneert. De app is voortdurend in ontwikkeling op basis van feedback van gebruikers, dus neem gerust contact op als er iets is waarvan u denkt dat het toegevoegd zou kunnen worden.
