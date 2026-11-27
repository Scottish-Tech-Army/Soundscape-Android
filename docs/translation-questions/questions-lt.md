---
title: "Lietuvių"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-lt/
content_lang: lt
---

# Soundscape vertimas į lietuvių kalbą – keli klausimai

> Atsakymus siųskite el. paštu adresu **{{ site.support_email }}**, laiško temoje nurodykite kalbą.


*Lithuanian translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Sveiki ir ačiū, kad sutikote tai peržiūrėti.

Lietuvišką vertimą parašė gimtakalbis, tačiau dar niekas jo negirdėjo
programėlėje, einant gatve su ekrano skaitytuvu. Būtent tai ir norime
patikrinti. **Programėlės nereikia žinoti ar įdiegti:** prie kiekvieno
klausimo parašyta, kada tekstas pasigirsta, kaip jis skamba angliškai ir kaip
dabar skamba lietuviškai.

## Kas yra Soundscape?

Soundscape – nemokama telefono programėlė akliesiems ir silpnaregiams. Ja
naudojamasi einant, su ausinėmis, telefonas dažnai būna kišenėje. Programėlė
neveda kaip navigacija („sukite į kairę“), o garsiai pasako, kas yra netoliese,
kad žmogus pats galėtų orientuotis.

Keli žodžiai, kurie kartojasi klausimuose:

- **Pranešimas** *(callout)*: trumpas sakytinis pranešimas apie tai, pro ką
  einate, pavyzdžiui, „Kavinė“, „Šaligatvis, šalia: Gedimino prospektas“ arba
  „Einate į šiaurę, Gedimino prospektas“. Jis skamba erdviškai, iš tos pusės,
  kurioje tas objektas yra.
- **Švyturys** *(audio beacon)*: pasirinkus tikslą, ausinėse girdėti tolygus,
  pasikartojantis garsas iš tikslo pusės. Kai pasisukate, garsas „juda“ kartu,
  todėl į tikslą galima eiti iš klausos.
- **Žymė** ir **maršrutas** *(marker, route)*: išsaugotos vietos ir jų seka,
  per kurią švyturys veda po vieną.

Aklieji telefonu naudojasi su **ekrano skaitytuvu** („Android“ – TalkBack,
iPhone – VoiceOver): kiekvieną mygtuką ir tekstą perskaito sintetinis balsas.
Todėl programėlės tekstai beveik visada yra *klausomi*, o ne skaitomi, dažnai
gatvės triukšme. Svarbiausia, kad jie būtų trumpi, aiškūs ir natūraliai
skambėtų.

Sąvokos išsamiau aprašytos (anglų kalba)
[šiame puslapyje]({{ "/developers/translation-terminology.html" | relative_url }}).

## Kaip atsakyti

Atsakykite el. laišku ir nurodykite klausimo numerį („Q1: aš sakyčiau…“).
Atsakyti į viską nebūtina. Jei kas nors jau gerai, padeda ir trumpas „OK“.

---

### Q1 – „Dukart palieskite, kad…“ *(TalkBack hint frame)*

**Kada tai pasigirsta:** kai TalkBack užveda fokusą ant mygtuko. Pirmiausia
perskaitomas mygtuko pavadinimas, o po trumpos pauzės – užuomina, ką mygtukas
daro. Programėlė pateikia tik užuominos pabaigą, o pradžią prideda pats
TalkBack.

**Angliškai:** „My Location“ … „Double tap to hear about your current location“.

**Kaip dabar skamba:** programėlė pateikia bendratį: „išgirsti apie dabartinę
savo vietą“, „nutildyti švyturį“, „užmigdyti Soundscape“.

**Dėl ko abejojame:** nežinome, kokiais žodžiais lietuviškas TalkBack pradeda
užuominą („Dukart palieskite, kad…“, „Dukart palieskite, norėdami…“ ar dar
kitaip). Nuo to priklauso, ar bendratis sakinyje skamba teisingai.

**Klausimas:** jei naudojate TalkBack lietuviškai, ką jis sako prieš užuominą
kitose programėlėse? Ar „…, kad išgirsti apie dabartinę savo vietą“ skambėtų
priimtinai, ar reikėtų kitos formos („išgirstumėte“)?

### Q2 – Pavadinimai po kablelio ar dvitaškio *(Names kept in the nominative)*

**Kada tai pasigirsta:** beveik kiekviename pranešime einant.

**Angliškai:** „Path to Švyturio gatvė“, „At Maxima“, „On Gedimino prospektas
between Vilniaus gatvė and Totorių gatvė“, „Bus Stop: Katedros aikštė“.

**Kaip dabar skamba:** „Takas, veda į: Švyturio gatvė“, „Esate čia: Maxima“,
„Esate: Gedimino prospektas, tarp: Vilniaus gatvė ir Totorių gatvė“,
„Autobusų stotelė: Katedros aikštė“.

**Dėl ko abejojame:** programėlė nemoka linksniuoti pavadinimų, kurie
žemėlapyje yra vardininku, todėl juos dedame po kablelio ar dvitaškio, o ne po
prielinksnio („į Švyturio gatvę“). Gramatiškai tai teisinga, bet einant tai
girdima dešimtis kartų, ir gali skambėti mechaniškai ir varginti.

**Klausimas:** ar taip priimtina klausytis ilgesnį laiką? Jei ne, kuris
pranešimas skamba blogiausiai, ir kaip jį sakytumėte, nekeisdami pavadinimo
formos?

### Q3 – iPhone nustatymų pavadinimai *(iOS menu names in the voices help)*

**Kada tai pasigirsta:** pagalbos puslapyje, kuriame paaiškinta, kaip iPhone
pasirinkti balsą.

**Angliškai:** „In the iOS Settings app, go to Accessibility > Read & Speak >
Voices and select a voice. Before iOS 26, *Read & Speak* is called *Spoken
Content*.“

**Kaip dabar skamba:** „iOS programoje Nustatymai eikite į Pritaikymas
neįgaliesiems > Skaitymas ir kalbėjimas > Balsai ir pasirinkite balsą. Iki
iOS 26 *Skaitymas ir kalbėjimas* vadinosi *Sakomas turinys*.“

**Dėl ko abejojame:** meniu pavadinimai turi būti lygiai tokie, kokius rodo
iPhone, o mes jų nepatikrinome lietuviškame iPhone.

**Klausimas:** jei turite iPhone lietuvių kalba, ar šie pavadinimai sutampa su
tuo, ką matote ar girdite? Jei ne, kaip jie vadinasi?

### Q4 – Kai kurių vietų pavadinimai *(Unconfirmed place-type words)*

**Kada tai pasigirsta:** kai einate ar važiuojate pro tokią vietą, ir vietų
sąrašuose.

**Angliškai ir kaip dabar skamba:**

- „City / Town / Village / Hamlet“ – „Didmiestis / Miestas / Gyvenvietė / Kaimas“
- „Convenience Shop“ – „Smulkių prekių parduotuvė“
- „Supermarket“ – „Maisto prekių parduotuvė“ („Mall“ yra „Prekybos centras“)
- „Town Hall“ – „Savivaldybė“
- „Pier“ – „Lieptas“

**Dėl ko abejojame:** šiuos žodžius rinkomės iš kelių variantų. Pavyzdžiui,
gyvenviečių eilė galėtų būti „Miestas / Miestelis / Kaimas / Viensėdis“,
„Smulkių prekių parduotuvė“ – trumpiau „Krautuvėlė“, „Savivaldybė“ –
„Rotušė“, „Lieptas“ – „Prieplauka“ ar „Molas“.

**Klausimas:** kuriuos žodžius pakeistumėte? Svarbiausia, kad jie būtų trumpi
ir aiškūs klausantis.

### Q5 – Dar kas nors? *(Anything else)*

Jei kuris nors sakinys skamba kaip vertimas iš anglų kalbos, yra per ilgas ar
neaiškus, praneškite.

Labai ačiū!
