---
title: Jak działa Soundscape
layout: page
parent: Using Soundscape
has_toc: false
lang: pl
permalink: /users/how-it-works.html
machine-translated: true
---
# Jak działa Soundscape
Celem tej strony jest przekazanie ogólnego zrozumienia tego, jak aplikacja Soundscape działa „pod maską”. Nie musisz tego czytać, aby korzystać z aplikacji, ale istnieje kilka powodów, dla których powstał ten tekst:

1. Aby pomóc zainteresowanym nowym użytkownikom aplikacji zrozumieć, gdzie leżą jej ograniczenia
1. Aby dać użytkownikom wyobrażenie o tym, co jeszcze mogłoby być możliwe dzięki nowym funkcjom
1. Aby dać programistom ogólny przegląd działania aplikacji

Istnieją dwie technologie, które umożliwiają działanie aplikacji: GPS i dane OpenStreetMap. GPS daje nam dobre wyobrażenie o tym, gdzie jest telefon i gdzie był. Dane OpenStreetMap można następnie wykorzystać do znalezienia tego, co znajduje się w pobliżu, i opisać to użytkownikowi.

## Dźwięki naprowadzające
Pod większością względów są to najprostsze rzeczy do zaimplementowania z punktu widzenia technologii. Zakładając, że mamy lokalizację telefonu i kierunek telefonu, możemy zmienić dźwięk naprowadzający tak, aby brzmiał, jakby dochodził z tego kierunku. Do pozycjonowania dźwięku używamy biblioteki od Steam Audio, która wykorzystuje funkcje przenoszenia związane z głową (head related transfer functions), aby zapewnić możliwie najlepiej brzmiące pozycjonowanie. Jedyne, co jeszcze robimy, to zmiana dźwięku naprowadzającego tak, aby w zależności od kąta między kierunkiem użytkownika a lokalizacją dźwięku naprowadzającego odtwarzany był inny dźwięk. Kąty różnią się w zależności od wybranego dźwięku naprowadzającego (Stukot, Błysk, Pikanie itp.), a niektóre mają większą liczbę dźwięków niż inne. I to są dźwięki naprowadzające na najprostszym poziomie.

Jedyną dodatkową złożonością jest założenie dotyczące lokalizacji telefonu oraz kierunku, w którym jest zwrócony użytkownik. Przyjrzyjmy się im po kolei.

### Lokalizacja
Lokalizacja zwracana przez GPS może mieć dość duży błąd, a zależy to od tego, jak duża część nieba jest widoczna dla GPS w telefonie oraz ile drzew i wysokich budynków odbija sygnał GPS w drodze do telefonu.

Podejście, które zastosowaliśmy do filtrowania lokalizacji, polega na wykorzystaniu czegoś znanego jako dopasowywanie do mapy (map matching). Zakłada ono, że użytkownik najprawdopodobniej porusza się wzdłuż zmapowanej ścieżki lub drogi — używamy terminu „Way” (droga) na określenie wszystkich dróg, traktów i ścieżek. Dopasowywanie do mapy analizuje, gdzie użytkownik był, i wykorzystując kierunek ruchu wraz z lokalnymi danymi mapowymi, wybiera najbardziej prawdopodobną lokalizację na danej drodze (Way). Podejście to uwzględnia nie tylko błędy GPS, ale także błędy w danych mapowych. Nie wszystkie drogi (Ways) są zmapowane dokładnie, więc one również mają błędy. Aby ustalić, na której drodze (Way) użytkownik najprawdopodobniej się znajduje, algorytm bierze pod uwagę:
* Jak blisko droga (Way) znajduje się od lokalizacji GPS oraz poprzednich lokalizacji GPS
* Kierunek ruchu — czy użytkownik porusza się w tym samym kierunku co droga (Way)
* Czy z ostatniej dopasowanej do mapy lokalizacji można dostać się do nowej lokalizacji poprzez sieć dróg (Ways). Jest to konieczne, aby wykluczyć przeskakiwanie między drogami (Ways), które w rzeczywistości nie są ze sobą połączone, np. jedna przechodzi nad drugą mostem lub pod nią tunelem.
Dopasowywanie do mapy może uznać, że w pobliżu nie ma żadnych dróg (Ways) lub że nie ma pewności, na której z nich znajduje się użytkownik, i w takim przypadku po prostu czeka na kolejną lokalizację GPS i próbuje ponownie, aż uzyska pewność.

### Kierunek
W oprogramowaniu śledzimy kilka kierunków:

1. Kierunek, w którym zwrócony jest telefon. Używamy go, gdy telefon jest odblokowany i aplikacja jest w użyciu, ale także gdy telefon jest zablokowany, o ile jest trzymany płasko ekranem skierowanym ku niebu. Warto o tym pamiętać, wkładając telefon do torby. Jeśli zostanie położony płasko na dnie torby trzymanej pionowo, aplikacja użyje przypadkowego kierunku, w którym zwrócona jest torba.
1. Kierunek, w którym telefon się porusza.
1. Kierunek ze słuchawek ze śledzeniem ruchów głowy. Obecnie tego nie używamy, choć aplikacja na iOS to obsługiwała. Mamy jednak technologię gotową, aby dodać to w przyszłości.

Gdy telefon jest zablokowany i znajduje się w torbie, aplikacja użyje kierunku ruchu. Jeśli jednak użytkownik się nie porusza, żaden kierunek nie jest dostępny. Gdy tak się dzieje, dźwięki naprowadzające stają się cichsze, aby zasygnalizować, że nie da się ustalić bieżącego kierunku do dźwięku naprowadzającego — użytkownik może się obracać, nie zmieniając lokalizacji.

W przypadku niektórych zastosowań kierunku w aplikacji kierunek jest „przyciągany” (snapped) do kierunku dopasowanej do mapy drogi (Way), więc jeśli użytkownik idzie mniej więcej w kierunku drogi (Way), zakłada się, że rzeczywisty kierunek tej drogi jest poprawny i jest on używany w tych obliczeniach.

### Podsumowanie
Choć na pierwszy rzut oka dźwięki naprowadzające są proste, wykorzystanie dopasowywania do mapy w celu usunięcia błędów lokalizacji i kierunku wprowadza sporo złożoności.

## Dane mapowe
Dane mapowe używane przez aplikację niemal w całości pochodzą z projektu OpenStreetMap. Prowadzimy serwer, który zawiera mapę całego świata na wielu poziomach przybliżenia. Każdy poziom przybliżenia jest podzielony na kafelki. Poziom przybliżenia 0 zawiera 1 kafelek, poziom 1 zawiera 4 kafelki, poziom 2 zawiera 16 kafelków i tak dalej, aż do poziomu 14, który zawiera około 268 milionów kafelków pokrywających całą planetę. Każdy kafelek zawiera wiele warstw, a każda warstwa ma punkty, linie i wielokąty, które można narysować, aby utworzyć graficzną mapę. Ta graficzna mapa to właśnie to, co jest pokazywane użytkownikowi w interfejsie graficznym aplikacji. Każdy punkt, linia i wielokąt ma metadane opisujące, czym jest. Pochodzą one głównie bezpośrednio z danych OpenStreetMap, więc linia może być chodnikiem (`footway`), który jest chodnikiem przyulicznym (`sidewalk`), albo drogą (`road`), która jest drogą podrzędną (`minor`).

Dane są zamieniane na graficzną mapę za pomocą „stylu”, który ma reguły dotyczące tego, jak rysować poszczególne punkty, linie i wielokąty w każdej warstwie, np. jak narysować ścieżkę, jak narysować las, jak narysować przystanek autobusowy. Reguły mogą różnić się w zależności od poziomu przybliżenia, dlatego w miarę powiększania widoku coraz więcej punktów i linii staje się widocznych, których nie widać przy oddaleniu, np. przystanki autobusowe i ścieżki.

Zmieniając styl, możemy zmienić wygląd mapy w interfejsie graficznym, i stąd pochodzi „mapa dostępna” (accessible map), którą testujemy. Ma ona zapewnić większy kontrast oraz wyraźniejsze linie i tekst. Styl jest wbudowany w aplikację, więc nie musimy zmieniać mapy na serwerze, aby zmienić jej wygląd.

Ale jak wykorzystujemy dane mapowe do dźwięku?

### Wykorzystanie danych mapowych do dźwięku
Obecnie wykorzystujemy stosunkowo niewielką część danych mapowych do generowania dźwiękowego interfejsu użytkownika. Niemal cały dźwiękowy interfejs używa wyłącznie kafelków na maksymalnym poziomie przybliżenia. Aplikacja zszywa razem siatkę 2 na 2 kafelki wokół miejsca, w którym znajduje się użytkownik, a następnie analizuje tylko kilka warstw:

* `transportation` — dla wszystkich rodzajów dróg (Ways), w tym dróg, ścieżek, kolei i tramwajów.
* `poi` — punkty zainteresowania, np. sklepy, ośrodki sportowe, ławki, skrzynki pocztowe, przystanki autobusowe itp.
* `building` — dotyczy punktów `poi`, które są zmapowane jako coś więcej niż tylko punkt, np. duże supermarkety lub ratusze.

Aplikacja łączy linie i wielokąty przez granice kafelków oraz zamienia wszystkie drogi (Ways) w połączone segmenty dróg i skrzyżowania. Jest to ważne, ponieważ pozwala nam przeszukiwać drogę (Way), aby dowiedzieć się, dokąd można dotrzeć.

Wszystkie przetworzone dane są też umieszczane w łatwym do przeszukiwania formacie, dzięki czemu aplikacja może łatwo znaleźć, które elementy mapy znajdują się w pobliżu. Na tym etapie dane są klasyfikowane w kategorie. Obecne kategorie to:

* Drogi
* Drogi i ścieżki (wszystkie drogi, Ways)
* Skrzyżowania — punkty, w których drogi (Ways) się przecinają
* Wejścia — to punkty na budynku, które zostały oznaczone jako wejście.
* Przejścia — przejścia dla pieszych
* POI — wszystkie punkty zainteresowania
* Przystanki transportu publicznego — przystanki autobusowe, stacje kolejowe, przystanki tramwajowe i tak dalej.
* Podkategorie POI:
  * Informacja
  * Obiekt
  * Miejsce
  * Punkt orientacyjny
  * Poruszanie się
  * Bezpieczeństwo
* Miejscowości i podkategorie (zobacz następną sekcję)
  * Miasta
  * Miasteczka
  * Wsie
  * Przysiółki

 Mając to gotowe, dla dowolnej lokalizacji aplikacja może następnie łatwo znaleźć

 * „Wszystkie przystanki transportu publicznego w promieniu 50 m” lub
 * „Najbliższe skrzyżowanie przede mną” lub
 * „Najbliższy przysiółek, wieś, miasteczko lub miasto”
  
Mając to gotowe, tworzenie powiadomień dźwiękowych jest tylko kwestią odpytania danych na podstawie bieżącej lokalizacji i kierunku. W miarę jak użytkownik przemieszcza się po siatce kafelków, jest ona aktualizowana tak, aby skupiała się wokół bieżącej lokalizacji.

### Więcej danych
Jednym z problemów z naszą bardzo lokalną siatką danych mapowych jest to, że oznacza ona, iż możemy „widzieć” co najwyżej około 1 km w każdym kierunku. To wystarcza, gdy opisujemy, co jest przed nami, ale czasem chcielibyśmy podać więcej kontekstu. Głównym tego przykładem jest sytuacja, gdy aplikacja jest używana, a użytkownik nie idzie.

Gdy aplikacja wykryje, że użytkownik porusza się z prędkością większą niż 5 metrów na sekundę, zmienia sposób, w jaki opisuje świat. Zamiast ogłaszać każde skrzyżowanie i punkt POI, ogłasza rzadziej i tylko pobliskie drogi. Problem polega na tym, że znajomość nazwy drogi nie jest zbyt przydatna, jeśli nie wiesz, w jakim mieście się ona znajduje.

Aby spróbować temu zaradzić, obecnie przetwarzamy także dane mapowe na niższym poziomie przybliżenia i wydobywamy dane z warstwy `place`. Zawiera ona nazwy miasteczek, miast, dzielnic, wsi i tak dalej. Jednym z problemów z mapowaniem tych rzeczy jest to, że nie zawsze istnieje oczywista granica między tymi miejscami. OpenStreetMap czasami ma w swojej bazie danych granice miast, ale nawet gdy tak jest, to zanim trafi to na naszą kafelkową mapę, ta informacja często jest tracona. To, co mamy, to lokalizacja, w której nazwy miejsc są rysowane na mapie. Są one kategoryzowane, a następnie aplikacja znajduje najbliższy przysiółek, wieś, miasteczko lub miasto względem użytkownika i to zgłasza.

W przypadku wielu miast właściwa nazwa miasta nigdy nie zostanie ogłoszona, ponieważ większość miast jest podzielona na mniejsze jednostki, takie jak dzielnice, ale to one dają dodatkowy kontekst i są bardzo przydatne. Pamiętaj tylko, że gdy aplikacja zgłasza, że znajdujesz się w pobliżu ulicy w określonej dzielnicy, oznacza to po prostu, że etykieta tej dzielnicy jest najbliższym punktem, i może ona być niepoprawna lub nawet znajdować się po drugiej stronie rzeki.

### Więcej kontekstu
Im więcej kontekstu można dodać do opisów, tym lepiej, o ile pozostaje on zwięzły i przewidywalny. Jednym z problemów, które zauważyliśmy przy opisywaniu skrzyżowań, było to, że często uczestniczyły w nich drogi „bez nazwy” (Ways). To drogi, które nie mają nazwy. W danych mapowych mogą to być po prostu trakt, ścieżka lub droga serwisowa, ale bez dodatkowego kontekstu nie jest to zbyt przydatne w opisach tekstowych. Na szczęście możemy zrobić to lepiej, więc aplikacja robi tak, że zawsze, gdy ma ogłosić drogę (Way) bez nazwy, sprawdza, czy może ustalić dla niej jakiś dodatkowy kontekst.

* **Czy to chodnik przyuliczny?**
W wielu obszarach OpenStreetMap chodniki są obecnie zmapowane oddzielnie od dróg. Są zwykle oznaczone jako `sidewalk`, ale zazwyczaj nie podają, do jakiej drogi należą.

    Gdy aplikacja natrafi na chodnik bez nazwy, wyszukuje drogę, która jej zdaniem biegnie obok niego, i używa jej do nazwania chodnika. Okazuje się to bardzo ważne dla naszych powiadomień. Zamiast ogłaszać każde skrzyżowanie chodnika, w miarę jak poruszamy się wzdłuż chodnika, powiadomienia są tworzone tak, jakbyśmy poruszali się wzdłuż powiązanej drogi. Zamiast *„Poruszasz się na zachód wzdłuż ścieżki”* mamy *„Poruszasz się na zachód wzdłuż Moor Road”*. Użytkownik znajduje się na zmapowanym chodniku, ale opis ma większy sens.

* **Czy kończy się na nazwanej drodze (Way)?**
Bardzo często istnieją ścieżki dla pieszych, które łączą ze sobą dwie drogi. Patrząc na oba końce ścieżki, możemy łatwo dodać ten kontekst, więc w jednym kierunku może to być *„Ścieżka do Moor Road”*, a podchodząc z drugiego końca *„Ścieżka do Roselea Drive”*. Robi się to tylko tam, gdzie ścieżka się nie rozwidla; jeśli rozwidla się na dwie ścieżki bez nazwy, nie próbujemy dodawać tego kontekstu.

* **Czy kończy się w pobliżu Znacznika?**
Jeśli droga (Way) bez nazwy zaczyna się lub kończy w pobliżu Znacznika, jest on używany do jej opisania, np. *„Ścieżka do skrzyżowania z dużym drzewem”*. Użytkownik może dodawać Znaczniki gdziekolwiek chce, a dodając Znaczniki wzdłuż sieci ścieżek, może dodać kontekst do całej trasy.

* **Czy wchodzi do lub wychodzi z POI?**
Jeśli droga (Way) bez nazwy zaczyna się poza punktem POI, a kończy w jego wnętrzu (lub odwrotnie), możemy dodać ten kontekst, np. *„Trakt do Lennox Park”*.

* **Czy kończy się w pobliżu Wejścia?**
Jeśli droga (Way) bez nazwy zaczyna się lub kończy bliżej Wejścia, możemy dodać ten kontekst, np. *„Droga serwisowa do Best Buy”*.

* **Czy kończy się w pobliżu Punktu orientacyjnego lub Miejsca?**
Jeśli droga (Way) bez nazwy zaczyna się lub kończy bliżej Punktu orientacyjnego, możemy także dodać ten kontekst, np. *„Droga serwisowa do katedry St. Giles”*.

* **Czy to ślepa uliczka?**
Aplikacja oznacza jako ślepą uliczkę każdą drogę (Way) bez nazwy, która donikąd nie prowadzi.

* **Czy mija jakieś schody?**
Jeśli droga (Way) bez nazwy przechodzi przez most, przez tunel lub w górę/w dół po schodach, jest to odnotowywane i dodawane do kontekstu. Jest to niezależne od oznaczania celu, więc możliwy jest kontekst taki jak *„Ścieżka przez most do Lennox Park”*.

Te konteksty są dodawane po kolei, więc możliwe jest, że w jednym kierunku będzie *„Ścieżka do Park Lane przez schody”*, a w drugim *„Ścieżka do Lennox Park przez schody”*. Nazwana ulica ma priorytet przy wychodzeniu z parku, ale przy wchodzeniu do niego używany jest park.

#### Przyszły kontekst
Istnieją różne dodatkowe konteksty, które mamy nadzieję dodać w przyszłości, w tym:
* Kontekst dla dróg (Ways) biegnących wzdłuż liniowych obiektów wodnych, np. *„Ścieżka wzdłuż rzeki Dee”*
* Kontekst dla dróg (Ways) biegnących wzdłuż krawędzi zbiorników wodnych, *„Ścieżka wzdłuż zbiornika Milngavie”*
* Kontekst dla dróg (Ways) biegnących wzdłuż torów kolejowych, np. *„Ścieżka wzdłuż kolei”*. Mógłby on nawet zawierać nazwę linii kolejowej
* Dodanie dodatkowej treści do mostów i tuneli — nad czym lub pod czym się znajdują, np. *„Ścieżka przez most nad koleją do Moor Road”*

## Powiadomienia dźwiękowe
Teraz, gdy mamy dane mapowe w formacie, którego możemy łatwo użyć, generowanie powiadomień jest naprawdę dość proste.

### Powiadomienia podczas chodzenia
Podczas chodzenia powiadomienia dźwiękowe, które mogą wystąpić, to (w kolejności priorytetu):

1. Opisanie, jak daleko znajduje się bieżący cel
1. Opisanie nadchodzącego skrzyżowania
1. Opisanie 5 najbliższych punktów zainteresowania

Wszystkie powiadomienia mają ograniczoną częstotliwość, aby nie powtarzały się zbyt często. Jeśli użytkownik przestanie się poruszać, powiadomienia ustaną, a nawet podczas ruchu powiadomienie nie powtórzy się przy każdej nowej lokalizacji GPS. Częstotliwość, podobnie jak w aplikacji na iOS, wynosi:

* Co 60 sekund dla bieżącego celu
* Co 30 sekund dla nadchodzącego skrzyżowania
* Co 60 sekund dla punktu zainteresowania

Powiadomienia można filtrować w menu ustawień i z pewnością istnieje pole do poszerzenia tego zachowania.

### Powiadomienia podczas szybszego poruszania się
Podczas poruszania się z prędkością większą niż 5 metrów na sekundę powiadomienie o bieżącym celu nadal następuje, ale powiadomienia o skrzyżowaniach i punktach zainteresowania są zastępowane powiadomieniem opisującym z grubsza, gdzie znajduje się użytkownik. Podaje ono pobliski przystanek transportu publicznego, punkt zainteresowania, w którym się znajdujemy, np. wnętrze dużego parku, albo pobliską drogę i miejscowość. Wykorzystują one dane opisane wcześniej i jest tu oczywiste pole do umożliwienia dostosowania tego w przyszłości.

## Znaczniki i Trasy
W większości przypadków znaczniki i trasy to po prostu funkcja interfejsu użytkownika, która nie opiera się ani na GPS, ani tak naprawdę na danych mapowych. Znaczniki to nazwane lokalizacje, które użytkownik chce zapisać, a trasy to uporządkowana lista tych znaczników. Interfejs użytkownika do tworzenia obu jest wzięty bezpośrednio z wersji na iOS.

### Odtwarzanie trasy
Odtwarzanie trasy to moment, w którym trasy ożywają. Gdy trasa jest odtwarzana, w pierwszym znaczniku na trasie tworzony jest dźwięk naprowadzający. Gdy użytkownik zbliży się do tego znacznika, trasa automatycznie przenosi dźwięk naprowadzający do następnego znacznika na trasie. Jeśli nie ma już więcej znaczników, odtwarzanie trasy się kończy.

## Podsumowanie
Mamy nadzieję, że dało to pewien wgląd w to, jak działa aplikacja. Aplikacja jest stale rozwijana na podstawie opinii użytkowników, więc prosimy o kontakt, jeśli jest coś, co Twoim zdaniem można by dodać.
