---
title: "Polski"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-pl/
content_lang: pl
---

# Tłumaczenie na polski — prośba o opinię

> Odpowiedzi wyślij e-mailem na adres **soundscapeAndroid@scottishtecharmy.support**, podając język w temacie wiadomości.


*Polish translation, round 3 — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Cześć i dziękujemy za odpowiedzi na drugą rundę pytań. Prawie wszystkie zostały
już wprowadzone (zob. „Co ustaliliśmy” niżej). Zostały tylko trzy krótkie pytania.
**Nie musisz znać ani instalować aplikacji:** przy każdym pytaniu jest napisane,
kiedy tekst słychać, jak brzmi po angielsku i jak brzmi teraz po polsku.

## Czym jest Soundscape?

Soundscape to bezpłatna aplikacja na telefon dla osób niewidomych i
słabowidzących. Używa się jej w trakcie chodzenia, ze słuchawkami, często z
telefonem w kieszeni. Nie prowadzi krok po kroku jak nawigacja („skręć w lewo”),
tylko mówi na głos, co jest w pobliżu, żeby można było samemu się zorientować.

Kilka pojęć, które pojawiają się w pytaniach:

- **Powiadomienie** *(callout)*: krótki komunikat głosowy o czymś, obok czego
  przechodzisz, np. „Kawiarnia” albo „Idziesz na północ wzdłuż Marszałkowska”.
  Słychać go przestrzennie, z kierunku, w którym to coś jest.
- **Dźwięk naprowadzający** *(audio beacon)*: gdy wybierzesz **cel**, w słuchawkach
  słychać regularny, powtarzający się dźwięk dobiegający z kierunku celu. Całą
  funkcję nazywamy **naprowadzaniem**.
- **Znacznik** i **trasa** *(marker, route)*: zapisane miejsca i ich kolejność
  (**punkty trasy**), przez którą dźwięk naprowadzający prowadzi po kolei.

Osoby niewidome obsługują telefon za pomocą **czytnika ekranu** (TalkBack na
Androidzie, VoiceOver na iPhonie): każdy przycisk i tekst czyta syntetyczny głos.
Teksty aplikacji są więc prawie zawsze *słuchane*, a nie czytane, i to często w
ulicznym hałasie.

Pojęcia są dokładniej opisane (po angielsku)
[na tej stronie]({{ "/developers/translation-terminology.html" | relative_url }}).

## Co ustaliliśmy

Z pierwszej rundy:

- **Powiadomienie** zostaje, a czasownik to **„powiadamia o”**.
- **Dźwięk naprowadzający** to sam dźwięk, **cel** to miejsce, do którego
  prowadzi, a **naprowadzanie** to funkcja. **Punkt trasy** tylko dla punktów
  zapisanej trasy.
- **Znacznik** bez „(pinezka)”, **forma „ty”**, **ślepa ulica**, **„Znak
  nawigacyjny”**, nazwy ulic w mianowniku.

Z drugiej rundy:

- **„Do celu: 105 metrów”**, także w komunikacie „Ustawiono naprowadzanie na …,
  do celu: 2 metry”.
- Ustawienie nazywa się teraz **„Powiadomienia o miejscach”**.
- Poziomy **Szczegółowy / Uproszczony / Podstawowy / Wyciszony** zostają.
- **„Jesteś zwrócony”** wróciło wszędzie, także w pomocy, zamiast „patrzysz”.
- Tunel: **„Wchodzisz do tunelu”** pieszo i **„Wjeżdżasz do tunelu”** w
  pojeździe lub w pociągu.
- Dźwięk „Drop” to teraz **„Upuszczanie”**, a nazwy dźwięków naprowadzających
  w ustawieniach są już po polsku (wcześniej zostawały po angielsku).
- Godziny zostają opcją: domyślnie aplikacja mówi „Po lewej” / „Po prawej”.

## Jak odpowiedzieć

Odpisz mailem, podając numer pytania („Q2: lepiej brzmiałoby…”). Nie musisz
odpowiadać na wszystko. Jeśli coś jest dobre, też warto napisać „OK”.

---

### Q1 — Forma męska w innych tekstach *(Gender beyond "facing")*

**Kiedy to słychać:** w różnych miejscach: na powitaniu po pierwszym
uruchomieniu, w samouczku i w wiadomości, którą wysyła się innej osobie razem
ze znacznikiem.

**Po angielsku:** „You are ready for your first walk with Soundscape.”,
„When you're ready to stop the beacon, tap the Stop Route button.”,
„I've shared a Soundscape marker with you: …”.

**Jak brzmi teraz:** „Wszystko gotowe na pierwszy spacer z Soundscape.”,
„Gdy zechcesz zatrzymać naprowadzanie, stuknij przycisk „Zatrzymaj trasę”.”,
„Udostępniam Ci znacznik Soundscape: …”.

Wcześniej było: „Jesteś gotowy na pierwszy spacer z Soundscape.”, „Gdy będziesz
gotowy, aby zatrzymać naprowadzanie…”, „Udostępniłem Ci znacznik Soundscape: …”.

**Co budzi wątpliwości:** z odpowiedzi na drugą rundę wynika, że forma męska
jest w polszczyźnie zwykłą formą ogólną, dlatego przywróciliśmy „jesteś
zwrócony”. Te trzy teksty (i kilka podobnych w pomocy)
przerobiliśmy wcześniej na formy bezosobowe z tego samego powodu.

**Pytanie:** czy obecne brzmią naturalnie i mogą zostać, czy lepiej wrócić do
dawnych form męskich?

### Q2 — „piętnastu metrów” z syntezatorem Google *(Distances with Google TTS)*

**Kiedy to słychać:** gdy aplikacja mówi, gdzie coś jest, z odległością i
godziną.

**Po angielsku:** „Pedestrian crossing, 15 metres, at 9 o’clock”.

**Jak brzmi teraz:** tekst to „Przejście dla pieszych, 15 metrów, na godzinie
dziewiątej”. Godzina jest już słowem, nie cyfrą.

**Co budzi wątpliwości:** w pierwszej rundzie syntezator przeczytał „piętnastu
metrów”, choć w tekście jest „15 metrów”. Nie wiemy, czy to się powtarza teraz,
gdy po przecinku jest już „na godzinie dziewiątej”, a nie cyfra.

**Pytanie:** po aktualizacji, z włączonymi godzinami w ustawieniach, czy Google
nadal czyta „piętnastu metrów”? Jeśli tak, przy jakich jeszcze liczbach?

### Q3 — Rodzaje dróg *(Road types)*

**Kiedy to słychać:** gdy aplikacja nazywa drogę według jej rodzaju, np. drogę
bez nazwy w danych mapy.

**Po angielsku:** „Motorway”, „Trunk Road”, „Highway”.

**Jak brzmi teraz:** „Autostrada”, „Droga główna”, „Droga główna”.

**Co budzi wątpliwości:** w drugiej rundzie okazało się, że trudno to
rozstrzygnąć. Padły propozycje „Droga tranzytowa” (Trunk Road) i „Droga” (Highway), z uwagą, że w
polskich danych mapy „motorway” obejmuje też większość dróg ekspresowych (S).

**Pytanie:** tylko jeśli znasz kogoś, kto zna polskie oznaczenia w
OpenStreetMap: jak nazwałby te trzy rodzaje dróg w krótkim komunikacie
głosowym? Jeśli nie, zostawiamy to na razie tak, jak jest.

### Q4 — Coś jeszcze? *(Anything else)*

Po aktualizacji aplikacji chętnie przyjmiemy uwagi z używania jej w praktyce:
jeśli jakieś zdanie brzmi jak tłumaczenie z angielskiego, jest za długie albo
niejasne, daj znać.

Bardzo dziękujemy!
