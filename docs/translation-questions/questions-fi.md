---
title: "Suomi"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-fi/
---

# Soundscapen suomennos — muutama kysymys

> Lähetä vastauksesi sähköpostilla osoitteeseen **soundscapeAndroid@scottishtecharmy.support** ja mainitse kieli otsikossa.


*Finnish translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Hei, ja kiitos, että lupasit katsoa tätä.

Suomennos on osittain peräisin Microsoftin alkuperäisestä sovelluksesta, mutta
paljon on uutta, eikä kukaan suomea äidinkielenään puhuva ole tarkistanut sitä.
**Sovellusta ei tarvitse tuntea eikä asentaa:** jokaisen kysymyksen kohdalla
kerrotaan, milloin teksti kuuluu, mitä se on englanniksi ja miltä se nyt
kuulostaa suomeksi.

## Mikä Soundscape on?

Soundscape on ilmainen puhelinsovellus sokeille ja heikkonäköisille. Sitä
käytetään kävellessä kuulokkeiden kanssa, puhelin usein taskussa. Sovellus ei
opasta kuin navigaattori ("käänny vasemmalle"), vaan kertoo ääneen, mitä
lähellä on, jotta käyttäjä voi itse suunnistaa.

Muutama käsite, joka toistuu kysymyksissä:

- **Ilmoitus** *(callout)*: lyhyt puhuttu viesti jostakin, jonka ohi kuljet,
  esimerkiksi "Kahvila", "Jalkakäytävä Mannerheimintie:n vieressä" tai "Matkalla
  pohjoiseen tiellä Mannerheimintie". Se kuuluu tilaäänenä siitä suunnasta,
  jossa kohde on.
- **Äänimajakka** *(audio beacon)*: kun valitset määränpään, kuulokkeissa
  alkaa soida tasainen, toistuva ääni määränpään suunnasta. Kun käännyt, ääni
  "liikkuu" mukana, joten määränpäähän voi kulkea kuulon varassa.
- **Merkitsin** ja **reitti** *(marker, route)*: tallennetut paikat ja niiden
  sarja, jonka läpi äänimajakka johdattaa yksi kerrallaan.

Sokeat käyttävät puhelinta **ruudunlukijalla** (Androidissa TalkBack,
iPhonessa VoiceOver): jokainen painike ja teksti luetaan synteettisellä
äänellä. Sovelluksen tekstit siis melkein aina *kuullaan*, ei lueta, ja usein
liikenteen melussa. Tärkeintä on, että ne ovat lyhyitä, selkeitä ja
luontevia kuunneltuina.

Käsitteet on kuvattu tarkemmin (englanniksi)
[tällä sivulla]({{ "/developers/translation-terminology.html" | relative_url }}).

## Näin vastaat

Vastaa vain sähköpostilla ja kerro kysymyksen numero ("Q1: minä sanoisin…").
Kaikkeen ei tarvitse vastata. Jos jokin on jo hyvin, lyhyt "OK" auttaa myös.

---

### Q1 — "Polku kohteeseen umpikuja" *(Dead-end way description)*

**Milloin sen kuulee:** kävellessä, kun ohitat sivutien. Sovellus kertoo,
minne se johtaa.

**Englanniksi:** "Path to Moor Road", "Path to dead end".

**Nyt se kuulostaa tältä:** "Polku kohteeseen Moor Road", "Polku kohteeseen
umpikuja".

**Mikä meitä epäilyttää:** "kohteeseen" on kiertotie, jolla vältetään
paikannimen taivuttaminen (sovellus ei osaa taivuttaa nimiä). Umpikujan kohdalla
se kuulostaa kuitenkin kömpelöltä.

**Kysymys:** olisiko "Polku umpikujaan" parempi, tai jokin muu?

### Q2 — "Ilmoitus" *(Callout vs phone notifications)*

**Milloin sen kuulee:** se on yllä kuvattujen lyhyiden viestien nimi.
Sitä näkyy etenkin asetuksissa, esimerkiksi "Automaattiset ilmoitukset".

**Englanniksi:** "Callout", "Automatic Callouts".

**Nyt se kuulostaa tältä:** "Ilmoitus", "Automaattiset ilmoitukset".

**Mikä meitä epäilyttää:** "ilmoitus" on myös puhelimen omien ilmoitusten
(viestit, sähköpostit…) nimi.

**Kysymys:** sekoittuuko se puhelimen ilmoituksiin? Olisiko jokin muu sana
parempi?

### Q3 — Neljä tarkkuustasoa *(Four detail levels, renamed 2026-09-30)*

**Milloin sen kuulee:** asetuksissa, joissa korvakuulolta valitaan, kuinka
paljon sovellus puhuu matkalla.

**Englanniksi:** "Detailed / Simplified / Essential / Silent".

**Nyt se kuulostaa tältä:** **Yksityiskohtainen / Yksinkertaistettu /
Olennainen / Äänetön**. Yksityiskohtainen kertoo kaiken lähellä olevan;
Yksinkertaistettu jättää pienet polut pois ja toistaa harvemmin; Olennainen
kertoo vain kadut, risteykset ja maamerkit; Äänetön ei anna lainkaan
automaattisia ilmoituksia.

**Mikä meitä epäilyttää:** nimesimme juuri kaksi keskimmäistä tasoa uudelleen.
Ennen ne olivat "Tasapainoinen" ja "Hiljainen", mutta muiden kielten
äidinkieliset puhujat kokivat, että "Hiljainen" kuulostaa
äänenvoimakkuudelta. Uudet nimet valitsimme me, eivät suomea äidinkielenään
puhuvat.

**Kysymys:** erottuvatko tasot hyvin ja ovatko ne ymmärrettäviä kuultuina?
Nimeäisitkö kaksi keskimmäistä toisin?

### Q4 — Siri-komennot *(Siri phrases)*

**Milloin sen kuulee:** ei koskaan; nämä *sanotaan* itse. iPhonella
Soundscapea voi ohjata Sirillä koskematta puhelimeen.

**Nyt se kuulostaa tältä:** "Soundscape ympäristö", "Soundscape reitti",
"Soundscape aloita reitti", "Soundscape majakka", "Soundscape pysäytä majakka",
"Soundscape luettelo", "Soundscape yksityiskohdat".

**Mikä meitä epäilyttää:** komentojen pitää olla luontevia sanoa, muuten niitä
ei muista.

**Kysymys:** kuulostavatko ne luontevilta? Sanoisitko jotain muuta?

### Q5 — Kadunnimet ilmoituksissa *(Street names now inflected)*

**Milloin sen kuulee:** melkein jokaisessa kävelyn aikaisessa ilmoituksessa.

**Englanniksi:** "Heading north along Mannerheimintie", "On Aleksanterinkatu
between …".

**Nyt se kuulostaa tältä:** aiemmin sovellus sanoi "Tiellä Mannerheimintie" ja
"Kadulla Aleksanterinkatu", koska se ei osannut taivuttaa nimiä. Nyt se taivuttaa
nimen lopun: "Matkalla pohjoiseen Mannerheimintiellä", "Aleksanterinkadulla,
välillä …", "Vanhalla Vihdintiellä", "Toisella linjalla". Jos nimi ei pääty
tuttuun sanaan (esim. ruotsinkielinen nimi tai "E18"), se sanoo edelleen "Tiellä
Almas väg".

**Kysymys:** kuulostaako tämä oikealta? Keksitkö nimiä, jotka se taivuttaa väärin?

### Q6 — Suunta kellotaulun mukaan *(Clock-face directions)*

**Milloin sen kuulee:** jos asetuksissa on valittu suunnat kellotaulun mukaan.
Sovellus kertoo, missä suunnassa jokin on, niin kuin kellon viisari osoittaisi.

**Englanniksi:** "at 3 o'clock".

**Nyt se kuulostaa tältä:** «kello 3».

**Mikä meitä epäilyttää:** «kello 3» kuulostaa ehkä kellonajalta eikä suunnalta.
Emme tiedä, sanotaanko suomeksi mieluummin «kello kolmessa» tai «kello kolmen
suunnassa». Tunnin voi nyt kirjoittaa myös sanana, joten taivutettu muoto on
mahdollinen. (Oletuksena sovellus kertoo nyt suunnat muodossa vasen/oikea;
kellotaulu on valinnainen asetus.)

**Kysymys:** miten sanoisit tämän?

### Q7 — Jotain muuta? *(Anything else)*

Jos jokin lause kuulostaa englannista käännetyltä, liian pitkältä tai
epäselvältä, kerro meille.

Kiitos paljon!
