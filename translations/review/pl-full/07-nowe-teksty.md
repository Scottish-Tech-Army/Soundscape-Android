<!-- Moved 2026-09-25 from docs/translation-questions/questions-pl.md, which now asks the
numbered open questions like every other language. This per-text review of the 27 strings
added 2026-09-23 is kept here, unpublished, alongside the full-corpus pack. -->

# Tłumaczenie na polski — prośba o opinię

> Odpowiedzi wyślij e-mailem na adres **soundscapeAndroid@scottishtecharmy.support**, podając język w temacie wiadomości.


*Polish translation — request for review*

Cześć i dziękujemy, że zgodziłeś/aś się na to zerknąć.

Polskie tłumaczenie powstało bez udziału native speakera, więc Twoja opinia jest dla nas naprawdę cenna. **Nie musisz znać ani instalować aplikacji:** przy każdym tekście jest napisane, gdzie się pojawia i jak brzmi po angielsku.

## Czym jest Soundscape?

Soundscape to bezpłatna aplikacja na telefon dla osób niewidomych i słabowidzących. Używa się jej w trakcie chodzenia, ze słuchawkami, często z telefonem w kieszeni. Nie prowadzi krok po kroku jak nawigacja („skręć w lewo”), tylko mówi na głos, co jest w pobliżu, żeby można było samemu się zorientować.

Kilka pojęć, które pojawiają się niżej:

- **Powiadomienie** *(callout)*: krótki komunikat głosowy o czymś, obok czego przechodzisz, np. „Kawiarnia” albo „Ulica Główna w lewo”. Słychać go przestrzennie, z kierunku, w którym to coś jest.
- **Dźwięk naprowadzający** *(audio beacon)*: gdy wybierzesz cel, w słuchawkach słychać regularny, powtarzający się dźwięk dobiegający z kierunku celu. Gdy się obrócisz, dźwięk „przesuwa się”, więc do celu można dojść na słuch.
- **Znacznik** i **trasa** *(marker, route)*: zapisane miejsca i ich kolejność, przez którą dźwięk naprowadzający prowadzi po kolei.

Osoby niewidome obsługują telefon za pomocą **czytnika ekranu** (TalkBack na Androidzie, VoiceOver na iPhonie): każdy przycisk i tekst czyta syntetyczny głos. Teksty aplikacji są więc prawie zawsze *słuchane*, a nie czytane, i to często w ulicznym hałasie. Najważniejsze, żeby były krótkie, jasne i naturalne w odbiorze na słuch.

Pojęcia są dokładniej opisane (po angielsku) [na tej stronie](https://scottish-tech-army.github.io/Soundscape-Android/developers/translation-terminology.html).

Poniżej są **nowe teksty**, które właśnie dodaliśmy — nikt ich jeszcze nie sprawdzał. Na początku jest też **słowniczek** kluczowych pojęć, których używamy w całej aplikacji.

## Jak odpowiedzieć

Najprościej: **odpisz mailem, podając numer tekstu i swoją uwagę** (np. „Tekst 3: lepiej brzmiałoby…”). Nie przejmuj się formatowaniem ani tym, że nie skomentujesz wszystkiego — nawet kilka uwag bardzo pomoże.

Przydatne nam rzeczy:

- Czy brzmi to naturalnie po polsku, czy jak tłumaczenie z angielskiego?
- Czy to słowo, którego użyłaby polska aplikacja mapowa (Mapy Google, Maps.me)?
- Czy dobrze się tego **słucha**? Prawie wszystko tu jest czytane przez syntezator mowy, a nie oglądane na ekranie.
- Czy forma grzecznościowa („możesz” / „Ty”) jest w porządku? Teraz jest nieformalna, tak jak w reszcie aplikacji.

Jeśli coś jest po prostu dobre — też warto napisać „OK”. Wtedy wiemy, że tego nie ruszać.

Teksty są ponumerowane („Tekst 1”, „Tekst 2”…), a pojęcia ze słowniczka możesz wskazać po nazwie.

Dwie rzeczy, które można pominąć:

- **Gwiazdki** (`*tak*`) to formatowanie — pogrubienie w aplikacji. Proszę zostawić je tam, gdzie są.
- **`%1$s`** to miejsce, w które aplikacja wstawia słowo (np. nazwę poziomu). Musi zostać — ale może być w innym miejscu zdania, jeśli tak jest po polsku naturalniej.

---

## Słowniczek — pojęcia używane w całej aplikacji

To są najważniejsze decyzje. Jeśli któreś słowo jest nietrafione, wpływa to na dziesiątki albo setki tekstów — dlatego pytamy o nie osobno.

### powiadomienie

- **Po angielsku:** Callout
- **Co to znaczy:** Krótki komunikat głosowy o tym, co jest w pobliżu — np. „Kawiarnia”, „Ulica Główna w lewo”. Serce aplikacji.

**Uwagi:**

### dźwięk naprowadzający

- **Po angielsku:** Audio Beacon
- **Co to znaczy:** Ciągły dźwięk dobiegający z kierunku celu. Nie prowadzi krok po kroku — pomaga trzymać kierunek.

**Uwagi:**

### znacznik

- **Po angielsku:** Marker
- **Co to znaczy:** Zapisane miejsce z własną nazwą (dom, praca, wejście do budynku).

**Uwagi:**

### punkt trasy

- **Po angielsku:** Waypoint
- **Co to znaczy:** Pojedynczy punkt na trasie. Aplikacja ogłasza dotarcie i przenosi dźwięk naprowadzający na następny.

**Uwagi:**

### punkty orientacyjne

- **Po angielsku:** Landmarks
- **Co to znaczy:** Parki, kościoły i inne obiekty służące do orientacji.

**Uwagi:**

### skrzyżowanie

- **Po angielsku:** Intersection / Junction
- **Co to znaczy:** Miejsce, gdzie spotykają się ulice.

**Uwagi:**

### Tryb uśpienia / Tryb drzemki

- **Po angielsku:** Sleep / Snooze
- **Co to znaczy:** Uśpienie milczy do ręcznego wybudzenia; drzemka budzi się, gdy ruszysz dalej.

**Uwagi:**

---

## Nowe ustawienie: poziom szczegółowości

Zastępuje dawny przełącznik „Zezwól na powiadomienia”. Zamiast włącz/wyłącz są teraz cztery poziomy.

### Tekst 1

**Gdzie się pojawia:** Ustawienia → Zarządzaj powiadomieniami. Nazwa suwaka.

**Oryginał (angielski):**

```
Callout Detail
```

**Tłumaczenie (polskie):**

```
Szczegółowość powiadomień
```

**Uwagi:**

### Tekst 2

**Gdzie się pojawia:** Ustawienia → Zarządzaj powiadomieniami. Opis pod suwakiem.

**Oryginał (angielski):**

```
Callout detail controls how much Soundscape tells you about as you walk. Detailed mode calls out everything nearby. Balanced mode leaves out minor paths and service roads, and repeats itself less often. Quiet mode calls out streets, junctions and landmarks only. Silent mode makes no automatic callouts at all, leaving beacons, routes and the buttons on the home screen.
```

**Tłumaczenie (polskie):**

```
Szczegółowość powiadomień określa, jak dużo Soundscape mówi ci podczas chodzenia. Tryb Szczegółowy ogłasza wszystko w pobliżu. Tryb Zrównoważony pomija mniejsze ścieżki i drogi serwisowe oraz rzadziej się powtarza. Tryb Cichy ogłasza tylko ulice, skrzyżowania i punkty orientacyjne. Tryb Wyciszony nie tworzy żadnych automatycznych powiadomień, pozostawiając dźwięki naprowadzające, trasy i przyciski na ekranie głównym.
```

**Uwagi:**

### Tekst 3

**Gdzie się pojawia:** Nazwa poziomu. Wypowiadana przez Siri i przez przycisk na słuchawkach.

**Oryginał (angielski):**

```
Detailed
```

**Tłumaczenie (polskie):**

```
Szczegółowy
```

**Uwagi:**

### Tekst 4

**Gdzie się pojawia:** Nazwa poziomu. Wypowiadana przez Siri i przez przycisk na słuchawkach.

**Oryginał (angielski):**

```
Balanced
```

**Tłumaczenie (polskie):**

```
Zrównoważony
```

**Uwagi:**

### Tekst 5

**Gdzie się pojawia:** Nazwa poziomu. Wypowiadana przez Siri i przez przycisk na słuchawkach.

**Oryginał (angielski):**

```
Quiet
```

**Tłumaczenie (polskie):**

```
Cichy
```

**Uwagi:**

### Tekst 6

**Gdzie się pojawia:** Nazwa poziomu. Wypowiadana przez Siri i przez przycisk na słuchawkach.

**Oryginał (angielski):**

```
Silent
```

**Tłumaczenie (polskie):**

```
Wyciszony
```

**Uwagi:**

### Tekst 7

**Gdzie się pojawia:** Wypowiadane przez asystenta po zmianie poziomu. %1$s to nazwa poziomu.

**Oryginał (angielski):**

```
Callout detail: %1$s
```

**Tłumaczenie (polskie):**

```
Szczegółowość powiadomień: %1$s
```

**Uwagi:**

### Tekst 8

**Gdzie się pojawia:** Wypowiadane przez asystenta, gdy poprosisz o nieistniejący poziom. %1$s to to, o co poprosiłeś/aś.

**Oryginał (angielski):**

```
There is no callout detail level called %1$s. Choose Silent, Quiet, Balanced or Detailed.
```

**Tłumaczenie (polskie):**

```
Nie ma poziomu szczegółowości o nazwie %1$s. Wybierz Wyciszony, Cichy, Zrównoważony albo Szczegółowy.
```

**Uwagi:**

---

## Nowe ustawienia: co jest ogłaszane

### Tekst 9

**Gdzie się pojawia:** Ustawienia → Zarządzaj powiadomieniami. Przełącznik.

**Oryginał (angielski):**

```
Streets and Junctions
```

**Tłumaczenie (polskie):**

```
Ulice i skrzyżowania
```

**Uwagi:**

### Tekst 10

**Gdzie się pojawia:** Opis pod przełącznikiem „Ulice i skrzyżowania”.

**Oryginał (angielski):**

```
Intersections ahead and the road you are on
```

**Tłumaczenie (polskie):**

```
Skrzyżowania przed tobą i ulica, którą idziesz
```

**Uwagi:**

### Tekst 11

**Gdzie się pojawia:** Ustawienia → Zarządzaj powiadomieniami. Lista rodzajów miejsc do zaznaczenia.

**Oryginał (angielski):**

```
Places to Call Out
```

**Tłumaczenie (polskie):**

```
Miejsca do ogłaszania
```

**Uwagi:**

### Tekst 12

**Gdzie się pojawia:** Opis nad listą rodzajów miejsc.

**Oryginał (angielski):**

```
Which kinds of place are called out as you pass them. Tick as many as you like, or tick Everything or No Places. Your markers are always called out.
```

**Tłumaczenie (polskie):**

```
Jakie rodzaje miejsc są ogłaszane, gdy je mijasz. Zaznacz dowolną liczbę albo zaznacz Wszystko lub Brak miejsc. Twoje znaczniki są ogłaszane zawsze.
```

**Uwagi:**

### Tekst 13

**Gdzie się pojawia:** Pozycja na liście „Miejsca do ogłaszania”: ogłaszane jest wszystko.

**Oryginał (angielski):**

```
Everything
```

**Tłumaczenie (polskie):**

```
Wszystko
```

**Uwagi:**

### Tekst 14

**Gdzie się pojawia:** Pozycja na liście „Miejsca do ogłaszania”: parki, kościoły itp.

**Oryginał (angielski):**

```
Landmarks
```

**Tłumaczenie (polskie):**

```
Punkty orientacyjne
```

**Uwagi:**

### Tekst 15

**Gdzie się pojawia:** Pozycja na liście „Miejsca do ogłaszania”: żadne miejsca.

**Oryginał (angielski):**

```
No Places
```

**Tłumaczenie (polskie):**

```
Brak miejsc
```

**Uwagi:**

---

## Siri i okno nowej wersji

### Tekst 16

**Gdzie się pojawia:** Nazwa skrótu Siri pod ikoną w aplikacji Skróty. Musi być krótka.

**Oryginał (angielski):**

```
Detail
```

**Tłumaczenie (polskie):**

```
Szczegóły
```

**Uwagi:**

### Tekst 17

**Gdzie się pojawia:** Etykieta pytania „który poziom?” przy poleceniu Siri.

**Oryginał (angielski):**

```
Level
```

**Tłumaczenie (polskie):**

```
Poziom
```

**Uwagi:**

### Tekst 18

**Gdzie się pojawia:** Opis akcji Siri, która ustawia poziom szczegółowości.

**Oryginał (angielski):**

```
Sets how much Soundscape says as you walk.
```

**Tłumaczenie (polskie):**

```
Ustawia, jak dużo Soundscape mówi podczas chodzenia.
```

**Uwagi:**

### Tekst 19

**Gdzie się pojawia:** Przycisk w oknie „nowa wersja”; otwiera pełne informacje o wersji w przeglądarce.

**Oryginał (angielski):**

```
Full release notes
```

**Tłumaczenie (polskie):**

```
Pełne informacje o wersji
```

**Uwagi:**

---

## Teksty pomocy i FAQ

Dłuższe teksty. Zmieniły się, bo opisywały stary przełącznik. Tu najbardziej liczy się, czy brzmią naturalnie — nie trzeba czytać słowo w słowo.

### Tekst 20

**Gdzie się pojawia:** Ekran pomocy: „Powiadomienia automatyczne”.

**Oryginał (angielski):**

```
**Turning callouts on or off :**

 Turning callouts off will silence the app. Callouts can be turned off in the *"Manage Callouts"* section of the *"Settings"* screen by setting *"Callout Detail"* to *"Silent"*, and turned back on by choosing any other level. You can do the same by asking Siri or Gemini. You can also make Soundscape quieter a step at a time with the media control buttons on your headphones: each press of *"previous"* moves down through *"Detailed"*, *"Balanced"*, *"Quiet"* and *"Silent"*, and one more press goes back to *"Detailed"*. See the *"Using Media Controls"* help topic. Alternatively, you can use the *"Sleep"* button in the top-right corner of the home screen to stop Soundscape from making callouts until you choose to wake it up again.
```

**Tłumaczenie (polskie):**

```
**Włączanie i wyłączanie powiadomień :**

 Wyłączenie powiadomień sprawi, że aplikacja będzie cicha. Powiadomienia można wyłączyć w sekcji *„Zarządzaj powiadomieniami”* na ekranie *„Ustawienia”*, ustawiając *„Szczegółowość powiadomień”* na *„Wyciszony”*, a włączyć z powrotem, wybierając dowolny inny poziom. To samo możesz zrobić, prosząc o to Siri lub Gemini. Możesz również stopniowo wyciszać Soundscape przyciskami multimedialnymi słuchawek: każde naciśnięcie *„poprzedni”* przechodzi o jeden poziom niżej przez *„Szczegółowy”*, *„Zrównoważony”*, *„Cichy”* i *„Wyciszony”*, a kolejne naciśnięcie wraca do *„Szczegółowy”*. Zobacz temat pomocy *„Sterowanie przyciskami multimedialnymi”*. Alternatywnie możesz użyć przycisku *„Tryb uśpienia”* w prawym górnym rogu ekranu głównego, aby zatrzymać ogłaszanie powiadomień przez Soundscape aż do momentu, gdy zdecydujesz się ją obudzić.
```

**Uwagi:**

### Tekst 21

**Gdzie się pojawia:** Ekran pomocy: „Powiadomienia automatyczne”, ciąg dalszy.

**Oryginał (angielski):**

```
**Managing which callouts you hear :**

 To choose the types of things Soundscape will automatically call out, go to the *"Settings"* screen using the menu on the *"Home"* screen. The *"Manage Callouts"* section of the *"Settings"* screen is where you choose how much the app says with *"Callout Detail"*, whether it calls out *"Streets and Junctions"*, and which *"Places to Call Out"*. If you wish to turn off all callouts, set *"Callout Detail"* to *"Silent"*.
```

**Tłumaczenie (polskie):**

```
**Zarządzanie powiadomieniami, które słyszysz :**

 Aby wybrać rodzaje informacji, które Soundscape będzie automatycznie ogłaszać, przejdź do ekranu *„Ustawienia”* z menu na ekranie *„Głównym”*. W sekcji *„Zarządzaj powiadomieniami”* na ekranie *„Ustawienia”* wybierasz, jak dużo aplikacja mówi, za pomocą *„Szczegółowość powiadomień”*, czy ogłasza *„Ulice i skrzyżowania”* oraz jakie są *„Miejsca do ogłaszania”*. Jeśli chcesz wyłączyć wszystkie powiadomienia, ustaw *„Szczegółowość powiadomień”* na *„Wyciszony”*.
```

**Uwagi:**

### Tekst 22

**Gdzie się pojawia:** Ekran pomocy: „Sterowanie przyciskami multimedialnymi”.

**Oryginał (angielski):**

```
There are 2 modes of operation for the media controls. The mode can be selected in the *Settings* *Media Controls* section. The modes are:

 Original mode. 

⏯ Play/Pause: Toggles the beacon audio on and off. 

⏭ Next: If a route is playing back move the audio beacon to the next Waypoint in the route. If no route is being played back callout *What's Around Me*.

⏮ Previous: If a route is playing back move the audio beacon to the previous Waypoint in the route. If no route is being played back change the *Callout Detail*, one step quieter with each press: *Detailed*, *Balanced*, *Quiet*, *Silent*, and back to *Detailed*.



Audio menu. 



⏭ Next moves through a series of menu options which the app describes via text to speech, returning to the first one after the last. ⏯ Play/Pause then triggers the app to perform the option described. There is a single top level menu which only has sub-menus as options. Each sub-menu has a group of similar possible actions.

⏮ Previous does not move through the menu. Instead it changes the *Callout Detail*, exactly as it does in Original mode, so the same button quietens the app whichever mode you use.
```

**Tłumaczenie (polskie):**

```
Elementy sterowania multimediami mają 2 tryby działania. Tryb można wybrać w sekcji *Ustawienia* *Sterowanie multimediami*. Tryby to:

 Tryb oryginalny. 

⏯ Odtwórz/Pauza: włącza i wyłącza dźwięk naprowadzający. 

⏭ Następny: Jeśli trwa odtwarzanie trasy, przenosi dźwięk naprowadzający do następnego punktu trasy. Jeśli żadna trasa nie jest odtwarzana, ogłasza *Wokół mnie*.

⏮ Poprzedni: Jeśli trwa odtwarzanie trasy, przenosi dźwięk naprowadzający do poprzedniego punktu trasy. Jeśli żadna trasa nie jest odtwarzana, zmienia *Szczegółowość powiadomień* o jeden poziom ciszej przy każdym naciśnięciu: *Szczegółowy*, *Zrównoważony*, *Cichy*, *Wyciszony* i z powrotem *Szczegółowy*.



Menu dźwiękowe. 



⏭ Następny przechodzi przez szereg opcji menu, które aplikacja opisuje za pomocą syntezatora mowy, i po ostatniej wraca do pierwszej. ⏯ Odtwórz/Pauza uruchamia następnie opisaną opcję. Istnieje jedno menu główne, którego elementami są tylko podmenu. Każde podmenu grupuje podobne akcje.

⏮ Poprzedni nie przechodzi przez menu. Zamiast tego zmienia *Szczegółowość powiadomień*, dokładnie tak jak w trybie oryginalnym, więc ten sam przycisk wycisza aplikację niezależnie od używanego trybu.
```

**Uwagi:**

### Tekst 23

**Gdzie się pojawia:** Najczęściej zadawane pytania: „Jak kontrolować to, co słyszę?”

**Oryginał (angielski):**

```
Soundscape provides several ways to control what you hear and when:

1. Immediately stop all audio: Double tap the screen with two fingers to immediately turn off all audio, including any callout that is currently playing and the beacon if it is on. Callouts will resume automatically when you approach the next intersection or point of interest, but the audible beacon will not. Select the *"unmute beacon button"* on the main screen to resume hearing the beacon.

2. Stop automatic callouts: When you are not traveling or have reached a destination, you probably will not need Soundscape to continue to notify you of things around you. Instead of exiting the app, you can put Soundscape in to Snooze Mode and it will wake up again when you leave, or you can put Soundscape into Sleep Mode and it will stay off until you choose to turn it back on. Alternatively, you can select *"Settings"* from the menu and set *"Callout Detail"* to *"Silent"* in the *"Manage Callouts"* section.

3. Stop the beacon: There are several scenarios where you might set a destination but not need the audible beacon on. For example, you may know exactly how to get to your destination but still want automatic updates about how far away you are. Or you might only need the audio beacon as you near your destination. Whatever the case, you can choose when to hear the beacon by toggling the *"mute beacon"*/*"unmute beacon"* button on the main screen.

If you still want to interact with Soundscape but don’t want to hear automatic callouts, you can set *"Callout Detail"* to *"Silent"* in the *"Manage Callouts"* section of the *"Settings"* screen from the menu. Or, if you aren’t going to be using Soundscape, you can put it in either Sleep or Snooze Mode using the *"Sleep"* button on the home screen.
```

**Tłumaczenie (polskie):**

```
Soundscape oferuje kilka sposobów kontrolowania tego, co i kiedy słyszysz:

1. Natychmiastowe wyłączenie wszystkich dźwięków: stuknij dwoma palcami ekran, aby natychmiast wyłączyć wszystkie dźwięki, w tym aktualnie odtwarzane powiadomienia i dźwięk naprowadzający, jeśli jest włączony. Powiadomienia wznowią się automatycznie, gdy zbliżysz się do następnego skrzyżowania lub punktu zainteresowania, natomiast dźwięk naprowadzający nie. Aby ponownie usłyszeć naprowadzanie, wybierz na ekranie głównym przycisk *„Włącz dźwięk naprowadzający”*.

2. Wyłączenie automatycznych powiadomień: gdy nie podróżujesz lub dotarłeś na miejsce, prawdopodobnie nie chcesz, aby Soundscape nadal informował Cię o otoczeniu. Zamiast zamykać aplikację, możesz przełączyć Soundscape w Tryb drzemki — obudzi się on ponownie, gdy opuścisz miejsce — albo w Tryb uśpienia — pozostanie wtedy wyłączony do momentu ręcznego ponownego uruchomienia. Alternatywnie, w menu wybierz *„Ustawienia”* i w sekcji *„Zarządzaj powiadomieniami”* ustaw *„Szczegółowość powiadomień”* na *„Wyciszony”*.

3. Wyłączenie naprowadzania: zdarzają się sytuacje, gdy ustawisz cel, ale nie potrzebujesz włączonego dźwięku naprowadzającego. Na przykład możesz dokładnie wiedzieć, jak dotrzeć do celu, ale nadal chcieć otrzymywać automatyczne informacje o odległości, albo potrzebować naprowadzania jedynie w miarę zbliżania się do celu. W każdej z tych sytuacji możesz zdecydować, kiedy słyszeć naprowadzanie, przełączając na ekranie głównym przycisk *„Wycisz dźwięk naprowadzający”*/*„Włącz dźwięk naprowadzający”*.

Jeśli chcesz nadal korzystać z Soundscape, ale nie chcesz słyszeć automatycznych powiadomień, ustaw *„Szczegółowość powiadomień”* na *„Wyciszony”* w sekcji *„Zarządzaj powiadomieniami”* na ekranie *„Ustawienia”* w menu. Jeśli natomiast nie zamierzasz używać Soundscape, możesz przełączyć aplikację w Tryb uśpienia lub Tryb drzemki, korzystając z przycisku *„Tryb uśpienia”* na ekranie głównym.
```

**Uwagi:**

### Tekst 24

**Gdzie się pojawia:** Najczęściej zadawane pytania: wskazówka.

**Oryginał (angielski):**

```
To keep using Soundscape without hearing automatic callouts, set *"Callout Detail"* to *"Silent"* in the *"Manage Callouts"* section of the *"Settings"* screen from the menu. If you are not going to use Soundscape for a while, you can put it in Sleep or Snooze mode instead using the *"Sleep"* button on the home screen.
```

**Tłumaczenie (polskie):**

```
Aby dalej korzystać z Soundscape bez słuchania automatycznych powiadomień, ustaw *„Szczegółowość powiadomień”* na *„Wyciszony”* w sekcji *„Zarządzaj powiadomieniami”* na ekranie *„Ustawienia”* w menu. Jeśli przez jakiś czas nie zamierzasz używać Soundscape, możesz zamiast tego przełączyć aplikację w Tryb uśpienia lub Tryb drzemki, korzystając z przycisku *„Tryb uśpienia”* na ekranie głównym.
```

**Uwagi:**

### Tekst 25

**Gdzie się pojawia:** Ekran pomocy: lista poleceń dla asystenta (Android).

**Oryginał (angielski):**

```
You can ask Soundscape to:

Describe *"My Location"*, what is *"Around Me"*, or what is *"Ahead of Me"*.

Call out the saved markers near you.

Start one of your saved routes by name, move on to the next waypoint, go back to the previous one, mute the beacon, or stop the route.

Set an audio beacon on one of your saved markers by name, or switch the beacon off.

Set the callout detail to *"Silent"*, *"Quiet"*, *"Balanced"* or *"Detailed"*, to change how much Soundscape says as you walk. Silent turns automatic callouts off.

Read back the names of your saved routes or your saved markers.
```

**Tłumaczenie (polskie):**

```
Możesz poprosić Soundscape, aby:

Opisał *"Moja lokalizacja"*, co jest *"Wokół mnie"* albo co jest *"Przede mną"*.

Ogłosił zapisane znaczniki w twoim pobliżu.

Uruchomił jedną z twoich zapisanych tras po nazwie, przeszedł do następnego punktu trasy, wrócił do poprzedniego, wyciszył dźwięk naprowadzający albo zatrzymał trasę.

Ustawił dźwięk naprowadzający na jednym z twoich zapisanych znaczników po nazwie albo go wyłączył.

Ustawił szczegółowość powiadomień na *"Wyciszony"*, *"Cichy"*, *"Zrównoważony"* albo *"Szczegółowy"*, aby zmienić, jak dużo Soundscape mówi podczas chodzenia. Wyciszony wyłącza automatyczne powiadomienia.

Odczytał nazwy twoich zapisanych tras albo twoich zapisanych znaczników.
```

**Uwagi:**

### Tekst 26

**Gdzie się pojawia:** Ekran pomocy o Siri. Tylko iOS.

**Oryginał (angielski):**

```
Every command has the same shape: *"Soundscape"*, then a group, then your choice. You can say the whole thing at once, for example *"Soundscape surroundings around me"*, or you can stop after the group and Siri will ask which one you want. So each group works as a spoken menu, and there are only seven of them to remember.
```

**Tłumaczenie (polskie):**

```
Wszystkie polecenia mają tę samą postać: *"Soundscape"*, potem grupa, potem twój wybór. Możesz powiedzieć wszystko naraz, na przykład *"Soundscape surroundings around me"*, albo zatrzymać się po grupie, a wtedy Siri zapyta, o którą chodzi. Każda grupa działa więc jak mówione menu, a jest ich tylko siedem do zapamiętania.
```

**Uwagi:**

### Tekst 27

**Gdzie się pojawia:** Ekran pomocy: lista poleceń dla Siri. Tylko iOS.

**Oryginał (angielski):**

```
*"Soundscape surroundings"* — then My Location, Around Me, Ahead of Me or Nearby Markers.

*"Soundscape route"* — then Next Waypoint, Previous Waypoint, Mute Beacon or Stop.

*"Soundscape start route"* — then the name of one of your saved routes.

*"Soundscape beacon"* — then the name of one of your saved markers.

*"Soundscape stop beacon"*.

*"Soundscape detail"* — then Silent, Quiet, Balanced or Detailed, to change how much Soundscape says as you walk. Silent turns automatic callouts off.

*"Soundscape list"* — then Routes, Markers or Commands.

Saying *"Soundscape list commands"* reads this list of commands back to you.
```

**Tłumaczenie (polskie):**

```
*"Soundscape surroundings"* — potem Moja lokalizacja, Wokół mnie, Przede mną albo Bliskie znaczniki.

*"Soundscape route"* — potem Następny punkt trasy, Poprzedni punkt trasy, Wycisz dźwięk naprowadzający albo Zatrzymaj.

*"Soundscape start route"* — potem nazwa jednej z twoich zapisanych tras.

*"Soundscape beacon"* — potem nazwa jednego z twoich zapisanych znaczników.

*"Soundscape stop beacon"*.

*"Soundscape detail"* — potem Wyciszony, Cichy, Zrównoważony albo Szczegółowy, aby zmienić, jak dużo Soundscape mówi podczas chodzenia. Wyciszony wyłącza automatyczne powiadomienia.

*"Soundscape list"* — potem Trasy, Znaczniki albo Polecenia.

Powiedzenie *"Soundscape list commands"* odczytuje ci tę listę poleceń.
```

**Uwagi:**

---

## Pytania, na które szczególnie czekamy

1. **Nazwy czterech poziomów.** Po angielsku to Detailed / Balanced / Quiet / Silent, a u nas *Szczegółowy / Zrównoważony / Cichy / Wyciszony*. Czy *Cichy* i *Wyciszony* dostatecznie się od siebie różnią, gdy usłyszy się je na głos? To osobne poziomy i użytkownik musi je odróżnić ze słuchu.
2. **„powiadomienie” na *callout*.** To słowo kojarzy się w telefonie z powiadomieniami systemowymi, a tu chodzi o komunikat głosowy o otoczeniu. Czy to myli? Jeśli tak — co byłoby lepsze?
3. **„dźwięk naprowadzający” na *beacon*.** Poprawne, ale długie — pada w wielu tekstach i bywa czytane na głos. Jest krótszy odpowiednik, który nadal byłby jasny?
4. **Forma zwracania się do użytkownika.** Teraz jest na „Ty”. Czy w aplikacji tego typu nie powinno być raczej formalnie („Pan/Pani”)?

---

Dziękujemy! — zespół Soundscape
