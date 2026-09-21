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

### Překonávání vodních toků a železnic

Soundscape vám nyní řekne, když překračujete řeku, kanál, záliv, zátoku nebo železniční trať. Funguje
to pěšky i za jízdy a zahrnuje jak průchod pod, tak nad, takže je popsána jak lávka, tak podchod.

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
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

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
