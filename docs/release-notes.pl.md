---
title: Informacje o wersji
layout: page
nav_order: 5
has_toc: false
lang: pl
permalink: /release-notes.html
machine-translated: true
---

# Informacje o wersji

Soundscape 2.0 to duże wydanie, obecnie w zamkniętej wersji beta. Najważniejsza zmiana polega na tym,
że Soundscape ma teraz coś użytecznego do powiedzenia, gdy podróżujesz samochodem, autobusem lub
pociągiem, a nie tylko wtedy, gdy idziesz pieszo. Doszło też wiele mniejszych prac nad tym, jak
opisywane są miejsca, dwadzieścia nowych języków oraz długa lista poprawek.

Informacje o starszych wersjach znajdują się na stronie
[Informacje o wersjach 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nowości w wersji 2.0

* **Komunikaty podczas podróży samochodem, autobusem lub pociągiem.** Soundscape rozpoznaje, że
  poruszasz się z prędkością, i opisuje twoją podróż zamiast najbliższego otoczenia.
* **Informacja o przekraczaniu wód i linii kolejowych.** Rzeki, kanały, zatoki i linie kolejowe są
  ogłaszane, gdy je przekraczasz — zarówno pieszo, jak i w podróży.
* **Lepsze adresy i nazwy miejsc.** Miejsca bez własnego adresu otrzymują teraz ulicę i okolicę, w
  której się znajdują, numery domów są przypisywane do właściwej strony ulicy, a przystanki autobusowe
  w Wielkiej Brytanii używają swoich oficjalnych nazw.
* **Dwadzieścia nowych języków**, co daje łącznie 46. Ta witryna z dokumentacją również została
  przetłumaczona.
* **Budzenie przy wyjściu.** Tryb uśpienia może teraz obudzić Soundscape, gdy opuścisz miejsce, w
  którym go uśpiłeś.
* **Krótsze, bardziej naturalne odległości**, z większymi jednostkami, gdy poruszasz się szybko.
* **Szybsze wyjście.** *Zamknij Soundscape* znajduje się teraz na górze menu głównego.
* **Ulepszenia map offline**, w tym aktualizacja już pobranej mapy oraz mapa dostępnych regionów na
  tej witrynie.
* **Dużo pracy nad dostępnością** z TalkBack, zwłaszcza wokół ekranów wprowadzających.
* **Bardzo wiele poprawek awarii i stabilności.**

W wersji 2.0 **usunięto** dwie rzeczy: sterowanie głosowe i menu języka wewnątrz aplikacji. Zobacz
[Usunięte funkcje](#things-that-have-been-removed) poniżej, aby dowiedzieć się, co robić zamiast tego.

---

## Bardziej szczegółowo

### Podróż samochodem, autobusem lub pociągiem

To największa nowość dla dotychczasowych użytkowników. Wcześniej Soundscape miał bardzo niewiele do
powiedzenia, gdy tylko wsiadłeś do pojazdu: nadal opisywał najbliższe otoczenie, co przy prędkości
oznaczało strumień rzeczy, które już dawno minąłeś.

Soundscape zauważa teraz, że poruszasz się szybciej niż pieszo, i zmienia to, co ci przekazuje. Nie
trzeba niczego włączać, a wszystko samo wraca do normy, gdy tylko zwolnisz albo wysiądziesz i
pójdziesz pieszo.

Podczas podróży usłyszysz:

* **Gdzie jesteś**, co jakiś czas — drogę, którą jedziesz, i kierunek jazdy, na przykład „Jazda na
  północ drogą M8”. Drogi z numerem są ogłaszane ich numerem, a Soundscape nie powtarza tej samej
  drogi za każdym razem, gdy zmienia się nazwa ulicy.
* **Miasta i wsie**, w kierunku których jedziesz, wraz z odległością, a także te, od których się
  oddalasz lub które po prostu mijasz.
* **Węzły i zjazdy z autostrady**, gdy do nich dojeżdżasz.
* **Duże punkty orientacyjne**, które mijasz, takie jak parki, szpitale, stadiony i centra handlowe.
* **Przystanki autobusowe, tramwajowe i stacje kolejowe**, które mijasz. Soundscape wymienia tylko
  przystanki po twojej stronie drogi, ponieważ te po przeciwnej obsługują przeciwny kierunek.
* **Rzeki, kanały i linie kolejowe, które przekraczasz.**
* **Tunele**, co przede wszystkim wyjaśnia, dlaczego Soundscape zaraz zamilknie — w środku nie ma
  sygnału GPS.

W **pociągu** Soundscape rozpoznaje, że jesteś na linii kolejowej, a nie na drodze, i mówi ci, obok
jakich miejscowości przejeżdżasz oraz jaką odległość pokonałeś od ostatniej stacji. Ustalenie tego
jest trudniejsze, niż się wydaje, ponieważ autostrady i linie kolejowe często biegną obok siebie
kilometrami, więc spora część pracy w tym wydaniu poszła na to, by nie mylić jednego z drugim.

Zwykłe komunikaty dla pieszych — pobliskie sklepy, przejścia dla pieszych i tak dalej — są celowo
wstrzymywane podczas podróży, a odległości, na jakich ogłaszane są obiekty, zostały znacznie
zwiększone, żebyś dowiedział się o czymś, zanim to miniesz.

### Przekraczanie wód i linii kolejowych

Soundscape informuje teraz, gdy przekraczasz rzekę, kanał, zatokę, cieśninę lub linię kolejową.
Działa to zarówno pieszo, jak i w podróży, i obejmuje zarówno przejście pod spodem, jak i górą, więc
opisywana jest zarówno kładka, jak i przejście podziemne.

### Lepsze adresy i nazwy miejsc

Włożono wiele pracy w to, aby Soundscape opisywał miejsca tak, jak zrobiłby to człowiek:

* Miejsca bez własnego adresu są teraz opisywane przez ulicę i okolicę, w której się znajdują, zamiast
  pozostawać nieokreślone.
* Numery domów są przypisywane do właściwej strony ulicy. Wcześniej adres mógł zostać podany z
  przeciwnego chodnika.
* Adres miejsca nie powtarza już nazwy samego miejsca.
* Przystanki autobusowe w Wielkiej Brytanii używają oficjalnych nazw transportu publicznego, zwykle
  tych z rozkładu jazdy i z tabliczki na przystanku.
* Nienazwane ścieżki biegnące wzdłuż rzeki lub kanału są teraz nazywane od wody, którą podążają.
* Ścieżki i drogi bez nazwy są opisywane sensowniej, a używane słowa są prawidłowo przetłumaczone,
  zamiast pojawiać się po angielsku.

### Języki

W wersji 2.0 dodano dwadzieścia nowych języków: arabski, bengalski, bułgarski, kataloński, chorwacki,
czeski, hausa, węgierski, indonezyjski, koreański, marathi, serbski, słowacki, słoweński, suahili,
tamilski, telugu, tajski, urdu i wietnamski. Wszystkie te języki są w fazie alfa i bardzo zależy nam
na opiniach o ich poprawności. Łącznie Soundscape jest teraz dostępny w 46 językach, a ta witryna z
dokumentacją również została przetłumaczona.

Egipski arabski został połączony z arabskim, a luganda wycofana, ponieważ żaden z nich nie miał
wystarczająco dużo przetłumaczonego tekstu, by był użyteczny.

Tłumaczenia to praca społeczności i chętnie przyjmiemy twoją pomoc albo poprawki tam, gdzie coś czyta
się źle. Każdy tekst można poprawić na
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Tryb uśpienia

Tryb uśpienia zyskał **budzenie przy wyjściu**. Gdy usypiasz Soundscape, możesz poprosić, by obudził
się, gdy tylko opuścisz okolicę. Przydaje się to, gdy gdzieś docierasz i chcesz mieć spokój, dopóki
znów nie wyruszysz.

### Odległości i mowa

Wypowiadane odległości zostały skrócone i brzmią naturalniej, a Soundscape przechodzi teraz na większe
jednostki, gdy poruszasz się szybko — mile lub kilometry zamiast długiego odliczania w stopach czy
metrach. Każdy język sam decyduje, jak wypowiedzieć odległość ułamkową, co wcześniej było wtłoczone w
schemat ukształtowany po angielsku.

### Mapy offline

Mapy offline pojawiły się w wersji 1.0 i są stale ulepszane:

* Pobraną mapę można teraz zaktualizować na miejscu, gdy dostępna jest nowsza wersja, z ekranu
  szczegółów wycinka.
* Mapy, których nie da się użyć — na przykład uszkodzone pobranie — są teraz wyraźnie oznaczane,
  zamiast zawodzić po cichu.
* Pobieranie jest bardziej niezawodne, a ekran pokazuje, co się dzieje podczas pobierania listy
  dostępnych map, zamiast pełnoekranowego wskaźnika ładowania.
* Zakończone pobranie pojawia się jako zakończone dopiero wtedy, gdy naprawdę jest gotowe do użycia.
* Na tej witrynie znajduje się
  [mapa dostępnych regionów]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Dostępność

Włożono bardzo dużo pracy w zachowanie czytników ekranu, zwłaszcza na ekranach wprowadzających, gdzie
fokus wcześniej przeskakiwał w niewłaściwe miejsce. Inne ulepszenia to lepsze odczytywanie rozmiarów
plików i liczb dziesiętnych, poprawne podpowiedzi typu „stuknij dwukrotnie, aby...” w językach, które
stawiają czasownik na końcu, oraz sensowne podpowiedzi tam, gdzie nie było żadnych.

### Menu i nawigacja

* **Zamknij Soundscape** jest teraz pierwszą pozycją menu głównego, a nie gdzieś niżej.
* Menu główne nie pokazuje już paska ekranu z boku, który dawał osobom korzystającym z czytnika ekranu
  mylący dodatkowy obszar do stuknięcia.
* Systemowy gest cofania nie pomija już poziomu, gdy przeglądasz kategorie w Miejscach w pobliżu.
* *Samouczek dźwiękowy* został przemianowany na **samouczek prowadzony**.
* Ustawienia zostały uporządkowane, a *Przywróć wartości domyślne* czyści teraz wszystko poprawnie.

### Stabilność

Wersja 2.0 zawiera długą listę poprawionych awarii i zawieszeń, w tym zawieszanie się aplikacji na
ekranie powitalnym, zawieszenia przy przywracaniu ustawień, awarie przy uszkodzonej pobranej mapie,
awarie przy otwieraniu szczegółów trasy z ekranu głównego, awarie przy zmianie języka oraz kilka
problemów zgłoszonych automatycznie przez Sklep Play. Zachowanie związane z baterią i uruchamianiem
również zostało wzmocnione na telefonach agresywnie zamykających aplikacje w tle.

### Usunięte funkcje
{: #things-that-have-been-removed }

* **Sterowanie głosowe** zostało usunięte. Nigdy nie działało wystarczająco niezawodnie, by je
  zachować, a przyciski multimedialne na słuchawkach obejmują w dużej mierze to samo — zobacz
  [Pomoc dotyczącą korzystania z przycisków multimedialnych]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Menu języka wewnątrz aplikacji** zniknęło. Soundscape podąża teraz za językiem ustawionym w
  telefonie, czego większość osób i tak oczekiwała. Aby go zmienić, zmień język telefonu albo ustaw
  język dla poszczególnych aplikacji w jego ustawieniach, jeśli taka opcja jest dostępna.

## Zgłaszanie nam problemów

Jeśli coś jest nie tak, chętnie się o tym dowiemy. Napisz do Help Desku na adres
<soundscapeAndroid@scottishtecharmy.support> albo zapytaj na Slacku, jeśli jesteś członkiem STA.

Jeśli komunikat był błędny albo się nie pojawił, nagranie twojej podróży ogromnie nam pomaga — możemy
je odtworzyć i zobaczyć dokładnie, na czym Soundscape się opierał. Instrukcje znajdziesz w sekcji
[Udostępnianie zapisu położenia na potrzeby diagnostyki]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Uwaga o iPhonie

Wszystko powyżej dotyczy aplikacji na Androida, ale warto wiedzieć, dokąd trafiła reszta pracy w tym
wydaniu. Soundscape działa teraz również na iPhonie, a obie aplikacje są budowane z tego samego
wspólnego kodu — te same ekrany, te same sformułowania i te same komunikaty. Nowość taka jak opisane
wyżej komunikaty podróżne trafia więc do obu naraz, zamiast być pisana dwa razy. Ta wspólna podstawa
tłumaczy, dlaczego wersja 2.0 zajęła tyle czasu, i to ona powinna sprawić, że przyszłe wydania będą
pojawiać się szybciej na obu platformach. Aplikacja na iPhone'a jest obecnie dostępna przez TestFlight
na zaproszenie: zapytaj na Slacku, jeśli jesteś członkiem STA, albo napisz do Help Desku.
