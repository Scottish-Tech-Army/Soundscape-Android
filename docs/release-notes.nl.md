---
title: Releaseopmerkingen
layout: page
nav_order: 5
has_toc: false
lang: nl
permalink: /release-notes.html
machine-translated: true
---

# Releaseopmerkingen

Soundscape 2.0 is een grote release en bevindt zich momenteel in een gesloten bèta. De belangrijkste
verandering is dat Soundscape nu ook iets nuttigs te melden heeft wanneer u met de auto, de bus of
de trein reist, en niet langer alleen wanneer u te voet gaat. Daarnaast is er veel kleiner werk
verzet aan de manier waarop plaatsen worden beschreven, zijn er twintig nieuwe talen bijgekomen en
is er een lange lijst met oplossingen.

Opmerkingen bij oudere versies staan op de pagina
[Releaseopmerkingen voor 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nieuw in 2.0

* **Meldingen tijdens het reizen met auto, bus of trein.** Soundscape merkt op wanneer u zich met
  snelheid verplaatst en beschrijft uw reis in plaats van uw directe omgeving.
* **Melding wanneer u water en spoorlijnen kruist.** Rivieren, kanalen, zeearmen en spoorlijnen
  worden aangekondigd wanneer u ze oversteekt, zowel lopend als onderweg.
* **Betere adressen en plaatsnamen.** Plaatsen zonder eigen adres krijgen nu de straat en de buurt
  waarin ze liggen, huisnummers worden aan de juiste kant van de straat gekoppeld en bushaltes in
  Groot-Brittannië gebruiken hun officiële namen.
* **Twintig nieuwe talen**, waarmee het totaal op 46 komt. Ook deze documentatiewebsite is vertaald.
* **Wekken bij vertrek.** De slaapstand kan Soundscape nu weer wekken zodra u de plek verlaat waar u
  hem in slaap hebt gezet.
* **Kortere, natuurlijkere afstanden**, met grotere eenheden wanneer u zich snel verplaatst.
* **Sneller afsluiten.** *Soundscape afsluiten* staat nu bovenaan het hoofdmenu.
* **Verbeteringen aan offlinekaarten**, waaronder het bijwerken van een reeds gedownloade kaart en
  een kaart met de beschikbare regio's op deze website.
* **Veel toegankelijkheidswerk** aan TalkBack, vooral rond de introductieschermen.
* **Zeer veel oplossingen voor crashes en stabiliteit.**

Twee dingen zijn in 2.0 **verwijderd**: de spraakbediening en het taalmenu in de app. Zie
[Verwijderde onderdelen](#things-that-have-been-removed) hieronder voor wat u in plaats daarvan kunt
doen.

---

## Uitgebreider

### Reizen met auto, bus of trein

Dit is de grootste nieuwe functie voor bestaande gebruikers. Voorheen had Soundscape bijzonder
weinig te melden zodra u in een voertuig stapte: het bleef uw directe omgeving beschrijven, wat bij
snelheid neerkwam op een stroom van dingen waar u allang voorbij was.

Soundscape merkt nu op dat u sneller gaat dan wandeltempo en verandert wat het u vertelt. Er is
niets in te schakelen, en zodra u vaart mindert of uitstapt en gaat lopen, keert alles vanzelf terug
naar normaal.

Onderweg hoort u:

* **Waar u bent**, zo nu en dan: de weg waarop u zich bevindt en uw rijrichting, bijvoorbeeld
  «Noordwaarts over de M8». Genummerde wegen worden met hun nummer aangekondigd, en Soundscape
  herhaalt dezelfde weg niet telkens wanneer de straatnaam verandert.
* **Steden en dorpen** waar u naartoe rijdt, met de afstand, evenals plaatsen waar u vandaan gaat of
  waar u eenvoudigweg langs komt.
* **Knooppunten en afritten** zodra u ze bereikt.
* **Grote oriëntatiepunten** waar u langs komt, zoals parken, ziekenhuizen, stadions en
  winkelcentra.
* **Bus-, tram- en treinhaltes** waar u langs komt. Soundscape noemt alleen de haltes aan uw kant
  van de weg, omdat die aan de overkant de tegenovergestelde richting bedienen.
* **Rivieren, kanalen en spoorlijnen die u kruist.**
* **Tunnels**, wat vooral verklaart waarom Soundscape zo meteen stilvalt: daarbinnen is geen
  gps-signaal.

In een **trein** begrijpt Soundscape dat u zich op een spoorlijn bevindt en niet op een weg, en
vertelt het u langs welke plaatsen u komt en hoe ver u sinds het laatste station bent gekomen. Dat
uitzoeken is lastiger dan het klinkt, want snelwegen en spoorlijnen liggen vaak kilometers lang naast
elkaar. Een flink deel van het werk in deze release ging dan ook zitten in het niet verwisselen van
de een met de ander.

De gewone meldingen voor voetgangers — winkels in de buurt, oversteekplaatsen enzovoort — worden
tijdens het reizen bewust achtergehouden, en de afstanden waarop dingen worden aangekondigd zijn
flink vergroot, zodat u er iets over hoort voordat u er al voorbij bent.

### Water en spoorlijnen kruisen

Soundscape vertelt u nu wanneer u een rivier, kanaal, zeearm, baai of spoorlijn kruist. Dit werkt
zowel lopend als onderweg en geldt zowel voor eronderdoor als eroverheen, zodat een voetgangersbrug
en een tunneltje allebei worden beschreven.

### Betere adressen en plaatsnamen

Er is veel werk gestoken in het laten beschrijven van plaatsen zoals een mens dat zou doen:

* Plaatsen zonder eigen adres worden nu beschreven aan de hand van de straat en de buurt waarin ze
  liggen, in plaats van vaag te blijven.
* Huisnummers worden aan de juiste kant van de straat gekoppeld. Voorheen kon een adres vanaf de
  overkant van de stoep worden gemeld.
* Het adres van een plaats herhaalt de naam van die plaats niet meer.
* Bushaltes in Groot-Brittannië gebruiken hun officiële namen uit het openbaar vervoer, doorgaans
  die op de dienstregeling en op het bordje bij de halte.
* Naamloze voetpaden langs een rivier of kanaal worden nu genoemd naar het water dat ze volgen.
* Paden en wegen zonder naam worden zinniger beschreven, en de gebruikte woorden zijn netjes vertaald
  in plaats van in het Engels te verschijnen.

### Talen

In 2.0 zijn twintig nieuwe talen toegevoegd: Arabisch, Bengaals, Bulgaars, Catalaans, Kroatisch,
Tsjechisch, Hausa, Hongaars, Indonesisch, Koreaans, Marathi, Servisch, Slowaaks, Sloveens, Swahili,
Tamil, Telugu, Thai, Urdu en Vietnamees. Deze talen bevinden zich allemaal in het alfastadium en we
horen graag hoe accuraat ze zijn. In totaal is Soundscape nu in 46 talen beschikbaar, en ook deze
documentatiewebsite is vertaald.

Egyptisch Arabisch is opgegaan in het Arabisch en Luganda is teruggetrokken, omdat geen van beide
genoeg vertaalde tekst had om nuttig te zijn.

Vertalingen zijn gemeenschapswerk en we stellen uw hulp op prijs, of uw correcties waar iets slecht
leest. Elke tekst kan worden verbeterd op
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Slaapstand

De slaapstand heeft **wekken bij vertrek** gekregen. Wanneer u Soundscape in slaap zet, kunt u hem
vragen weer wakker te worden zodra u het gebied verlaat. Dat is handig wanneer u ergens aankomt en
rust wilt tot u weer vertrekt.

### Afstanden en spraak

Uitgesproken afstanden zijn korter en natuurlijker gemaakt, en Soundscape schakelt nu over op
grotere eenheden wanneer u snel gaat: mijlen of kilometers in plaats van een lange telling in voet
of meters. Elke taal bepaalt zelf hoe een gedeeltelijke afstand wordt uitgesproken, wat voorheen in
een Engels gevormd patroon was geperst.

### Offlinekaarten

Offlinekaarten kwamen met 1.0 en zijn sindsdien gestaag verbeterd:

* Een gedownloade kaart kan nu ter plekke worden bijgewerkt wanneer er een nieuwere versie is, via
  het detailscherm van het kaartgebied.
* Kaarten die niet bruikbaar zijn — bijvoorbeeld een beschadigde download — worden nu duidelijk
  gemarkeerd in plaats van stilzwijgend te falen.
* Downloads zijn betrouwbaarder, en het scherm toont wat er gebeurt terwijl de lijst met beschikbare
  kaarten wordt opgehaald, in plaats van een laadindicator over het hele scherm.
* Een voltooide download verschijnt pas als voltooid wanneer die werkelijk klaar is voor gebruik.
* Er is een [kaart met de beschikbare regio's]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  op deze website.

### Toegankelijkheid

Er is bijzonder veel werk verzet aan het gedrag van schermlezers, vooral in de introductieschermen
waar de focus voorheen naar de verkeerde plek sprong. Andere verbeteringen zijn een betere weergave
van bestandsgroottes en decimale getallen, correcte hints van het type «dubbeltik om...» in talen
die het werkwoord achteraan plaatsen, en zinnige hints waar er helemaal geen waren ingesteld.

### Menu's en navigatie

* **Soundscape afsluiten** is nu het eerste item in het hoofdmenu in plaats van ergens verderop.
* Het hoofdmenu laat aan één kant geen strook van het scherm meer zien, wat schermlezergebruikers
  een verwarrend extra tikgebied gaf.
* Het terugveeggebaar van het systeem slaat geen niveau meer over wanneer u door categorieën bladert
  in Plaatsen in de buurt.
* De *audiotutorial* heet nu **begeleide tutorial**.
* De instellingen zijn opgeruimd, en *Standaardwaarden herstellen* wist nu werkelijk alles.

### Stabiliteit

2.0 bevat een lange lijst opgeloste crashes en vastlopers, waaronder het vastlopen van de app op het
startscherm, vastlopers bij het herstellen van instellingen, crashes bij een beschadigde gedownloade
kaart, crashes bij het openen van routedetails vanaf het startscherm, crashes bij het wisselen van
taal en diverse problemen die automatisch via de Play Store werden gemeld. Ook het gedrag rond accu
en opstarten is robuuster gemaakt op telefoons die achtergrond-apps agressief afsluiten.

### Verwijderde onderdelen
{: #things-that-have-been-removed }

* **De spraakbediening** is verwijderd. Die werkte nooit betrouwbaar genoeg om te behouden, en de
  mediatoetsen op koptelefoons dekken grotendeels hetzelfde af — zie
  [Hulp bij het gebruik van mediatoetsen]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Het taalmenu in de app** is verdwenen. Soundscape volgt nu de taal die u op uw telefoon hebt
  ingesteld, wat de meeste mensen ook verwachtten. Wilt u die wijzigen, verander dan de taal van uw
  telefoon of stel in de telefooninstellingen een taal per app in, als die optie er is.

## Problemen aan ons melden

Klopt er iets niet, dan horen we dat graag. Mail de Help Desk op
<soundscapeAndroid@scottishtecharmy.support>, of vraag het op Slack als u STA-lid bent.

Was een melding onjuist of bleef die uit, dan helpt een opname van uw reis ons enorm: we kunnen die
opnieuw afspelen en precies zien waarmee Soundscape werkte. Instructies vindt u onder
[Een locatieopname voor foutopsporing aanleveren]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Een opmerking over de iPhone

Al het bovenstaande gaat over de Android-app, maar het is nuttig te weten waar de rest van het werk
in deze release naartoe ging. Soundscape draait nu ook op de iPhone, en beide apps worden gebouwd
vanuit dezelfde gedeelde code: dezelfde schermen, dezelfde formuleringen en dezelfde meldingen. Een
nieuwe functie zoals de reismeldingen hierboven verschijnt daardoor op allebei tegelijk in plaats
van twee keer geschreven te worden. Die gedeelde basis verklaart waarom 2.0 zo lang duurde, en zou
toekomstige releases op beide platforms sneller beschikbaar moeten maken. De iPhone-app is momenteel
op uitnodiging beschikbaar via TestFlight: vraag het op Slack als u STA-lid bent, of mail de Help
Desk.
