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
* **Wybierz, ile mówi Soundscape.** Nowe ustawienie *Szczegółowość powiadomień* sprawia, że Soundscape
  mówi mniej w ruchliwych miejscach, a w *Miejscach do ogłaszania* wybierasz, o jakich rodzajach
  miejsc chcesz słyszeć. Szczegółowość możesz zmieniać przyciskami słuchawek w trakcie marszu.
* **Wiesz, jak daleko jest najbliższe skrzyżowanie.** Skrzyżowania są ogłaszane w stałej odległości,
  gdy się do nich zbliżasz, a powiadomienie mówi teraz, jak daleko jest do krawężnika.
* **Szukaj rodzaju miejsca albo współrzędnych.** Wpisz „apteka” albo „przystanek autobusowy”, żeby
  znaleźć najbliższe, niezależnie od ich nazwy, albo wklej współrzędne, link do mapy lub Plus Code.
* **Otwieraj miejsce w innej aplikacji z mapami**, np. w Mapach Google, ze szczegółów lokalizacji
  lub z list.
* **Więcej z naprowadzania na ekranie głównym.** Pokazuje ono teraz odległość i kierunek oraz ma
  akcje czytnika ekranu, które ogłaszają cel, podają więcej informacji albo zapisują go jako znacznik.
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

### Skrzyżowania

Najczęstsze pytanie o powiadomienia o skrzyżowaniach brzmiało: jak daleko właściwie jest to
skrzyżowanie? Teraz Soundscape to mówi: „Skrzyżowanie za 30 metrów”. Odległość jest mierzona do
krawężnika ulicy, którą za chwilę przejdziesz, a nie do środka skrzyżowania, bo to przy krawężniku
naprawdę się zatrzymujesz.

Powiadomienie pojawia się też w bardziej stałym miejscu. Wcześniej mogło przyjść 45 metrów przed
skrzyżowaniem albo 10 metrów przed nim i nie dało się tego odróżnić. Teraz czeka, aż skrzyżowanie
będzie około 30 metrów przed tobą, więc odległość za każdym razem znaczy mniej więcej to samo.

### Przekraczanie wód i linii kolejowych

Soundscape informuje teraz, gdy przekraczasz rzekę, kanał, zatokę, cieśninę lub linię kolejową.
Działa to zarówno pieszo, jak i w podróży, i obejmuje zarówno przejście pod spodem, jak i górą, więc
opisywana jest zarówno kładka, jak i przejście podziemne.

### Wybierz, ile mówi Soundscape

Najczęściej słyszymy o Soundscape, że mówi za dużo w ruchliwych miejscach, takich jak centrum miasta.
Sekcja *Zarządzaj powiadomieniami* w *Ustawieniach* ma teraz trzy ustawienia zamiast dawnej listy
przełączników:

* **Szczegółowość powiadomień** ma poziomy Wyciszony, Podstawowy, Uproszczony i Szczegółowy.
  *Szczegółowy* to to, co Soundscape robił zawsze, i od tego zaczynasz. *Uproszczony* pomija
  mniejsze ścieżki i drogi dojazdowe i rzadziej się powtarza. *Podstawowy* powiadamia tylko o ulicach,
  skrzyżowaniach i punktach orientacyjnych. *Wyciszony* nie daje żadnych automatycznych powiadomień, a
  dźwięk naprowadzający, trasy i przyciski ekranu głównego nadal działają. Zastępuje dawny przełącznik
  *Zezwól na powiadomienia*; jeśli był wyłączony, szczegółowość jest teraz ustawiona na Wyciszony.
* **Ulice i skrzyżowania** włącza lub wyłącza powiadomienia o skrzyżowaniach i o ulicy, na której
  jesteś.
* **Miejsca do ogłaszania** to lista do zaznaczenia: Wszystko, Punkty orientacyjne, Transport
  publiczny, Jedzenie i napoje, Sklepy spożywcze i sklepy osiedlowe, Banki i bankomaty albo Brak
  miejsc. Zaznacz ich tyle, ile chcesz, na przykład punkty orientacyjne i przystanki autobusowe. Twoje
  znaczniki są ogłaszane zawsze.

Odpowiednia szczegółowość zmienia się w trakcie marszu, więc nie musisz wchodzić do Ustawień, żeby ją
zmienić. Naciśnięcie *Poprzedni* na słuchawkach obniża szczegółowość o jeden poziom: od Szczegółowego
przez Uproszczony i Podstawowy do Wyciszonego, a potem z powrotem do Szczegółowego. Za każdym razem
słyszysz nowy poziom. Działa to w obu trybach sterowania multimediami, dlatego przyciski słuchawek
trochę się zmieniły:

* W *Trybie oryginalnym* *Następny* ogłasza teraz *Wokół mnie*, gdy żadna trasa nie jest odtwarzana,
  a *Moja pozycja* nie jest już na przyciskach. Podczas odtwarzania trasy *Następny* i *Poprzedni*
  nadal przechodzą między punktami trasy.
* W trybie *Menu audio* *Poprzedni* nie cofa się już w menu. *Następny* nadal po nim przechodzi, a
  *Odtwórz/Pauza* nadal wybiera. Znaczniki i trasy są teraz w menu ułożone według nazw, a po
  uruchomieniu jednego z nich menu wraca na początek, zamiast zostawiać cię głęboko w liście.

### Wyszukiwanie

Pasek wyszukiwania rozumie teraz więcej niż nazwy miejsc:

* **Rodzaje miejsc.** Wpisz „apteka”, „toaleta”, „bankomat” i tak dalej, we własnym języku, a
  Soundscape pokaże najbliższe miejsca tego rodzaju, niezależnie od nazwy. Miejsca bez nazwy, jak
  większość toalet i ławek, są pokazywane według tego, czym są, razem z adresem.
* **Współrzędne, linki do map i Plus Codes.** Wklej parę liczb, stopnie i minuty, link z Map Google,
  Map Apple albo OpenStreetMap lub Plus Code, a Soundscape poda dokładnie to miejsce. Samą parę liczb
  można odczytać na dwa sposoby, więc jeśli oba mają sens, dostaniesz oba, najbliższy jako pierwszy.
* **Wyszukiwanie offline.** Wyszukiwanie zawsze sprawdza teraz także pobrane mapy, a nie tylko
  internet, dzięki czemu znajduje dużo więcej miejsc bez nazwy. Jeśli szukasz bez połączenia z
  internetem i nie masz mapy offline miejsca, w którym jesteś, Soundscape o tym mówi, zamiast po
  prostu nic nie znaleźć.

### Otwieranie miejsca w innej aplikacji

W szczegółach lokalizacji jest nowy przycisk **Otwórz w aplikacji z mapami**, który pokazuje
aplikacje z mapami i nawigacją na twoim telefonie. Zaznacz *Zawsze używaj tej aplikacji*, a przycisk
zmieni się na przykład na *Otwórz w aplikacji Mapy Google* i od razu ją otworzy; długie naciśnięcie
przywraca listę. Listy *Miejsca w pobliżu* i *Znaczniki (pinezki)* mają też akcje czytnika ekranu
*Otwórz w aplikacji…* i *Udostępnij*, obok *Uruchom dźwięk naprowadzający*.

### Naprowadzanie i znaczniki

* Naprowadzanie na ekranie głównym pokazuje teraz **odległość i kierunek**, a czytnik ekranu czyta
  je na przykład jako „Naprowadzanie na Milngavie Library, 390 metrów, południowy wschód”. Trasy
  pokazują w ten sam sposób odległość do bieżącego punktu trasy.
* Naprowadzanie ma trzy **akcje czytnika ekranu**: *Powiadom o punkcie trasy* mówi, gdzie jest cel,
  *Więcej informacji* dodaje adres, a *Dodaj do znaczników* go zapisuje.
* Znów możesz **przesunąć znacznik**, przeciągając mapę na ekranie *Edytuj znacznik (pinezkę)*.

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
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

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
  Soundscape jest też gotowy na polecenia głosowe przez Gemini na Androidzie 16 i nowszym, ale nie
  będą działać, dopóki Google nie udostępni ich obsługi w Gemini.
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
