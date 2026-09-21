---
title: Väljalaskemärkmed
layout: page
nav_order: 5
has_toc: false
lang: et
permalink: /release-notes.html
machine-translated: true
---

# Väljalaskemärkmed

Soundscape 2.0 on suur väljalase ja on praegu suletud beetajärgus. Kõige olulisem muudatus on see, et
nüüd on Soundscape'il midagi kasulikku öelda ka siis, kui sõidad autoga, bussiga või rongiga, mitte
üksnes jalgsi liikudes. Lisaks on tehtud palju väiksemat tööd kohtade kirjeldamise osas, lisandunud on
kakskümmend uut keelt ja pikk paranduste loend.

Vanemate versioonide märkmed on lehel
[1.x väljalaskemärkmed]({{ "/v1.0-release-notes.html" | relative_url }}).

## Mis on uut versioonis 2.0

* **Teated auto, bussi või rongiga sõites.** Soundscape tunneb ära, et liigud kiirusega, ja kirjeldab
  sinu teekonda vahetu ümbruse asemel.
* **Teade veekogude ja raudteede ületamisel.** Jõgesid, kanaleid, lahtesid ja raudteeliine
  teavitatakse nende ületamisel, nii jalgsi kui ka sõites.
* **Paremad aadressid ja kohanimed.** Ilma oma aadressita kohad saavad nüüd tänava ja piirkonna, kus
  nad asuvad, majanumbrid seotakse tänava õige poolega ning Suurbritannia bussipeatused kasutavad oma
  ametlikke nimesid.
* **Kakskümmend uut keelt**, kokku on neid nüüd 46. Ka see dokumentatsioonisait on tõlgitud.
* **Ärkamine lahkumisel.** Unerežiim suudab nüüd Soundscape'i uuesti äratada, kui lahkud kohast, kus
  ta magama panid.
* **Lühemad, loomulikumad vahemaad**, suuremate ühikutega, kui liigud kiiresti.
* **Kiirem väljumine.** *Välju Soundscape'ist* on nüüd peamenüü ülaosas.
* **Võrguühenduseta kaartide parandused**, sealhulgas juba alla laaditud kaardi kohapealne uuendamine
  ja saadaolevate piirkondade kaart sellel saidil.
* **Palju ligipääsetavuse tööd** TalkBackiga, eriti tutvustusekraanide juures.
* **Väga palju kokkujooksmiste ja stabiilsuse parandusi.**

Versioonis 2.0 on **eemaldatud** kaks asja: hääljuhtimine ja rakendusesisene keelemenüü. Vaata allpool
[Eemaldatud funktsioonid](#things-that-have-been-removed), mida selle asemel teha.

---

## Üksikasjalikumalt

### Sõit auto, bussi või rongiga

See on olemasolevate kasutajate jaoks suurim uuendus. Varem oli Soundscape'il väga vähe öelda kohe,
kui sõidukisse istusid: ta kirjeldas edasi sinu vahetut ümbrust, mis kiirusel tähendas voogu asjadest,
millest olid ammu möödunud.

Nüüd märkab Soundscape, et liigud kiiremini kui kõndides, ja muudab seda, mida sulle räägib. Midagi
pole vaja sisse lülitada ning kõik läheb ise tagasi tavapäraseks niipea, kui aeglustad või väljud ja
kõnnid edasi.

Sõidu ajal kuuled:

* **Kus sa oled**, aeg-ajalt — tee, millel sõidad, ja sinu suund, näiteks „Sõit põhja suunas mööda
  M8-t”. Numbriga teed teatatakse nende numbriga ning Soundscape ei korda sama teed iga kord, kui
  tänava nimi muutub.
* **Linnad ja külad**, mille poole liigud, koos vahemaaga, samuti need, millest eemaldud või millest
  lihtsalt möödud.
* **Maanteesõlmed ja mahasõidud**, kui nendeni jõuad.
* **Suured maamärgid**, millest möödud, näiteks pargid, haiglad, staadionid ja kaubanduskeskused.
* **Bussi-, trammi- ja rongipeatused**, millest möödud. Soundscape mainib ainult sinupoolseid
  peatusi, sest teisel pool teed asuvad teenindavad vastassuunda.
* **Jõed, kanalid ja raudteed, mida ületad.**
* **Tunnelid**, mis selgitab peamiselt, miks Soundscape kohe vaikima jääb — sees pole GPS-signaali.

**Rongis** saab Soundscape aru, et oled raudteel, mitte maanteel, ja ütleb, millistest asulatest
möödud ning kui kaugele oled viimasest jaamast jõudnud. Selle väljaselgitamine on raskem, kui kõlab,
sest maanteed ja raudteeliinid ehitatakse tihti kilomeetrite kaupa kõrvuti, nii et hea osa selle
väljalaske tööst läks sellele, et üht teisega mitte segi ajada.

Tavalised jalakäija teated — lähedal asuvad poed, ülekäigurajad ja nii edasi — peetakse sõidu ajal
meelega kinni, ning vahemaad, mille pealt asju teatatakse, on tublisti pikendatud, et saaksid millestki
teada enne, kui oled sellest möödunud.

### Veekogude ja raudteede ületamine

Soundscape ütleb nüüd, kui ületad jõe, kanali, lahe, abaja või raudteeliini. See toimib nii jalgsi kui
sõites ning hõlmab niisama hästi alt läbi kui ka pealt üle minemist, nii et kirjeldatakse nii
jalakäijate silda kui ka tunnelit.

### Paremad aadressid ja kohanimed

Palju tööd on tehtud selleks, et Soundscape kirjeldaks kohti nii, nagu inimene teeks:

* Ilma oma aadressita kohti kirjeldatakse nüüd tänava ja piirkonna kaudu, kus nad asuvad, selle
  asemel et jääda ebamääraseks.
* Majanumbrid seotakse tänava õige poolega. Varem võidi aadress teatada vastaskülje kõnniteelt.
* Koha aadress ei korda enam koha enda nime.
* Suurbritannia bussipeatused kasutavad ametlikke ühistranspordi nimesid, tavaliselt neid, mis on
  sõiduplaanil ja peatuse sildil.
* Nimeta jalgteed, mis kulgevad mööda jõge või kanalit, kannavad nüüd selle vee nime, mida nad
  järgivad.
* Nimeta radu ja teid kirjeldatakse mõistlikumalt ning nende kohta kasutatavad sõnad on korralikult
  tõlgitud, mitte ei ilmu inglise keeles.

### Keeled

Versioonis 2.0 lisandus kakskümmend uut keelt: araabia, bengali, bulgaaria, katalaani, horvaadi,
tšehhi, hausa, ungari, indoneesia, korea, marathi, serbia, slovaki, sloveeni, suahiili, tamili,
telugu, tai, urdu ja vietnami. Kõik need keeled on alfajärgus ja ootame väga tagasisidet nende
täpsuse kohta. Kokku on Soundscape nüüd saadaval 46 keeles ning ka see dokumentatsioonisait on
tõlgitud.

Egiptuse araabia keel liideti araabia keelega ja luganda võeti tagasi, sest kummalgi polnud piisavalt
tõlgitud teksti, et kasulik olla.

Tõlked on kogukonna töö ja oleme tänulikud sinu abi või paranduste eest seal, kus miski loeb halvasti.
Iga teksti saab parandada aadressil
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Unerežiim

Unerežiim sai **ärkamise lahkumisel**. Kui paned Soundscape'i magama, võid paluda tal ärgata niipea,
kui piirkonnast lahkud. See on kasulik, kui kuhugi jõuad ja tahad vaikust kuni järgmise väljumiseni.

### Vahemaad ja kõne

Väljaöeldud vahemaad on lühemad ja loomulikumad ning Soundscape läheb nüüd kiiresti liikudes üle
suurematele ühikutele — miilidele või kilomeetritele, mitte pikale loendile jalgades või meetrites.
Iga keel otsustab ise, kuidas murdosalist vahemaad öelda; varem oli see surutud inglise keele
kujulisse mustrisse.

### Võrguühenduseta kaardid

Võrguühenduseta kaardid tulid versiooniga 1.0 ja neid on pidevalt parandatud:

* Alla laaditud kaarti saab nüüd kohapeal uuendada, kui saadaval on uuem versioon, väljavõtte
  üksikasjade ekraanilt.
* Kaardid, mida ei saa kasutada — näiteks rikutud allalaadimine — märgistatakse nüüd selgelt, selle
  asemel et vaikselt ebaõnnestuda.
* Allalaadimised on usaldusväärsemad ja ekraan näitab, mis toimub, kui saadaolevate kaartide loendit
  hangitakse, täisekraanilise laadimisnäidiku asemel.
* Lõpetatud allalaadimine kuvatakse lõpetatuna alles siis, kui see on tõesti kasutusvalmis.
* Sellel saidil on
  [saadaolevate piirkondade kaart]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Ligipääsetavus

Väga palju tööd on tehtud ekraanilugejate käitumisega, eriti tutvustusekraanidel, kus fookus hüppas
varem valesse kohta. Muude paranduste hulka kuuluvad failisuuruste ja kümnendarvude parem ettelugemine,
õiged „topeltpuudutus, et...” vihjed keeltes, mis panevad tegusõna lõppu, ning mõistlikud vihjed seal,
kus neid polnud üldse määratud.

### Menüüd ja navigeerimine

* **Välju Soundscape'ist** on nüüd peamenüü esimene kirje, mitte kusagil allpool.
* Peamenüü ei jäta enam ühele küljele ekraaniriba nähtavaks, mis andis ekraanilugeja kasutajatele
  segadust tekitava lisaala puudutamiseks.
* Süsteemi tagasiliigutus ei jäta enam taset vahele, kui sirvid kategooriaid jaotises Lähedal asuvad
  kohad.
* *Heliõpetus* on ümber nimetatud **juhendatud õpetuseks**.
* Sätted on korrastatud ja *Lähtesta vaikeväärtustele* tühjendab nüüd kõik korralikult.

### Stabiilsus

2.0 sisaldab pikka loendit parandatud kokkujooksmistest ja hangumistest, nende hulgas rakenduse
hangumine avaekraanil, hangumised sätete lähtestamisel, kokkujooksmised rikutud allalaaditud kaardi
korral, kokkujooksmised marsruudi üksikasjade avamisel avaekraanilt, kokkujooksmised keele vahetamisel
ning mitu Play poe kaudu automaatselt teatatud probleemi. Aku ja käivitusega seotud käitumine on samuti
muudetud töökindlamaks telefonides, mis sulgevad taustarakendusi agressiivselt.

### Eemaldatud funktsioonid
{: #things-that-have-been-removed }

* **Hääljuhtimine** on eemaldatud. See ei töötanud kunagi piisavalt usaldusväärselt, et seda alles
  hoida, ja kõrvaklappide meedianupud katavad suures osas sama — vaata
  [Abi meedianuppude kasutamise kohta]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Rakendusesisene keelemenüü** on kadunud. Soundscape järgib nüüd telefonis määratud keelt, mida
  enamik inimesi niikuinii ootas. Selle muutmiseks vaheta telefoni keelt või määra telefoni sätetes
  rakendusepõhine keel, kui see on võimalik.

## Kuidas meile probleemidest teatada

Kui midagi pole korras, tahaksime sellest kuulda. Kirjuta Help Deskile aadressil
<soundscapeAndroid@scottishtecharmy.support> või küsi Slackis, kui oled STA liige.

Kui teade oli vale või jäi üldse tulemata, aitab sinu teekonna salvestus meid tohutult — saame selle
uuesti läbi mängida ja näha täpselt, millele Soundscape tugines. Juhised on jaotises
[Asukohasalvestuse esitamine silumiseks]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Märkus iPhone'i kohta

Kõik ülaltoodu käib Androidi rakenduse kohta, kuid tasub teada, kuhu läks selle väljalaske ülejäänud
töö. Soundscape töötab nüüd ka iPhone'is ja mõlemad rakendused ehitatakse samast jagatud koodist —
samad ekraanid, sama sõnastus ja samad teated. Nii jõuab uuendus, nagu ülalkirjeldatud sõiduteated,
mõlemasse korraga, selle asemel et seda kaks korda kirjutada. See ühine alus selgitab, miks 2.0 nii
kaua aega võttis, ja just see peaks tooma tulevased väljalasked mõlemale platvormile kiiremini.
iPhone'i rakendus on praegu saadaval TestFlighti kaudu kutsega: küsi Slackis, kui oled STA liige, või
kirjuta Help Deskile.
