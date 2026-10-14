---
title: "Magyar"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-hu/
---

# A Soundscape magyar fordítása — néhány kérdés

> Küldje el a válaszait e-mailben a **soundscapeAndroid@scottishtecharmy.support** címre, és a tárgyban adja meg a nyelvet.


*Hungarian translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Üdvözöljük, és köszönjük, hogy vállalta, hogy átnézi.

A Soundscape magyar fordítása nagyrészt anyanyelvi segítség nélkül
készült, ezért a véleménye nagyon értékes számunkra. **Nem kell ismernie
vagy telepítenie az alkalmazást:** minden kérdésnél megtalálja, hol hangzik
el a szöveg, mi az angol eredeti, és most hogyan szól magyarul.

## Mi a Soundscape?

A Soundscape egy ingyenes telefonos alkalmazás vak és gyengénlátó emberek
számára. Séta közben használják, fülhallgatóval, a telefon gyakran a
zsebben van. Az alkalmazás nem úgy vezet, mint egy navigáció („forduljon
balra”), hanem hangosan elmondja, mi van a közelben, hogy a felhasználó
maga tájékozódhasson.

Néhány fogalom, amely a kérdésekben előkerül:

- **Bemondás** *(callout)*: rövid hangüzenet arról, ami mellett éppen
  elhalad, például „Kávézó”, „Járda a Fő utca mellett” vagy „Gyaloglás
  észak felé az Andrássy úton”. Térhatású hangon szól, abból az irányból,
  ahol a dolog van.
- **Irányjelző hang** *(audio beacon)*: ha kiválaszt egy célpontot, egy
  folyamatos, ismétlődő hang szól, amely a fülhallgatóban a célpont
  irányából hallatszik. Ha elfordul, a hang is „elmozdul”, így hallásból
  lehet a cél felé menni.
- **Jelölő** és **útvonal** *(marker, route)*: elmentett helyek, illetve
  ezek sorozata, amelyen az irányjelző hang végigvezet.

A vak felhasználók **képernyőolvasóval** kezelik a telefont (Androidon
TalkBack, iPhone-on VoiceOver): minden gombot és szöveget gépi hang olvas
fel. Ezért az alkalmazás szövegeit szinte mindig *hallják*, nem olvassák, és
gyakran séta közben, zajban. A legfontosabb tehát, hogy hallgatva rövid,
érthető és természetes legyen.

Ha érdekli, a fogalmak részletes leírása angolul
[itt olvasható]({{ "/developers/translation-terminology.html" | relative_url }}).

## Amit már eldöntöttünk *(Settled, 2026-10-01)*

Egy anyanyelvi beszélő válaszai alapján:

- Az alkalmazás mindenhol **magáz** (korábban a gombok és a bevezető
  tegeztek, a súgó magázott).
- *Audio beacon* = **irányjelző hang** (korábban „hangjelző”).
- *Callout* = mindenhol **bemondás** (korábban „bejelentés”, „közlés” és
  „bemondás” keveredett).
- Az „a/az” névelőt és az utcanév ragozását az alkalmazás maga választja
  ki: „az Andrássy úton”, „a Váci utcán”. Az útszámok mostantól képzőt
  kapnak: „az M7-es úton”, „a 8-as úton”.
- A négy részletességi szint neve marad: Részletes / Egyszerűsített /
  Alapvető / Néma.

## Hogyan válaszoljon

Egyszerűen válaszoljon e-mailben, és írja meg a kérdés számát („Q1:
szerintem…”). Nem kell mindenre válaszolnia. Ha valami már jó, egy rövid
„OK” is sokat segít.

---

### Q1 — Autópályák: „úton” vagy „autópályán”? *(Motorways)*

**Mikor hallja:** autóban vagy buszon utazva (utazási mód), amikor az
alkalmazás bemondja, melyik úton halad.

**Angolul:** „Traveling north along M7”

**Most így szól:** „Haladás észak felé az M7-es úton”

**Amiben bizonytalanok vagyunk:** az alkalmazás az útszámból nem tudja,
hogy autópályáról, autóútról vagy főútról van-e szó, ezért mindig az „úton”
alakot használja. Autópályán ez furcsán hathat.

**A kérdés:** elfogadható az „az M7-es úton” autópályára is, vagy
mindenképp „az M7-es autópályán” kellene?

### Q2 — A magázás a gombokon és a bevezetőben *(Register switch)*

**Mikor hallja:** az első indításkor, a bemutatóban és a párbeszédablakokban.

**Angolul:** „You're ready!”, „Welcome!”, „Are you sure?”, „Great! When
creating a marker, you can edit its name and add notes. Try that now, and
when you're ready, tap Done.”

**Most így szól:** „Készen áll!”, „Üdvözöljük!”, „Biztos benne?”,
„Nagyszerű! Jelölő létrehozásakor szerkesztheti a nevét, és megjegyzéseket
is hozzáadhat. Próbálja ki most, és amikor kész van, koppintson a Kész
gombra.”

**Amiben bizonytalanok vagyunk:** ezeket a szövegeket tegezésből írtuk át
magázásra, anyanyelvi ellenőrzés nélkül. A „Készen áll!” önmagában úgy is
érthető, hogy „valami elkészült”.

**A kérdés:** természetesen hangzanak így? Ha nem, hogyan mondaná?

### Q3 — Valami más? *(Anything else)*

Ha egy mondat angolból fordítottnak hangzik, túl hosszú vagy nem érthető,
szóljon nekünk.

Köszönjük szépen!
