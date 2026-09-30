---
title: Poznámky k vydaniu
layout: page
nav_order: 5
has_toc: false
lang: sk
permalink: /release-notes.html
machine-translated: true
---

# Poznámky k vydaniu

Soundscape 2.0 je veľké vydanie a momentálne je v uzavretej beta verzii. Hlavnou zmenou je, že
Soundscape má teraz čo užitočné povedať aj vtedy, keď cestujete autom, autobusom alebo vlakom, a nie
len keď idete pešo. Pribudlo aj množstvo menších úprav v tom, ako sa opisujú miesta, dvadsať nových
jazykov a dlhý zoznam opráv.

Poznámky k starším verziám nájdete na stránke
[Poznámky k vydaniu pre 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novinky vo verzii 2.0

* **Hlásenia počas cesty autom, autobusom alebo vlakom.** Soundscape rozpozná, že sa pohybujete
  rýchlosťou, a opisuje vašu cestu namiesto bezprostredného okolia.
* **Upozornenie pri prekonávaní vodných tokov a železníc.** Rieky, kanály, zálivy a železničné trate
  sa ohlasujú, keď ich prekračujete — pešo aj počas jazdy.
* **Zvoľte, koľko toho Soundscape hovorí.** Nové nastavenie *Podrobnosť hlásení* stíši Soundscape na
  rušných miestach a v *Miestach na ohlasovanie* si vyberiete, o akých druhoch miest chcete počuť.
  Podrobnosť môžete meniť tlačidlami na slúchadlách aj počas chôdze.
* **Viete, ako ďaleko je ďalšia križovatka.** Križovatky sa ohlasujú v stálej vzdialenosti, keď sa k
  nim blížite, a hlásenie teraz hovorí, ako ďaleko je obrubník.
* **Hľadajte druh miesta alebo súradnice.** Vyhľadajte „lekáreň“ alebo „autobusová zastávka“ a
  nájdete tie najbližšie, nech sa volajú akokoľvek, alebo vložte súradnice, odkaz na mapu či Plus
  Code.
* **Otvorte miesto v inej mapovej aplikácii**, napríklad v Mapách Google, z podrobností o mieste alebo
  zo zoznamov.
* **Viac z majáka na domovskej obrazovke.** Teraz ukazuje vzdialenosť a smer a má akcie pre čítačku
  obrazovky, ktorými maják ohlásite, zistíte o ňom viac alebo ho uložíte ako značku.
* **Lepšie adresy a názvy miest.** Miesta bez vlastnej adresy teraz dostávajú ulicu a oblasť, v
  ktorej ležia, súpisné čísla sa priraďujú k správnej strane ulice a autobusové zastávky vo Veľkej
  Británii používajú svoje oficiálne názvy.
* **Dvadsať nových jazykov**, teda spolu 46. Preložený bol aj tento dokumentačný web.
* **Prebudenie pri odchode.** Režim spánku dokáže teraz Soundscape opäť prebudiť, keď opustíte
  miesto, kde ste ho uspali.
* **Kratšie, prirodzenejšie vzdialenosti**, s väčšími jednotkami, keď sa pohybujete rýchlo.
* **Rýchlejšia cesta von.** *Ukončiť Soundscape* je teraz na začiatku hlavnej ponuky.
* **Vylepšenia offline máp**, vrátane aktualizácie už stiahnutej mapy a mapy dostupných oblastí na
  tomto webe.
* **Veľa práce na prístupnosti** s TalkBackom, najmä pri úvodných obrazovkách.
* **Veľmi veľa opráv pádov a stability.**

Vo verzii 2.0 boli **odstránené** dve veci: hlasové ovládanie a ponuka jazyka vnútri aplikácie. Čo
robiť namiesto toho, nájdete nižšie v časti
[Odstránené funkcie](#things-that-have-been-removed).

---

## Podrobnejšie

### Cestovanie autom, autobusom alebo vlakom

Ide o najväčšiu novinku pre existujúcich používateľov. Predtým mal Soundscape veľmi málo čo povedať,
len čo ste nasadli do vozidla: naďalej opisoval vaše bezprostredné okolie, čo pri rýchlosti znamenalo
prúd vecí, okolo ktorých ste už dávno prešli.

Soundscape teraz spozná, že sa pohybujete rýchlejšie ako chôdzou, a mení to, čo vám hovorí. Nie je
potrebné nič zapínať a všetko sa samo vráti do normálu, len čo spomalíte alebo vystúpite a idete pešo.

Počas cesty budete počuť:

* **Kde ste**, čas od času — cestu, po ktorej idete, a smer jazdy, napríklad „Jazda na sever po M8“.
  Cesty s číslom sa ohlasujú svojím číslom a Soundscape neopakuje tú istú cestu vždy, keď sa zmení
  názov ulice.
* **Mestá a dediny**, ku ktorým smerujete, so vzdialenosťou, ako aj tie, od ktorých sa vzďaľujete
  alebo ktoré len míňate.
* **Diaľničné križovatky a zjazdy**, keď k nim prídete.
* **Veľké orientačné body**, ktoré míňate, napríklad parky, nemocnice, štadióny a nákupné centrá.
* **Autobusové, električkové a vlakové zastávky**, ktoré míňate. Soundscape spomína len zastávky na
  vašej strane cesty, keďže tie na protiľahlej strane slúžia opačnému smeru.
* **Rieky, kanály a železnice, ktoré prekračujete.**
* **Tunely**, čo hlavne vysvetľuje, prečo Soundscape o chvíľu stíchne — vnútri nie je signál GPS.

Vo **vlaku** Soundscape rozpozná, že ste na železnici, a nie na ceste, a povie vám, okolo akých obcí
prechádzate a akú vzdialenosť ste prešli od poslednej stanice. Zistiť to je ťažšie, ako to znie,
pretože diaľnice a železničné trate sa často stavajú vedľa seba celé kilometre, takže značná časť
práce v tomto vydaní smerovala k tomu, aby sa jedno nezamieňalo za druhé.

Bežné hlásenia pre chodcov — obchody v okolí, priechody a tak ďalej — sú počas jazdy zámerne zadržané
a vzdialenosti, na ktorých sa veci ohlasujú, boli výrazne predĺžené, aby ste sa o niečom dozvedeli
skôr, než to miniete.

### Križovatky

Najčastejšia otázka k hláseniu križovatiek bola, ako ďaleko tá križovatka vlastne je. Soundscape vám
to teraz povie: „Križovatka vzdialená 30 metrov“. Vzdialenosť sa meria k obrubníku ulice, ktorú sa
chystáte prejsť, nie k stredu križovatky, pretože tam sa naozaj zastavíte.

Hlásenie tiež prichádza na stálejšom mieste. Predtým mohlo zaznieť 45 metrov vopred alebo 10 metrov
vopred a nedalo sa to rozlíšiť. Teraz počká, kým je križovatka asi 30 metrov ďaleko, takže
vzdialenosť znamená zakaždým zhruba to isté.

### Prekonávanie vodných tokov a železníc

Soundscape vám teraz povie, keď prekračujete rieku, kanál, záliv, zátoku alebo železničnú trať.
Funguje to pešo aj počas jazdy a zahŕňa prechod popod aj ponad, takže sa opíše aj lávka, aj podchod.

### Zvoľte, koľko toho Soundscape hovorí

Najčastejšie o Soundscape počúvame, že na rušných miestach, ako je centrum mesta, hovorí príliš veľa.
Časť *Spravovať hlásenia* v *Nastaveniach* má teraz namiesto starého zoznamu prepínačov tri
nastavenia:

* **Podrobnosť hlásení** je Bez zvuku, Základný, Zjednodušený alebo Podrobný. *Podrobný* je to, čo
  Soundscape robil vždy, a je predvolený. *Zjednodušený* vynecháva menšie cesty a obslužné komunikácie a
  menej sa opakuje. *Základný* ohlasuje len ulice, križovatky a orientačné body. *Bez zvuku* nerobí
  žiadne automatické hlásenia, zatiaľ čo majáky, trasy a tlačidlá na domovskej obrazovke ďalej
  fungujú. Nahrádza starý prepínač *Povoliť hlásenia*; ak ste ho mali vypnutý, nájdete Podrobnosť
  hlásení nastavenú na Bez zvuku.
* **Ulice a križovatky** zapína alebo vypína hlásenia o križovatkách a o ulici, na ktorej ste.
* **Miesta na ohlasovanie** je zoznam na zaškrtnutie: Všetko, Orientačné body, Verejná doprava, Jedlo
  a nápoje, Potraviny a obchody so zmiešaným tovarom, Banky a bankomaty alebo Žiadne miesta.
  Zaškrtnite ich, koľko chcete, napríklad orientačné body a autobusové zastávky. Vaše značky sa
  ohlasujú vždy.

Vhodná podrobnosť sa počas chôdze mení, takže kvôli nej nemusíte chodiť do Nastavení. Stlačením
*Predchádzajúce* na slúchadlách znížite podrobnosť hlásení vždy o jeden stupeň, z Podrobného cez
Zjednodušený a Základný na Bez zvuku a potom späť na Podrobný. Nový stupeň sa zakaždým ohlási. Funguje to v
oboch režimoch ovládania médií, a preto sa tlačidlá slúchadiel trochu zmenili:

* V *Pôvodnom režime* teraz *Ďalej* ohlási *Okolo mňa*, keď sa neprehráva žiadna trasa, a *Moja
  poloha* už na tlačidlách nie je. Počas prehrávania trasy *Ďalej* a *Predchádzajúce* ďalej
  prechádzajú medzi bodmi trasy.
* V režime *Zvukové menu* sa *Predchádzajúce* už v menu nevracia späť. *Ďalej* ním ďalej prechádza a
  *Prehrať/Pozastaviť* ďalej vyberá. Značky a trasy sú teraz v menu zoradené podľa názvu a po spustení
  jednej z nich sa menu vráti na začiatok, namiesto aby vás nechalo hlboko v zozname.

### Vyhľadávanie

Vyhľadávacie pole teraz rozumie viac než len názvom miest:

* **Druhy miest.** Vyhľadajte vo svojom jazyku „lekáreň“, „toaleta“, „bankomat“ a podobne a
  Soundscape vypíše najbližšie miesta tohto druhu, nech sa volajú akokoľvek. Miesta bez názvu, ako
  väčšina toaliet a lavičiek, sú uvedené podľa toho, čím sú, aj s adresou.
* **Súradnice, odkazy na mapy a Plus Codes.** Vložte dvojicu čísel, stupne a minúty, odkaz z Máp
  Google, Máp Apple alebo OpenStreetMap, prípadne Plus Code, a Soundscape vám dá presne to miesto.
  Samotnú dvojicu čísel možno čítať oboma smermi, takže keď dávajú zmysel oba, dostanete oba, bližší
  prvý.
* **Vyhľadávanie offline.** Vyhľadávanie sa teraz vždy pozrie aj do stiahnutých máp, nielen online, a
  nájde tak oveľa viac miest bez názvu. Ak hľadáte bez pripojenia na internet a nemáte offline mapu
  miesta, kde ste, Soundscape vám to povie, namiesto toho, aby jednoducho nič nenašiel.

### Otvorenie miesta v inej aplikácii

Podrobnosti o mieste majú nové tlačidlo **Otvoriť v mapovej aplikácii**, ktoré vypíše mapové a
navigačné aplikácie vo vašom telefóne. Zaškrtnite *Vždy používať túto aplikáciu* a tlačidlo sa zmení
napríklad na *Otvoriť v aplikácii Mapy Google* a otvorí ju hneď; dlhé stlačenie vráti zoznam.
Zoznamy *Miesta v okolí* a *Značky* majú aj akcie pre čítačku obrazovky *Otvoriť v aplikácii…* a
*Zdieľať*, vedľa *Spustiť zvukový maják*.

### Maják a značky

* Maják na domovskej obrazovke teraz ukazuje svoju **vzdialenosť a smer** a čítačka obrazovky ho
  prečíta napríklad ako „Maják pri Milngavie Library, 390 metrov, juhovýchod“. Trasy rovnako ukazujú
  vzdialenosť k aktuálnemu bodu trasy.
* Maják má tri **akcie pre čítačku obrazovky**: *Ohlásiť maják* povie, kde je, *Ďalšie informácie*
  pridá adresu a *Pridať do značiek* ho uloží.
* **Značku môžete znova presunúť** ťahaním mapy na obrazovke *Upraviť značku*.

### Lepšie adresy a názvy miest

Veľa práce bolo vložené do toho, aby Soundscape opisoval miesta tak, ako by to urobil človek:

* Miesta bez vlastnej adresy sa teraz opisujú ulicou a oblasťou, v ktorej ležia, namiesto toho, aby
  ostali neurčité.
* Súpisné čísla sa priraďujú k správnej strane ulice. Predtým mohla byť adresa ohlásená z protiľahlého
  chodníka.
* Adresa miesta už neopakuje názov samotného miesta.
* Autobusové zastávky vo Veľkej Británii používajú oficiálne názvy verejnej dopravy, zvyčajne tie z
  cestovného poriadku a z označníka zastávky.
* Nepomenované chodníky vedúce popri rieke alebo kanáli sa teraz pomenúvajú podľa vody, ktorú
  sledujú.
* Cesty a chodníky bez názvu sa opisujú rozumnejšie a slová použité pre ne sú riadne preložené
  namiesto toho, aby sa objavovali po anglicky.

### Jazyky

Vo verzii 2.0 pribudlo dvadsať nových jazykov: arabčina, bengálčina, bulharčina, katalánčina,
chorvátčina, čeština, hauština, maďarčina, indonézština, kórejčina, maráthčina, srbčina, slovenčina,
slovinčina, swahilčina, tamilčina, telugčina, thajčina, urdčina a vietnamčina. Všetky tieto jazyky sú
vo fáze alfa a veľmi nám záleží na spätnej väzbe k ich presnosti. Celkovo je teraz Soundscape
dostupný v 46 jazykoch a preložený bol aj tento dokumentačný web.

Egyptská arabčina bola zlúčená s arabčinou a lugandčina bola stiahnutá, keďže ani jedna nemala dosť
preloženého textu, aby bola užitočná.

Preklady sú dielom komunity a radi privítame vašu pomoc alebo opravy tam, kde sa niečo číta zle.
Akýkoľvek text možno vylepšiť na
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Režim spánku

Režim spánku získal **prebudenie pri odchode**. Keď Soundscape uspíte, môžete ho požiadať, aby sa
prebudil, len čo opustíte oblasť. Hodí sa to, keď niekam prídete a chcete pokoj, kým sa opäť
nevydáte na cestu.

### Vzdialenosti a reč

Vyslovované vzdialenosti boli skrátené a znejú prirodzenejšie a Soundscape teraz prechádza na väčšie
jednotky, keď sa pohybujete rýchlo — míle alebo kilometre namiesto dlhého počítania v stopách či
metroch. Každý jazyk sám rozhoduje, ako vysloviť zlomkovú vzdialenosť, čo bolo predtým vtesnané do
anglicky utvoreného vzorca.

### Offline mapy

Offline mapy prišli s verziou 1.0 a sústavne sa vylepšujú:

* Stiahnutú mapu možno teraz aktualizovať na mieste, keď je k dispozícii novšia verzia, z obrazovky
  s podrobnosťami výrezu.
* Mapy, ktoré sa nedajú použiť — napríklad poškodené stiahnutie — sú teraz zreteľne označené namiesto
  toho, aby zlyhali potichu.
* Sťahovanie je spoľahlivejšie a obrazovka ukazuje, čo sa deje, kým sa načítava zoznam dostupných
  máp, namiesto indikátora cez celú obrazovku.
* Dokončené sťahovanie sa ako dokončené zobrazí až vtedy, keď je naozaj pripravené na použitie.
* Na tomto webe je
  [mapa dostupných oblastí]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Prístupnosť

Veľké množstvo práce bolo venované správaniu čítačiek obrazovky, najmä na úvodných obrazovkách, kde
zameranie predtým skákalo na nesprávne miesto. Medzi ďalšie vylepšenia patrí lepšie čítanie veľkostí
súborov a desatinných čísel, správne pokyny typu „dvojitým ťuknutím...“ v jazykoch, ktoré kladú
sloveso na koniec, a zmysluplné pokyny tam, kde neboli nastavené žiadne.

### Ponuky a navigácia

* **Ukončiť Soundscape** je teraz prvou položkou hlavnej ponuky namiesto toho, aby bola niekde nižšie.
* Hlavná ponuka už nenecháva po strane vidieť pruh obrazovky, ktorý používateľom čítačiek dával
  mätúcu ďalšiu plochu na ťuknutie.
* Systémové gesto späť už nepreskakuje úroveň, keď prechádzate kategórie v Miestach v okolí.
* *Zvukový sprievodca* bol premenovaný na **riadeného sprievodcu**.
* Nastavenia boli upratané a *Obnoviť predvolené hodnoty* teraz správne vymaže všetko.

### Stabilita

Verzia 2.0 obsahuje dlhý zoznam opravených pádov a zamrznutí, okrem iného zamrznutie aplikácie na
úvodnej obrazovke, zamrznutia pri obnovovaní nastavení, pády pri poškodenej stiahnutej mape, pády pri
otváraní podrobností trasy z domovskej obrazovky, pády pri zmene jazyka a niekoľko problémov hlásených
automaticky cez Obchod Play. Správanie ohľadom batérie a spúšťania bolo tiež spevnené na telefónoch,
ktoré agresívne ukončujú aplikácie na pozadí.

### Odstránené funkcie
{: #things-that-have-been-removed }

* **Hlasové ovládanie** bolo odstránené. Nikdy nefungovalo dosť spoľahlivo na to, aby stálo za
  zachovanie, a multimediálne tlačidlá na slúchadlách pokrývajú z veľkej časti to isté — pozrite
  [Pomocníka k používaniu multimediálnych tlačidiel]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape je tiež pripravený na hlasové príkazy cez Gemini v Androide 16 a novšom, tie však
  nebudú fungovať, kým Google ich podporu v Gemini nezverejní.
* **Ponuka jazyka vnútri aplikácie** zmizla. Soundscape sa teraz riadi jazykom nastaveným v telefóne,
  čo väčšina ľudí očakávala. Ak ho chcete zmeniť, zmeňte jazyk telefónu alebo v jeho nastaveniach
  určte jazyk pre jednotlivú aplikáciu, ak to ponúka.

## Ako nám nahlásiť problém

Ak niečo nie je v poriadku, radi sa to dozvieme. Napíšte na Help Desk na adresu
<soundscapeAndroid@scottishtecharmy.support> alebo sa opýtajte na Slacku, ak ste členom STA.

Ak bolo hlásenie chybné alebo neprišlo, záznam vašej cesty nám nesmierne pomôže — môžeme ho prehrať a
presne vidieť, z čoho Soundscape vychádzal. Pokyny nájdete v časti
[Poskytnutie záznamu polohy na ladenie]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Poznámka k iPhonu

Všetko vyššie uvedené sa týka aplikácie pre Android, ale oplatí sa vedieť, kam šiel zvyšok práce v
tomto vydaní. Soundscape teraz beží aj na iPhone a obe aplikácie sú zostavené z rovnakého zdieľaného
kódu — rovnaké obrazovky, rovnaké formulácie a rovnaké hlásenia. Novinka ako vyššie opísané cestovné
hlásenia sa tak dostane do oboch naraz, namiesto toho, aby sa písala dvakrát. Tento spoločný základ
je dôvodom, prečo verzia 2.0 trvala tak dlho, a mal by zabezpečiť, aby budúce vydania prichádzali
rýchlejšie na obe platformy. Aplikácia pre iPhone je momentálne dostupná cez TestFlight na pozvanie:
opýtajte sa na Slacku, ak ste členom STA, alebo napíšte na Help Desk.
