---
title: "Magyar"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-hu/
---

# A Soundscape magyar fordítása — néhány kérdés

> Küldd el a válaszaidat e-mailben a **soundscapeAndroid@scottishtecharmy.support** címre, és a tárgyban add meg a nyelvet.


*Hungarian translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Szia, és köszönjük, hogy vállaltad, hogy átnézed.

A Soundscape magyar fordítása anyanyelvi segítség nélkül készült, ezért a
véleményed nagyon értékes számunkra. **Nem kell ismerned vagy telepítened az
alkalmazást:** minden kérdésnél megtalálod, hol hangzik el a szöveg, mi az
angol eredeti, és most hogyan szól magyarul.

## Mi a Soundscape?

A Soundscape egy ingyenes telefonos alkalmazás vak és gyengénlátó emberek
számára. Séta közben használják, fülhallgatóval, a telefon gyakran a
zsebben van. Az alkalmazás nem úgy vezet, mint egy navigáció („forduljon
balra”), hanem hangosan elmondja, mi van a közelben, hogy a felhasználó
maga tájékozódhasson.

Néhány fogalom, amely a kérdésekben előkerül:

- **Bejelentés** *(callout)*: rövid hangüzenet arról, ami mellett éppen
  elhaladsz, például „Kávézó”, „Járda a Fő utca mellett” vagy „Gyaloglás
  észak felé az Andrássy úton”. Térhatású hangon szól, abból az irányból,
  ahol a dolog van.
- **Hangjelző** *(audio beacon)*: ha kiválasztasz egy célpontot, egy
  folyamatos, ismétlődő hang szól, amely a fülhallgatóban a célpont
  irányából hallatszik. Ha elfordulsz, a hang is „elmozdul”, így hallásból
  lehet a cél felé menni.
- **Jelölő** és **útvonal** *(marker, route)*: elmentett helyek, illetve
  ezek sorozata, amelyen a hangjelző végigvezet.

A vak felhasználók **képernyőolvasóval** kezelik a telefont (Androidon
TalkBack, iPhone-on VoiceOver): minden gombot és szöveget gépi hang olvas
fel. Ezért az alkalmazás szövegeit szinte mindig *hallják*, nem olvassák, és
gyakran séta közben, zajban. A legfontosabb tehát, hogy hallgatva rövid,
érthető és természetes legyen.

Ha érdekel, a fogalmak részletes leírása angolul
[itt olvasható]({{ "/developers/translation-terminology.html" | relative_url }}).

## Hogyan válaszolj

Egyszerűen válaszolj e-mailben, és írd meg a kérdés számát („Q1: szerintem…”).
Nem kell mindenre válaszolnod. Ha valami már jó, egy rövid „OK” is sokat
segít.

---

### Q1 — „a” vagy „az” a nevek előtt *(automatic a/az)*

**Mikor hallod:** szinte minden bejelentésben, amely utcát vagy helyet nevez
meg, séta közben.

**Angolul:** „Heading north along Main Street”, „Sidewalk next to Main
Street”.

**Most így szól:** „Gyaloglás észak felé **az** Andrássy úton”, „Járda **a**
Rákóczi út mellett”.

**Amiben bizonytalanok vagyunk:** a fordítás nem tudhatja előre, milyen név
kerül a mondatba, ezért a szövegben „a(z)” áll. Ezt a gépi hang furcsán
olvasta fel (a zárójelekkel vagy betűzve), ezért most az alkalmazás maga
választ a behelyettesített név alapján:

- magánhangzóval kezdődő szó előtt „az”, egyébként „a”: az Andrássy, a Rákóczi;
- rövidítéseknél és útszámoknál a betű kiejtése számít: az M7 (em), az SZTE
  (esz), de a BKV (bé);
- számoknál a kimondott alak: az 1-es (egy), az 5 (öt), az 1000 (ezer), de
  a 12 (tizenkettő), a 100 (száz).

**A kérdés:** helyesek ezek a szabályok? Tudsz olyan esetet, ahol rossz
névelőt választana?

### Q2 — Tegezés vagy magázás? *(Mixed register)*

**Mikor hallod:** az egész alkalmazásban.

**Most így szól:** a gombok, a bevezető és az első indítás tegeznek:
„Készen állsz!”, „Koppints a Jelölők és útvonalak lehetőségre”. A súgóoldalak
viszont magáznak: „Ha hangjelzőt állít be egy közeli helyre, a Soundscape
folyamatosan tájékoztatja Önt…”.

**Amiben bizonytalanok vagyunk:** a kettő keveredése biztosan hiba, de nem
tudjuk, melyik illik jobban egy ilyen alkalmazáshoz. A felhasználók
felnőttek, sokan idősebbek is.

**A kérdés:** tegezzen vagy magázzon az alkalmazás mindenhol?

### Q3 — „Hangjelző” *(Audio Beacon)*

**Mikor hallod:** amikor a felhasználó kiválaszt egy célpontot, és a
célpont irányából folyamatos hang szól (lásd fent). A szó a gombokon és a
bejelentésekben is szerepel.

**Angolul:** „Audio Beacon”.

**Most így szól:** „Hangjelző”; például „Most hallhatod a hangjelzőt. A célod
irányából szól.”

**Amiben bizonytalanok vagyunk:** a „hangjelző” a hétköznapokban inkább
berregőt, csipogót vagy az autó dudáját jelenti. Nem tudjuk, hogy egy vak
felhasználó ebből megérti-e, hogy egy irányt mutató hangról van szó.

**A kérdés:** természetes így? Ha nem, mit mondanál helyette?

### Q4 — „Bejelentés” *(Callout)*

**Mikor hallod:** a rövid hangüzenetek neve (lásd fent). Leginkább a
beállításokban szerepel, például „Automatikus bejelentések” vagy „Az összes
bejelentés be- vagy kikapcsolása”.

**Angolul:** „Callout”, „Automatic Callouts”.

**Most így szól:** „Bejelentés”, „Automatikus bejelentések”.

**Amiben bizonytalanok vagyunk:** a „bejelentés” hivatalosnak hathat (mint
egy hivatalban tett bejelentés). A „bemondás” (mint a villamoson vagy a
pályaudvaron) talán közelebb áll, az „értesítés” viszont összekeverhető a
telefon saját értesítéseivel.

**A kérdés:** melyik hangzik természetesebben: „bejelentés”, „bemondás”,
vagy valami más?

### Q5 — Valami más? *(Anything else)*

Ha egy mondat angolból fordítottnak hangzik, túl hosszú vagy nem érthető,
szólj nekünk.

Köszönjük szépen!
