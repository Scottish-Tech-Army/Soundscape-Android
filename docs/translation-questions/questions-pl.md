---
title: "Polski"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-pl/
---

# Tłumaczenie na polski — prośba o opinię

> Odpowiedzi wyślij e-mailem na adres **soundscapeAndroid@scottishtecharmy.support**, podając język w temacie wiadomości.


*Polish translation, round 2 — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Cześć i jeszcze raz dziękujemy za odpowiedzi na pierwszą rundę pytań. Wszystkie
zostały już wprowadzone do aplikacji (zob. „Co ustaliliśmy” niżej). Zostało kilka
drobnych pytań, które wyniknęły z tych zmian. **Nie musisz znać ani instalować
aplikacji:** przy każdym pytaniu jest napisane, kiedy tekst słychać, jak brzmi po
angielsku i jak brzmi teraz po polsku.

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

- **Powiadomienie** zostaje, a czasownik to **„powiadamia o”** („Tryb Szczegółowy
  powiadamia o wszystkim w pobliżu”).
- **Dźwięk naprowadzający** to sam dźwięk, **cel** to miejsce, do którego
  prowadzi („Odległość do celu”, „Informacje o celu”), a **naprowadzanie** to
  funkcja („Brak aktywnego naprowadzania”). **Punkt trasy** tylko dla punktów
  zapisanej trasy.
- **Znacznik** bez „(pinezka)”, także na przycisku „Bliskie znaczniki”.
- **Forma „ty”** zostaje.
- **Ślepa ulica** zamiast „ślepa uliczka”; na mapie **„Znak nawigacyjny”**.
- **Nazwy ulic** zostają w mianowniku, jak w innych nawigacjach.
- **Kierunki** („Idziesz” / „Poruszasz się”) bez zmian.

## Jak odpowiedzieć

Odpisz mailem, podając numer pytania („Q2: lepiej brzmiałoby…”). Nie musisz
odpowiadać na wszystko. Jeśli coś jest dobre, też warto napisać „OK”.

---

### Q1 — „Do celu: 105 metrów” *(Distance to the beacon)*

**Kiedy to słychać:** co jakiś czas podczas marszu do celu, jeśli dźwięk
naprowadzający jest włączony.

**Po angielsku:** „Beacon is currently 105 metres away”.

**Jak brzmi teraz:** „Do celu: 105 metrów”. Wcześniej było „Naprowadzanie jest
obecnie w odległości 105 metrów”.

**Co budzi wątpliwości:** wybraliśmy tę formę, bo po „w odległości” liczba
musiałaby być w dopełniaczu („w odległości dwóch metrów”), a aplikacja podaje
ją w mianowniku („2 metry”). W jednym innym komunikacie wciąż jest stara forma:
„Ustawiono naprowadzanie na %1$s, w odległości 2 metry”.

**Pytanie:** czy „Do celu: 105 metrów” brzmi naturalnie? Czy tamten drugi
komunikat zmienić tak samo („…, do celu: 2 metry”)?

### Q2 — „Powiadamiaj o miejscach” *(Places to Call Out)*

**Kiedy to słychać:** w ustawieniach, nad listą rodzajów miejsc, o których
aplikacja ma mówić (Wszystko, Punkty orientacyjne, Transport publiczny…).

**Po angielsku:** „Places to Call Out”.

**Jak brzmi teraz:** „Powiadamiaj o miejscach”. Wcześniej było „Miejsca do
ogłaszania”.

**Co budzi wątpliwości:** to nasza propozycja, żeby pozbyć się „ogłaszania”.

**Pytanie:** czy tak jest dobrze, czy lepiej „Miejsca w powiadomieniach” albo coś
innego?

### Q3 — Godziny i odległości na głos *(Clock positions and distances)*

**Kiedy to słychać:** gdy aplikacja mówi, gdzie coś jest, np. „Przejście dla
pieszych, 15 metrów, na godzinie dziewiątej”.

**Po angielsku:** „15 metres, at 9 o'clock”.

**Jak brzmi teraz:** godzina jest teraz zapisana słowem: „na godzinie
dziewiątej”, a nie cyfrą, którą syntezator czytał „dziewięciu”. Domyślnie
aplikacja mówi teraz jednak „z lewej” / „z prawej” zamiast godzin, a godziny są
opcją w ustawieniach.

**Co budzi wątpliwości:** nie wiemy, skąd wzięło się „piętnastu metrów”: tekst to
„15 metrów”, więc tak odmienił to syntezator.

**Pytanie:** z jakiego syntezatora mowy korzystasz (Google, Samsung, eSpeak,
Vocalizer…)? Czy „na godzinie dziewiątej” brzmi dobrze, czy naturalniej
„na dziewiątej”?

### Q4 — Poziomy szczegółowości po zmianie nazw *(Detail levels after the rename)*

**Kiedy to słychać:** w ustawieniach, gdzie na słuch wybiera się, ile aplikacja
mówi w trakcie chodzenia.

**Po angielsku:** „Detailed / Simplified / Essential / Silent”. Angielskie nazwy
też się zmieniły, bo osoby z kilku krajów miały z nimi kłopot.

**Jak brzmi teraz:** Szczegółowy / Uproszczony / Podstawowy / Wyciszony. Tryb
Uproszczony pomija mniejsze ścieżki i rzadziej się powtarza; Tryb Podstawowy
powiadamia tylko o ulicach, skrzyżowaniach i punktach orientacyjnych.

**Co budzi wątpliwości:** zaproponowałeś/aś „Uproszczony” dla trzeciego poziomu,
tego, który mówi tylko to, co niezbędne. Po zmianie angielskich nazw
„Uproszczony” przesunął się o jeden poziom wyżej, a trzeci to teraz
„Podstawowy”.

**Pytanie:** czy taki układ jest w porządku, czy wolisz inne nazwy dla dwóch
środkowych poziomów?

### Q5 — Coś jeszcze? *(Anything else)*

Jeśli jakieś zdanie brzmi jak tłumaczenie z angielskiego, jest za długie albo
niejasne, daj znać.

Bardzo dziękujemy!
