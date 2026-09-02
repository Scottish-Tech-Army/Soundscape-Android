---
title: Opombe ob izdaji
layout: page
nav_order: 5
has_toc: false
lang: sl
permalink: /release-notes.html
machine-translated: true
---

# Opombe ob izdaji

Soundscape 2.0 je velika izdaja in je trenutno v zaprti beta različici. Glavna sprememba je, da ima
Soundscape zdaj kaj koristnega povedati tudi, ko potujete z avtomobilom, avtobusom ali vlakom, in ne
le, ko hodite peš. Poleg tega je bilo opravljenega veliko manjšega dela pri tem, kako so opisani
kraji, dodanih je dvajset novih jezikov in dolg je seznam popravkov.

Opombe za starejše različice so na strani
[Opombe ob izdajah 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novosti v različici 2.0

* **Obvestila med potovanjem z avtomobilom, avtobusom ali vlakom.** Soundscape prepozna, da se
  premikate s hitrostjo, in opisuje vaše potovanje namesto neposredne okolice.
* **Obvestilo, ko prečkate vode in železniške proge.** Reke, kanali, zalivi in železniške proge se
  najavijo, ko jih prečkate — peš in med vožnjo.
* **Boljši naslovi in imena krajev.** Kraji brez lastnega naslova zdaj dobijo ulico in območje, na
  katerem so, hišne številke se pripišejo pravi strani ulice, avtobusna postajališča v Veliki
  Britaniji pa uporabljajo svoja uradna imena.
* **Dvajset novih jezikov**, skupaj torej 46. Prevedeno je tudi to dokumentacijsko spletno mesto.
* **Bujenje ob odhodu.** Način spanja lahko zdaj znova zbudi Soundscape, ko zapustite kraj, kjer ste
  ga uspavali.
* **Krajše, bolj naravne razdalje**, z večjimi enotami, ko se premikate hitro.
* **Hitrejši izhod.** *Izhod iz Soundscapea* je zdaj na vrhu glavnega menija.
* **Izboljšave zemljevidov brez povezave**, vključno s posodobitvijo že prenesenega zemljevida in
  zemljevidom razpoložljivih regij na tem spletnem mestu.
* **Veliko dela na dostopnosti** s TalkBackom, zlasti pri uvodnih zaslonih.
* **Zelo veliko popravkov sesutij in stabilnosti.**

V različici 2.0 sta bili **odstranjeni** dve stvari: glasovno upravljanje in meni jezika v aplikaciji.
Spodaj v razdelku [Odstranjene funkcije](#things-that-have-been-removed) piše, kaj lahko storite
namesto tega.

---

## Podrobneje

### Potovanje z avtomobilom, avtobusom ali vlakom

To je največja novost za obstoječe uporabnike. Prej je imel Soundscape zelo malo povedati, takoj ko
ste vstopili v vozilo: še naprej je opisoval vašo neposredno okolico, kar je pri hitrosti pomenilo tok
stvari, mimo katerih ste že zdavnaj peljali.

Soundscape zdaj opazi, da se premikate hitreje od hoje, in spremeni, kaj vam sporoča. Ničesar ni treba
vklopiti, vse pa se samo vrne v običajno stanje, takoj ko upočasnite ali izstopite in greste peš.

Med potovanjem boste slišali:

* **Kje ste**, občasno — cesto, po kateri vozite, in svojo smer, na primer »Vožnja proti severu po
  M8«. Ceste s številko se najavijo s svojo številko, Soundscape pa iste ceste ne ponavlja vsakič, ko
  se spremeni ime ulice.
* **Mesta in vasi**, proti katerim se peljete, z razdaljo, pa tudi tiste, od katerih se oddaljujete
  ali jih zgolj mimoidete.
* **Avtocestna vozlišča in izvoze**, ko jih dosežete.
* **Velike orientirje**, mimo katerih peljete, kot so parki, bolnišnice, stadioni in nakupovalna
  središča.
* **Avtobusna, tramvajska in železniška postajališča**, mimo katerih peljete. Soundscape omenja le
  postajališča na vaši strani ceste, saj tista na nasprotni strani služijo nasprotni smeri.
* **Reke, kanale in železniške proge, ki jih prečkate.**
* **Predore**, kar predvsem pojasnjuje, zakaj bo Soundscape kmalu utihnil — znotraj ni signala GPS.

Na **vlaku** Soundscape ugotovi, da ste na železniški progi in ne na cesti, ter vam pove, mimo katerih
krajev peljete in koliko ste prevozili od zadnje postaje. To je težje, kot se sliši, ker so avtoceste
in železniške proge pogosto zgrajene druga ob drugi kilometre daleč, zato je dober del dela v tej
izdaji šel v to, da se enega ne zamenja z drugim.

Običajna obvestila za pešce — bližnje trgovine, prehodi za pešce in tako naprej — so med potovanjem
namenoma zadržana, razdalje, na katerih se stvari najavijo, pa so precej povečane, da za nekaj izveste,
preden ste že peljali mimo.

### Prečkanje voda in železniških prog

Soundscape vam zdaj pove, ko prečkate reko, kanal, zaliv, zatok ali železniško progo. To deluje peš in
med vožnjo ter zajema tako prehod pod kot nad, zato sta opisana tako brv kot podhod.

### Boljši naslovi in imena krajev

Veliko dela je bilo vloženega v to, da Soundscape opisuje kraje tako, kot bi jih človek:

* Kraji brez lastnega naslova so zdaj opisani z ulico in območjem, na katerem so, namesto da ostanejo
  nedoločeni.
* Hišne številke se pripišejo pravi strani ulice. Prej je bilo mogoče naslov sporočiti z nasprotnega
  pločnika.
* Naslov kraja ne ponavlja več imena kraja samega.
* Avtobusna postajališča v Veliki Britaniji uporabljajo uradna imena javnega prevoza, običajno tista z
  voznega reda in z oznake na postajališču.
* Neimenovane poti, ki tečejo ob reki ali kanalu, so zdaj poimenovane po vodi, ki ji sledijo.
* Poti in ceste brez imena so opisane bolj smiselno, besede zanje pa so pravilno prevedene, namesto da
  se pojavljajo v angleščini.

### Jeziki

V različici 2.0 je bilo dodanih dvajset novih jezikov: arabščina, bengalščina, bolgarščina,
katalonščina, hrvaščina, češčina, havščina, madžarščina, indonezijščina, korejščina, maratščina,
srbščina, slovaščina, slovenščina, svahili, tamilščina, telugu, tajščina, urdujščina in vietnamščina.
Vsi ti jeziki so v fazi alfa in zelo si želimo povratnih informacij o njihovi točnosti. Skupno je
Soundscape zdaj na voljo v 46 jezikih, prevedeno pa je tudi to dokumentacijsko spletno mesto.

Egiptovska arabščina je bila združena z arabščino, luganda pa umaknjena, saj nobena ni imela dovolj
prevedenega besedila, da bi bila uporabna.

Prevodi so delo skupnosti in veseli bomo vaše pomoči ali popravkov tam, kjer se kaj slabo bere. Vsako
besedilo je mogoče izboljšati na
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Način spanja

Način spanja je dobil **bujenje ob odhodu**. Ko Soundscape uspavate, ga lahko prosite, naj se zbudi,
takoj ko zapustite območje. To je koristno, ko nekam prispete in želite mir, dokler znova ne krenete.

### Razdalje in govor

Izgovorjene razdalje so bile skrajšane in zvenijo bolj naravno, Soundscape pa zdaj preide na večje
enote, ko se premikate hitro — na milje ali kilometre namesto dolgega štetja v čevljih ali metrih.
Vsak jezik sam odloči, kako izgovoriti delno razdaljo, kar je bilo prej stisnjeno v angleško oblikovan
vzorec.

### Zemljevidi brez povezave

Zemljevidi brez povezave so prišli z različico 1.0 in se nenehno izboljšujejo:

* Prenesen zemljevid je zdaj mogoče posodobiti na mestu, ko je na voljo novejša različica, na zaslonu
  s podrobnostmi izseka.
* Zemljevidi, ki jih ni mogoče uporabiti — na primer poškodovan prenos — so zdaj jasno označeni,
  namesto da bi tiho odpovedali.
* Prenosi so zanesljivejši, zaslon pa kaže, kaj se dogaja med pridobivanjem seznama razpoložljivih
  zemljevidov, namesto kazalnika nalaganja čez cel zaslon.
* Končan prenos se kot končan prikaže šele, ko je res pripravljen za uporabo.
* Na tem spletnem mestu je
  [zemljevid razpoložljivih regij]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Dostopnost

Zelo veliko dela je bilo vloženega v vedenje bralnikov zaslona, zlasti na uvodnih zaslonih, kjer je
pozornost prej skočila na napačno mesto. Druge izboljšave vključujejo boljše branje velikosti datotek
in decimalnih števil, pravilne namige tipa »dvakrat se dotaknite za ...« v jezikih, ki glagol
postavljajo na konec, in smiselne namige tam, kjer jih sploh ni bilo.

### Meniji in navigacija

* **Izhod iz Soundscapea** je zdaj prva postavka glavnega menija, namesto da bi bila niže.
* Glavni meni ob strani ne pušča več vidnega pasu zaslona, kar je uporabnikom bralnikov zaslona dajalo
  zmedeno dodatno območje za dotik.
* Sistemska poteza za nazaj ne preskoči več ravni, ko brskate po kategorijah v Kraji v bližini.
* *Zvočni vodnik* se zdaj imenuje **vodeni vodnik**.
* Nastavitve so pospravljene, *Ponastavi na privzeto* pa zdaj pravilno počisti vse.

### Stabilnost

Različica 2.0 vključuje dolg seznam odpravljenih sesutij in zamrznitev, med njimi zamrznitev aplikacije
na začetnem zaslonu, zamrznitve pri ponastavljanju nastavitev, sesutja ob poškodovanem prenesenem
zemljevidu, sesutja pri odpiranju podrobnosti poti z domačega zaslona, sesutja ob zamenjavi jezika in
več težav, samodejno sporočenih prek trgovine Play. Vedenje glede baterije in zagona je bilo prav tako
utrjeno na telefonih, ki agresivno zapirajo aplikacije v ozadju.

### Odstranjene funkcije
{: #things-that-have-been-removed }

* **Glasovno upravljanje** je bilo odstranjeno. Nikoli ni delovalo dovolj zanesljivo, da bi ga bilo
  vredno obdržati, predstavnostne tipke na slušalkah pa pokrivajo večinoma isto — glejte
  [Pomoč pri uporabi predstavnostnih tipk]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Meni jezika v aplikaciji** je izginil. Soundscape zdaj sledi jeziku, nastavljenemu na vašem
  telefonu, kar je večina ljudi tako ali tako pričakovala. Če ga želite spremeniti, spremenite jezik
  telefona ali v njegovih nastavitvah določite jezik za posamezno aplikacijo, če to omogoča.

## Kako nam sporočite težave

Če kaj ni v redu, bi radi izvedeli. Pišite na Help Desk na
<soundscapeAndroid@scottishtecharmy.support> ali vprašajte na Slacku, če ste član STA.

Če je bilo obvestilo napačno ali ga sploh ni bilo, nam posnetek vaše poti izjemno pomaga — lahko ga
predvajamo znova in natančno vidimo, s čim je Soundscape delal. Navodila so v razdelku
[Posredovanje posnetka lokacije za odpravljanje napak]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Opomba o iPhonu

Vse zgoraj navedeno se nanaša na aplikacijo za Android, a velja vedeti, kam je šel preostanek dela v
tej izdaji. Soundscape zdaj teče tudi na iPhonu, obe aplikaciji pa sta zgrajeni iz iste skupne kode —
isti zasloni, iste ubeseditve in ista obvestila. Novost, kot so zgornja potovalna obvestila, tako
pride na obe hkrati, namesto da bi bila napisana dvakrat. Ta skupna osnova je razlog, zakaj je 2.0
trajala tako dolgo, in prav ta naj bi poskrbela, da bodo prihodnje izdaje hitreje prišle na obe
platformi. Aplikacija za iPhone je trenutno na voljo prek TestFlighta na povabilo: vprašajte na
Slacku, če ste član STA, ali pišite na Help Desk.
