---
title: Kiadási megjegyzések
layout: page
nav_order: 5
has_toc: false
lang: hu
permalink: /release-notes.html
machine-translated: true
---

# Kiadási megjegyzések

A Soundscape 2.0 nagy kiadás, és jelenleg zárt bétaverzióban érhető el. A legfontosabb változás, hogy
a Soundscape mostantól akkor is tud valami hasznosat mondani, amikor autóval, busszal vagy vonattal
utazik, nem csupán gyaloglás közben. Emellett sok kisebb munka történt azon, ahogyan a helyeket
leírja, húsz új nyelv került be, és hosszú a javítások listája.

A korábbi verziók megjegyzései a
[Kiadási megjegyzések az 1.x-hez]({{ "/v1.0-release-notes.html" | relative_url }}) oldalon találhatók.

## Újdonságok a 2.0-ban

* **Bemondások autóval, busszal vagy vonattal utazás közben.** A Soundscape felismeri, ha sebességgel
  halad, és az utazását írja le a közvetlen környezete helyett.
* **Jelzés vizek és vasútvonalak keresztezésekor.** A folyókat, csatornákat, öblöket és
  vasútvonalakat bemondja, amikor áthalad rajtuk — gyalog és utazás közben egyaránt.
* **Jobb címek és helynevek.** A saját címmel nem rendelkező helyek mostantól megkapják az utcát és a
  területet, ahol vannak, a házszámok az utca megfelelő oldalához társulnak, a nagy-britanniai
  buszmegállók pedig a hivatalos nevüket használják.
* **Húsz új nyelv**, így összesen 46. Ez a dokumentációs webhely is lefordításra került.
* **Ébresztés távozáskor.** Az alvó mód mostantól fel tudja ébreszteni a Soundscape-et, amikor
  elhagyja azt a helyet, ahol alvó módba tette.
* **Rövidebb, természetesebb távolságok**, nagyobb mértékegységekkel, amikor gyorsan halad.
* **Gyorsabb kilépés.** A *Kilépés a Soundscape-ből* mostantól a főmenü tetején van.
* **Offline térkép fejlesztések**, köztük egy már letöltött térkép helyben történő frissítése és az
  elérhető régiók térképe ezen a webhelyen.
* **Sok akadálymentesítési munka** a TalkBackkel, különösen a bevezető képernyők körül.
* **Nagyon sok összeomlás- és stabilitási javítás.**

A 2.0-ban két dolgot **eltávolítottunk**: a hangvezérlést és az alkalmazáson belüli nyelvi menüt.
Hogy mit tehet helyettük, arról lentebb, az
[Eltávolított funkciók](#things-that-have-been-removed) részben olvashat.

---

## Részletesebben

### Utazás autóval, busszal vagy vonattal

Ez a legnagyobb újdonság a meglévő felhasználók számára. Korábban a Soundscape-nek nagyon kevés
mondanivalója volt, amint beült egy járműbe: továbbra is a közvetlen környezetét írta le, ami
sebességnél olyan dolgok áradatát jelentette, amelyeken már rég túlhaladt.

A Soundscape most észreveszi, hogy gyorsabban halad a gyalogos tempónál, és megváltoztatja, amit
mond. Nincs mit bekapcsolni, és minden magától visszaáll a megszokottra, amint lassít, vagy kiszáll
és gyalogol.

Utazás közben ezeket fogja hallani:

* **Hol tart**, időnként — az utat, amelyen halad, és az irányát, például „Észak felé az M8-on”. A
  számozott utakat a számukkal mondja be, és a Soundscape nem ismétli meg ugyanazt az utat minden
  alkalommal, amikor az utcanév megváltozik.
* **Városok és falvak**, amelyek felé tart, a távolsággal együtt, valamint azok, amelyektől
  távolodik, vagy amelyeket egyszerűen elhagy.
* **Autópálya-csomópontok és -lehajtók**, amint eléri őket.
* **Nagy tájékozódási pontok**, amelyek mellett elhalad, például parkok, kórházak, stadionok és
  bevásárlóközpontok.
* **Busz-, villamos- és vasúti megállók**, amelyek mellett elhalad. A Soundscape csak az Ön oldalán
  lévő megállókat említi, mivel a túloldaliak az ellenkező irányt szolgálják ki.
* **Folyók, csatornák és vasútvonalak, amelyeket keresztez.**
* **Alagutak**, ami főként azt magyarázza, miért fog a Soundscape elhallgatni — odabent nincs
  GPS-jel.

**Vonaton** a Soundscape felismeri, hogy vasúton van, nem úton, és megmondja, mely települések mellett
halad el, és mekkora utat tett meg az utolsó állomás óta. Ezt kitalálni nehezebb, mint amilyennek
hangzik, mert az autópályákat és a vasútvonalakat gyakran kilométereken át egymás mellé építik, így a
kiadás munkájának jó része arra ment el, hogy ne keverje össze a kettőt.

A szokásos gyalogos bemondásokat — közeli boltok, gyalogátkelők és így tovább — szándékosan
visszatartja utazás közben, és a távolságok, amelyeken a dolgokat bemondja, jelentősen megnőttek,
hogy azelőtt értesüljön valamiről, mielőtt elhaladna mellette.

### Vizek és vasútvonalak keresztezése

A Soundscape mostantól szól, amikor folyót, csatornát, öblöt, tengeröblöt vagy vasútvonalat
keresztez. Ez gyalog és utazás közben egyaránt működik, és az alatta, valamint a fölötte való áthaladást
is lefedi, így a gyalogoshidat és az aluljárót is leírja.

### Jobb címek és helynevek

Sok munka ment abba, hogy a Soundscape úgy írja le a helyeket, ahogyan egy ember tenné:

* A saját címmel nem rendelkező helyeket mostantól az utca és a terület alapján írja le, ahol vannak,
  ahelyett hogy homályosak maradnának.
* A házszámok az utca megfelelő oldalához társulnak. Korábban előfordult, hogy egy címet a szemközti
  járdáról jelentett.
* Egy hely címe már nem ismétli meg magának a helynek a nevét.
* A nagy-britanniai buszmegállók a hivatalos tömegközlekedési nevüket használják, jellemzően azokat,
  amelyek a menetrendben és a megálló tábláján szerepelnek.
* A folyó vagy csatorna mentén futó, névtelen gyalogutakat mostantól arról a vízről nevezi el,
  amelyet követnek.
* A név nélküli ösvényeket és utakat értelmesebben írja le, és a rájuk használt szavak rendesen le
  vannak fordítva, ahelyett hogy angolul jelennének meg.

### Nyelvek

A 2.0-ban húsz új nyelv került be: arab, bengáli, bolgár, katalán, horvát, cseh, hausza, magyar,
indonéz, koreai, maráthi, szerb, szlovák, szlovén, szuahéli, tamil, telugu, thai, urdu és vietnámi.
Ezek a nyelvek mind alfa állapotban vannak, és nagyon várjuk a visszajelzéseket a pontosságukról.
Összesen a Soundscape mostantól 46 nyelven érhető el, és ez a dokumentációs webhely is lefordításra
került.

Az egyiptomi arab beolvadt az arabba, a luganda pedig visszavonásra került, mivel egyiknek sem volt
elég lefordított szövege ahhoz, hogy hasznos legyen.

A fordítások közösségi munka, és szívesen fogadjuk a segítségét vagy javításait ott, ahol valami
rosszul olvasható. Bármely szöveg javítható a
<https://hosted.weblate.org/projects/soundscape-android/android-app/> címen.

### Alvó mód

Az alvó mód megkapta az **ébresztés távozáskor** funkciót. Amikor alvó módba teszi a Soundscape-et,
megkérheti, hogy ébredjen fel, amint elhagyja a területet. Ez akkor hasznos, amikor megérkezik
valahová, és csendet szeretne, amíg újra el nem indul.

### Távolságok és beszéd

A kimondott távolságok rövidebbek és természetesebbek lettek, és a Soundscape mostantól nagyobb
mértékegységekre vált, amikor gyorsan halad — mérföldre vagy kilométerre a lábban vagy méterben való
hosszas számolás helyett. Minden nyelv maga dönti el, hogyan mondja ki a tört távolságot, ami korábban
angol formájú mintába volt kényszerítve.

### Offline térképek

Az offline térképek az 1.0-val érkeztek, és folyamatosan fejlődtek:

* Egy letöltött térkép mostantól helyben frissíthető, ha újabb verzió érhető el, a kivonat adatlapjáról.
* A nem használható térképek — például egy sérült letöltés — mostantól egyértelműen meg vannak jelölve,
  ahelyett hogy csendben hibáznának.
* A letöltések megbízhatóbbak, és a képernyő megmutatja, mi történik, miközben az elérhető térképek
  listája betöltődik, teljes képernyős töltésjelző helyett.
* Egy befejezett letöltés csak akkor jelenik meg befejezettként, amikor valóban használatra kész.
* Ezen a webhelyen elérhető az
  [elérhető régiók térképe]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Akadálymentesség

Rendkívül sok munka ment a képernyőolvasók viselkedésébe, különösen a bevezető képernyőkön, ahol a
fókusz korábban rossz helyre ugrott. További fejlesztések: a fájlméretek és tizedes számok jobb
felolvasása, helyes „koppintson duplán a...” súgók az igét a végére helyező nyelvekben, valamint
értelmes súgók ott, ahol egyáltalán nem voltak beállítva.

### Menük és navigáció

* A **Kilépés a Soundscape-ből** mostantól a főmenü első eleme, nem pedig lejjebb található.
* A főmenü már nem hagy látszani egy sávot a képernyő szélén, ami a képernyőolvasót használóknak
  zavaró további koppintási területet adott.
* A rendszer vissza mozdulata már nem ugrik át egy szintet, amikor a Közeli helyek kategóriái között
  böngészik.
* A *hangos oktatóanyag* neve **vezetett oktatóanyag** lett.
* A beállítások rendezettebbek lettek, és az *Alaphelyzetbe állítás* mostantól helyesen töröl mindent.

### Stabilitás

A 2.0 hosszú listát tartalmaz a javított összeomlásokból és lefagyásokból, köztük az alkalmazás
lefagyása az indítóképernyőn, lefagyások a beállítások visszaállításakor, összeomlások sérült
letöltött térkép esetén, összeomlások az útvonal részleteinek a kezdőképernyőről való megnyitásakor,
összeomlások nyelvváltáskor, valamint több, a Play Áruházon keresztül automatikusan jelentett
probléma. Az akkumulátorral és az indulással kapcsolatos viselkedés is stabilabb lett azokon a
telefonokon, amelyek agresszíven zárják be a háttéralkalmazásokat.

### Eltávolított funkciók
{: #things-that-have-been-removed }

* **A hangvezérlést** eltávolítottuk. Soha nem működött elég megbízhatóan ahhoz, hogy megérje
  megtartani, és a fejhallgatók médiagombjai nagyrészt ugyanazt fedik le — lásd a
  [Médiavezérlők használatához nyújtott súgót]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Az alkalmazáson belüli nyelvi menü** megszűnt. A Soundscape mostantól a telefonján beállított
  nyelvet követi, amit a legtöbben amúgy is vártak. A módosításhoz változtassa meg a telefon nyelvét,
  vagy állítson be alkalmazásonkénti nyelvet a telefon beállításaiban, ha kínál ilyet.

## Hogyan jelezze a problémákat

Ha valami nem stimmel, szívesen hallunk róla. Írjon a Help Desknek a
<soundscapeAndroid@scottishtecharmy.support> címre, vagy kérdezzen a Slacken, ha STA-tag.

Ha egy bemondás hibás volt vagy elmaradt, az útjáról készült felvétel óriási segítség — le tudjuk
játszani, és pontosan látjuk, mivel dolgozott a Soundscape. Az útmutató a
[Helyzetrögzítés biztosítása hibakereséshez]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace)
részben található.

## Megjegyzés az iPhone-ról

A fentiek mind az Android-alkalmazásról szólnak, de érdemes tudni, hová ment a kiadás munkájának
többi része. A Soundscape mostantól iPhone-on is fut, és mindkét alkalmazás ugyanabból a megosztott
kódból épül — ugyanazok a képernyők, ugyanazok a megfogalmazások és ugyanazok a bemondások. Egy
újdonság, mint a fenti utazási bemondások, így egyszerre érkezik mindkettőre, ahelyett hogy kétszer
kellene megírni. Ez a közös alap a magyarázata annak, miért tartott ilyen sokáig a 2.0, és ennek
köszönhetően a jövőbeli kiadásoknak gyorsabban kell megérkezniük mindkét platformra. Az iPhone-os
alkalmazás jelenleg TestFlighten keresztül, meghívásos alapon érhető el: kérdezzen a Slacken, ha
STA-tag, vagy írjon a Help Desknek.
