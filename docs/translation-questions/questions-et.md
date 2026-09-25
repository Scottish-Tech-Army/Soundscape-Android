---
title: "Eesti"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-et/
---

# Soundscape'i eestikeelne tõlge — mõned küsimused

> Saatke oma vastused e-postiga aadressile **soundscapeAndroid@scottishtecharmy.support** ja märkige teemareale keel.


*Estonian translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Tere ja aitäh, et olete nõus seda vaatama.

Eestikeelse tõlke algus pärineb ühelt vabatahtlikult, kuid suur osa on uus ja
keegi eesti emakeelega inimene pole seda üle vaadanud. **Rakendust ei pea
tundma ega installima:** iga küsimuse juures on kirjas, millal tekst kõlab, mis
see on inglise keeles ja kuidas see praegu eesti keeles kõlab.

## Mis on Soundscape?

Soundscape on tasuta telefonirakendus pimedatele ja vaegnägijatele. Seda
kasutatakse kõndimise ajal, kõrvaklappidega, telefon sageli taskus. Rakendus ei
juhata nagu navigaator („pöörake vasakule“), vaid ütleb valjult, mis on
lähedal, et inimene saaks ise orienteeruda.

Mõned mõisted, mis küsimustes korduvad:

- **Häälteade** *(callout)*: lühike suuline teade millegi kohta, millest
  möödute, näiteks „Kohvik“, „Kõnnitee Viru tänav kõrval“ või „Kõnnib põhja
  suunas mööda Viru tänav“. See kõlab ruumiliselt sellest suunast, kus asi on.
- **Helimajakas** *(audio beacon)*: kui valite sihtkoha, kõlab kõrvaklappides
  ühtlane korduv heli sihtkoha suunast. Kui pöörate, heli „liigub“ kaasa, nii et
  sihtkohta saab minna kuulmise järgi.
- **Marker** ja **marsruut** *(marker, route)*: salvestatud kohad ja nende jada,
  mille helimajakas ükshaaval läbi juhatab.

Pimedad kasutavad telefoni **ekraanilugejaga** (Androidis TalkBack, iPhone'is
VoiceOver): iga nuppu ja teksti loeb ette sünteetiline hääl. Rakenduse tekste
seega peaaegu alati *kuulatakse*, mitte ei loeta, ja sageli tänavamüras. Kõige
tähtsam on, et need oleksid lühikesed, selged ja kuulates loomulikud.

Mõisted on täpsemalt kirjeldatud (inglise keeles)
[sellel lehel]({{ "/developers/translation-terminology.html" | relative_url }}).

## Kuidas vastata

Vastake e-kirjaga ja märkige küsimuse number („Q1: ma ütleksin…“). Kõigele ei
pea vastama. Kui miski on juba hea, aitab ka lühike „OK“.

---

### Q1 — „Sina“ või „teie“? *(Mixed register)*

**Millal see kõlab:** kogu rakenduses.

**Kuidas see praegu kõlab:** rakendus kasutab mõlemat: kohati „sina“ („Meil ei
õnnestu sinu asukohta tuvastada“, „Nüüd kuuled helimajakat“), kohati „teie“
(„Olete valmis!“).

**Mis meid kahtlema paneb:** segamini kasutamine on kindlasti viga, kuid me ei
tea, kumb sobib sellisele rakendusele paremini.

**Küsimus:** kumba peaks kasutama kõikjal?

### Q2 — „Rada kuni ummiktee“ *(Dead-end way description)*

**Millal see kõlab:** kõndimise ajal, kui möödute kõrvalteest. Rakendus ütleb,
kuhu see viib.

**Inglise keeles:** „Path to Moor Road“, „Path to dead end“.

**Kuidas see praegu kõlab:** „Rada kuni Moor Road“, „Rada kuni ummiktee“.

**Mis meid kahtlema paneb:** „kuni“ väldib nime käänamist (rakendus ei oska
nimesid käänata), kuid „kuni ummiktee“ kõlab kohmakalt.

**Küsimus:** kuidas oleks loomulikum?

### Q3 — „Liigub põhja suunas“ *(Traveling, used in a vehicle)*

**Millal see kõlab:** kui kasutaja sõidab (näiteks bussiga) ja rakendus ütleb
sõidusuuna.

**Inglise keeles:** „Traveling north“, „Traveling north along Viru tänav“.

**Kuidas see praegu kõlab:** „Liigub põhja suunas“, „Liigub põhja suunas mööda
Viru tänav“.

**Mis meid kahtlema paneb:** lausel pole alust („liigub“ – kes?).

**Küsimus:** kas „Sõidate põhja suunas“ oleks parem?

### Q4 — „Häälteade“ *(Callout)*

**Millal see kõlab:** see on ülal kirjeldatud lühikeste teadete nimi. Seda näeb
eriti seadetes, näiteks „Automaatsed häälteated“.

**Inglise keeles:** „Callout“, „Automatic Callouts“.

**Kuidas see praegu kõlab:** „Häälteade“, „Automaatsed häälteated“.

**Mis meid kahtlema paneb:** termin valiti masintõlkega.

**Küsimus:** kas see on loomulik? Kui ei, mida ütleksite?

### Q5 — „Uinak“ *(Snooze)*

**Millal see kõlab:** avaekraani nupul ja siis, kui rakendus ütleb, mis olekus
ta on. Rakenduse saab peatada kahel viisil: täielikult, kuni te selle ise
käivitate („Unerežiimis“), või ajutiselt, nii et see **ärkab ise**, kui te
praegusest kohast lahkute (näiteks kodust).

**Inglise keeles:** „Sleeping“ ja „Snoozing“.

**Kuidas see praegu kõlab:** „Unerežiimis“ ja „Uinakul“.

**Mis meid kahtlema paneb:** kas „Uinakul“ annab mõista, et rakendus ärkab ise.

**Küsimus:** kas see on arusaadav? Kas ütleksite teisiti?

### Q6 — Midagi muud? *(Anything else)*

Kui mõni lause kõlab nagu tõlge inglise keelest, on liiga pikk või ebaselge,
andke teada.

Suur aitäh!
