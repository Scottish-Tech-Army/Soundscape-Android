---
title: Poznámky k vydání
layout: page
nav_order: 5
has_toc: false
lang: cs
permalink: /release-notes.html
machine-translated: true
---

# Poznámky k vydání

Soundscape 2.0 je velké vydání a nachází se v současnosti v uzavřené beta verzi. Hlavní změnou je,
že Soundscape má nyní co užitečného říci i tehdy, když cestujete autem, autobusem nebo vlakem, a
nejen když jdete pěšky. Přibyla také spousta drobnější práce na tom, jak jsou popisována místa,
dvacet nových jazyků a dlouhý seznam oprav.

Poznámky ke starším verzím najdete na stránce
[Poznámky k vydání pro 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novinky ve verzi 2.0

* **Hlášení při cestě autem, autobusem nebo vlakem.** Soundscape pozná, že se pohybujete rychlostí,
  a popisuje vaši cestu místo bezprostředního okolí.
* **Upozornění při překonávání vodních toků a železnic.** Řeky, kanály, zálivy a železniční tratě
  jsou ohlašovány, když je překračujete — pěšky i za jízdy.
* **Zvolte, kolik toho Soundscape říká.** Nové nastavení *Podrobnost hlášení* ztiší Soundscape na
  rušných místech a v *Místech k ohlašování* si vyberete, o jakých druzích míst chcete slyšet.
  Podrobnost můžete měnit tlačítky na sluchátkách i za chůze.
* **Víte, jak daleko je další křižovatka.** Křižovatky se ohlašují ve stálé vzdálenosti, když se k
  nim blížíte, a hlášení teď říká, jak daleko je obrubník.
* **Hledejte druh místa nebo souřadnice.** Vyhledejte „lékárna“ nebo „autobusová zastávka“ a najdete
  ty nejbližší, ať se jmenují jakkoli, nebo vložte souřadnice, odkaz na mapu či Plus Code.
* **Otevřete místo v jiné mapové aplikaci**, třeba v Mapách Google, z podrobností o místě nebo ze
  seznamů.
* **Víc z majáku na domovské obrazovce.** Nyní ukazuje vzdálenost a směr a má akce pro čtečku
  obrazovky, kterými maják ohlásíte, zjistíte o něm víc nebo ho uložíte jako značku.
* **Lepší adresy a názvy míst.** Místa bez vlastní adresy nyní dostávají ulici a oblast, v níž leží,
  čísla popisná jsou přiřazena ke správné straně ulice a autobusové zastávky ve Velké Británii
  používají své oficiální názvy.
* **Dvacet nových jazyků**, celkem tedy 46. Přeložen byl i tento dokumentační web.
* **Probuzení při odchodu.** Režim spánku nyní může Soundscape opět probudit, když opustíte místo,
  kde jste jej uspali.
* **Kratší, přirozenější vzdálenosti**, s většími jednotkami, když se pohybujete rychle.
* **Rychlejší cesta ven.** *Ukončit Soundscape* je nyní na začátku hlavní nabídky.
* **Vylepšení offline map**, včetně aktualizace již stažené mapy a mapy dostupných oblastí na tomto
  webu.
* **Hodně práce na přístupnosti** s TalkBackem, zejména u úvodních obrazovek.
* **Velmi mnoho oprav pádů a stability.**

Ve verzi 2.0 byly **odstraněny** dvě věci: hlasové ovládání a nabídka jazyka uvnitř aplikace. Co
dělat místo toho, najdete níže v části
[Odstraněné funkce](#things-that-have-been-removed).

---

## Podrobněji

### Cestování autem, autobusem nebo vlakem

Jde o největší novinku pro stávající uživatele. Dříve měl Soundscape jen velmi málo co říci, jakmile
jste nasedli do vozidla: dál popisoval vaše bezprostřední okolí, což při rychlosti znamenalo proud
věcí, kolem kterých jste už dávno projeli.

Soundscape nyní pozná, že se pohybujete rychleji než chůzí, a mění to, co vám sděluje. Není třeba nic
zapínat a jakmile zpomalíte nebo vystoupíte a jdete pěšky, vše se samo vrátí do normálu.

Během cesty uslyšíte:

* **Kde jste**, čas od času — silnici, po níž jedete, a směr jízdy, například „Jízda na sever po
  M8“. Silnice s číslem jsou ohlašovány svým číslem a Soundscape neopakuje tutéž silnici pokaždé,
  když se změní název ulice.
* **Města a vesnice**, k nimž míříte, se vzdáleností, i ty, od nichž se vzdalujete nebo které jen
  míjíte.
* **Dálniční křižovatky a sjezdy**, jakmile k nim dojedete.
* **Velké orientační body**, které míjíte, například parky, nemocnice, stadiony a obchodní centra.
* **Autobusové, tramvajové a vlakové zastávky**, které míjíte. Soundscape zmiňuje jen zastávky na
  vaší straně silnice, protože ty na protější straně slouží opačnému směru.
* **Řeky, kanály a železnice, které překračujete.**
* **Tunely**, což hlavně vysvětluje, proč Soundscape za chvíli ztichne — uvnitř není signál GPS.

Ve **vlaku** Soundscape rozpozná, že jste na železnici, a ne na silnici, a řekne vám, kolem jakých
obcí projíždíte a jakou vzdálenost jste ujeli od poslední stanice. Zjistit to je těžší, než to zní,
protože dálnice a železniční tratě jsou často stavěny vedle sebe celé kilometry, takže značná část
práce v tomto vydání šla do toho, aby se jedno nezaměňovalo za druhé.

Běžná hlášení pro chodce — obchody v okolí, přechody a tak dále — jsou během jízdy záměrně zadržena a
vzdálenosti, na nichž se věci ohlašují, byly výrazně prodlouženy, abyste se o něčem dozvěděli dříve,
než to minete.

### Křižovatky

Nejčastější otázka k hlášení křižovatek byla, jak daleko ta křižovatka vlastně je. Soundscape vám to
teď řekne: „Křižovatka 30 metrů daleko“. Vzdálenost se měří k obrubníku ulice, kterou se chystáte
přejít, ne ke středu křižovatky, protože tam se skutečně zastavíte.

Hlášení také přichází na stálejším místě. Dříve mohlo zaznít 45 metrů předem, nebo 10 metrů předem, a
nebylo jak to rozlišit. Teď počká, až je křižovatka asi 30 metrů daleko, takže vzdálenost znamená
pokaždé zhruba totéž.

### Překonávání vodních toků a železnic

Soundscape vám nyní řekne, když překračujete řeku, kanál, záliv, zátoku nebo železniční trať. Funguje
to pěšky i za jízdy a zahrnuje jak průchod pod, tak nad, takže je popsána jak lávka, tak podchod.

### Zvolte, kolik toho Soundscape říká

Nejčastěji o Soundscape slýcháme, že na rušných místech, jako je centrum města, mluví příliš. Oddíl
*Spravovat hlášení* v *Nastavení* má teď místo starého seznamu přepínačů tři nastavení:

* **Podrobnost hlášení** je Bez zvuku, Tichý, Vyvážený nebo Podrobný. *Podrobný* je to, co Soundscape
  dělal vždycky, a je výchozí. *Vyvážený* vynechává menší cesty a obslužné komunikace a méně se
  opakuje. *Tichý* ohlašuje jen ulice, křižovatky a orientační body. *Bez zvuku* nedělá žádná
  automatická hlášení, zatímco majáky, trasy a tlačítka na domovské obrazovce dál fungují. Nahrazuje
  starý přepínač *Povolit hlášení*; pokud jste ho měli vypnutý, najdete Podrobnost hlášení nastavenou
  na Bez zvuku.
* **Ulice a křižovatky** zapíná nebo vypíná hlášení o křižovatkách a o ulici, na které jste.
* **Místa k ohlašování** je seznam k zaškrtnutí: Vše, Orientační body, Veřejná doprava, Jídlo a pití,
  Potraviny a smíšené zboží, Banky a bankomaty nebo Žádná místa. Zaškrtněte jich, kolik chcete,
  například orientační body a autobusové zastávky. Vaše značky se ohlašují vždy.

Vhodná podrobnost se za chůze mění, takže kvůli ní nemusíte chodit do Nastavení. Stisknutím
*Předchozí* na sluchátkách snížíte podrobnost hlášení vždy o jeden stupeň, z Podrobného přes Vyvážený
a Tichý na Bez zvuku a pak zase zpět na Podrobný. Nový stupeň se pokaždé ohlásí. Funguje to v obou
režimech ovládání médií, a proto se tlačítka sluchátek trochu změnila:

* V *Původním režimu* teď *Další* ohlásí *Kolem mě*, když se nepřehrává žádná trasa, a *Moje poloha*
  už na tlačítkách není. Při přehrávání trasy *Další* a *Předchozí* dál přecházejí mezi trasovými
  body.
* V režimu *Zvukové menu* se *Předchozí* už nevrací v menu zpět. *Další* jím dál prochází a
  *Přehrát/Pozastavit* dál vybírá. Značky a trasy jsou teď v menu seřazené podle názvu a po spuštění
  jedné z nich se menu vrátí na začátek, místo aby vás nechalo hluboko v seznamu.

### Vyhledávání

Vyhledávací pole teď rozumí víc než jen názvům míst:

* **Druhy míst.** Vyhledejte ve svém jazyce „lékárna“, „toaleta“, „bankomat“ a podobně a Soundscape
  vypíše nejbližší místa tohoto druhu, ať se jmenují jakkoli. Místa bez názvu, jako většina toalet a
  laviček, jsou uvedena podle toho, čím jsou, i s adresou.
* **Souřadnice, odkazy na mapy a Plus Codes.** Vložte dvojici čísel, stupně a minuty, odkaz z Map
  Google, Map Apple nebo OpenStreetMap, případně Plus Code, a Soundscape vám dá přesně to místo.
  Samotnou dvojici čísel lze číst oběma směry, takže když dávají smysl oba, dostanete oba, bližší
  první.
* **Vyhledávání offline.** Vyhledávání se teď vždy dívá i do stažených map, nejen online, a najde tak
  mnohem víc míst bez názvu. Pokud hledáte bez připojení k internetu a nemáte offline mapu místa, kde
  jste, Soundscape vám to řekne, místo aby prostě nic nenašel.

### Otevření místa v jiné aplikaci

Podrobnosti o místě mají nové tlačítko **Otevřít v mapové aplikaci**, které vypíše mapové a navigační
aplikace ve vašem telefonu. Zaškrtněte *Vždy používat tuto aplikaci* a tlačítko se změní například na
*Otevřít v aplikaci Mapy Google* a otevře ji hned; dlouhé stisknutí vrátí seznam. Seznamy *Místa v
okolí* a *Značky* mají také akce pro čtečku obrazovky *Otevřít v aplikaci…* a *Sdílet*, vedle
*Spustit zvukový maják*.

### Maják a značky

* Maják na domovské obrazovce teď ukazuje svou **vzdálenost a směr** a čtečka obrazovky ho přečte
  například jako „Maják na Milngavie Library, 390 metrů, jihovýchod“. Trasy stejně ukazují vzdálenost
  k aktuálnímu trasovému bodu.
* Maják má tři **akce pro čtečku obrazovky**: *Ohlásit maják* řekne, kde je, *Další informace* přidá
  adresu a *Přidat do značek* ho uloží.
* **Značku můžete znovu přesunout** tažením mapy na obrazovce *Upravit značku*.

### Lepší adresy a názvy míst

Hodně práce bylo vloženo do toho, aby Soundscape popisoval místa tak, jak by to udělal člověk:

* Místa bez vlastní adresy jsou nyní popsána ulicí a oblastí, v níž leží, místo aby zůstala neurčitá.
* Čísla popisná jsou přiřazena ke správné straně ulice. Dříve mohla být adresa hlášena z protějšího
  chodníku.
* Adresa místa už neopakuje název samotného místa.
* Autobusové zastávky ve Velké Británii používají oficiální názvy veřejné dopravy, obvykle ty z
  jízdního řádu a z označníku zastávky.
* Nepojmenované pěšiny vedoucí podél řeky nebo kanálu jsou nyní pojmenovány podle vody, kterou
  sledují.
* Cesty a silnice bez názvu jsou popsány rozumněji a slova pro ně použitá jsou řádně přeložena místo
  toho, aby se objevovala anglicky.

### Jazyky

Ve verzi 2.0 přibylo dvacet nových jazyků: arabština, bengálština, bulharština, katalánština,
chorvatština, čeština, hauština, maďarština, indonéština, korejština, maráthština, srbština,
slovenština, slovinština, svahilština, tamilština, telugština, thajština, urdština a vietnamština.
Všechny tyto jazyky jsou ve fázi alfa a velmi stojíme o zpětnou vazbu k jejich přesnosti. Celkem je
nyní Soundscape dostupný ve 46 jazycích a přeložen byl i tento dokumentační web.

Egyptská arabština byla sloučena s arabštinou a lugandština byla stažena, protože ani jedna neměla
dost přeloženého textu, aby byla užitečná.

Překlady jsou dílem komunity a rádi uvítáme vaši pomoc nebo opravy tam, kde se něco čte špatně.
Jakýkoli text lze zlepšit na
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Režim spánku

Režim spánku získal **probuzení při odchodu**. Když Soundscape uspíte, můžete jej požádat, aby se
probudil, jakmile opustíte oblast. To se hodí, když někam dorazíte a chcete klid, dokud se zase
nevydáte na cestu.

### Vzdálenosti a řeč

Vyslovované vzdálenosti byly zkráceny a znějí přirozeněji a Soundscape nyní přechází na větší
jednotky, když se pohybujete rychle — míle nebo kilometry místo dlouhého počítání ve stopách či
metrech. Každý jazyk sám rozhoduje, jak vyslovit zlomkovou vzdálenost, což bylo dříve vtěsnáno do
anglicky utvářeného vzorce.

### Offline mapy

Offline mapy přišly s verzí 1.0 a jsou soustavně vylepšovány:

* Staženou mapu lze nyní aktualizovat na místě, jakmile je k dispozici novější verze, z obrazovky
  s podrobnostmi výřezu.
* Mapy, které nelze použít — například poškozené stažení — jsou nyní zřetelně označeny, místo aby
  selhaly potichu.
* Stahování je spolehlivější a obrazovka ukazuje, co se děje, zatímco se načítá seznam dostupných
  map, místo indikátoru přes celou obrazovku.
* Dokončené stažení se jako dokončené zobrazí až tehdy, když je skutečně připraveno k použití.
* Na tomto webu je
  [mapa dostupných oblastí]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Přístupnost

Velké množství práce bylo věnováno chování čteček obrazovky, zejména na úvodních obrazovkách, kde
zaměření dříve skákalo na nesprávné místo. Mezi další vylepšení patří lepší čtení velikostí souborů a
desetinných čísel, správné nápovědy typu „dvojitým klepnutím...“ v jazycích, které kladou sloveso na
konec, a smysluplné nápovědy tam, kde žádné nastaveny nebyly.

### Nabídky a navigace

* **Ukončit Soundscape** je nyní první položkou hlavní nabídky, místo aby byla někde níže.
* Hlavní nabídka už nenechává po straně vidět pruh obrazovky, který uživatelům čteček dával matoucí
  další plochu ke klepnutí.
* Systémové gesto zpět už nepřeskakuje úroveň, když procházíte kategorie v Místech v okolí.
* *Zvukový průvodce* byl přejmenován na **řízeného průvodce**.
* Nastavení bylo uklizeno a *Obnovit výchozí hodnoty* nyní správně vymaže vše.

### Stabilita

Verze 2.0 obsahuje dlouhý seznam opravených pádů a zamrznutí, mimo jiné zamrznutí aplikace na úvodní
obrazovce, zamrznutí při obnovování nastavení, pády při poškozené stažené mapě, pády při otevírání
podrobností trasy z domovské obrazovky, pády při změně jazyka a několik problémů hlášených
automaticky přes Obchod Play. Chování ohledně baterie a spouštění bylo rovněž zpevněno na telefonech,
které agresivně ukončují aplikace na pozadí.

### Odstraněné funkce
{: #things-that-have-been-removed }

* **Hlasové ovládání** bylo odstraněno. Nikdy nefungovalo dost spolehlivě na to, aby stálo za
  zachování, a multimediální tlačítka na sluchátkách pokrývají z velké části totéž — viz
  [Nápovědu k používání multimediálních tlačítek]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape je také připravený na hlasové příkazy přes Gemini v Androidu 16 a novějším, ty ale
  nebudou fungovat, dokud Google jejich podporu v Gemini nezveřejní.
* **Nabídka jazyka uvnitř aplikace** zmizela. Soundscape se nyní řídí jazykem nastaveným v telefonu,
  což většina lidí očekávala. Chcete-li jej změnit, změňte jazyk telefonu nebo v jeho nastavení
  určete jazyk pro jednotlivou aplikaci, pokud to nabízí.

## Jak nám nahlásit problém

Pokud něco není v pořádku, rádi se to dozvíme. Napište na Help Desk na adresu
<soundscapeAndroid@scottishtecharmy.support> nebo se zeptejte na Slacku, jste-li členem STA.

Pokud bylo hlášení chybné nebo nepřišlo, záznam vaší cesty nám nesmírně pomůže — můžeme jej přehrát a
přesně vidět, z čeho Soundscape vycházel. Pokyny najdete v části
[Poskytnutí záznamu polohy pro ladění]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Poznámka k iPhonu

Vše výše uvedené se týká aplikace pro Android, ale stojí za to vědět, kam šel zbytek práce v tomto
vydání. Soundscape nyní běží i na iPhonu a obě aplikace jsou sestaveny ze stejného sdíleného kódu —
stejné obrazovky, stejné formulace a stejná hlášení. Novinka jako výše popsaná cestovní hlášení se
tak dostane do obou naráz, místo aby byla psána dvakrát. Tento společný základ je důvodem, proč
verze 2.0 trvala tak dlouho, a měl by zajistit, aby budoucí vydání přicházela rychleji na obě
platformy. Aplikace pro iPhone je momentálně dostupná přes TestFlight na pozvání: zeptejte se na
Slacku, jste-li členem STA, nebo napište na Help Desk.
