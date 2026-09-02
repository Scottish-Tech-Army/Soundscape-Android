---
title: Útgáfunótur
layout: page
nav_order: 5
has_toc: false
lang: is
permalink: /release-notes.html
machine-translated: true
---

# Útgáfunótur

Soundscape 2.0 er stór útgáfa og er sem stendur í lokaðri beta-prófun. Stærsta breytingin er sú að nú
hefur Soundscape eitthvað gagnlegt að segja þegar þú ferðast með bíl, strætó eða lest, ekki bara þegar
þú gengur. Að auki hefur verið unnið mikið af smærri verkum um það hvernig staðir eru lýstir, tuttugu
ný tungumál hafa bæst við og langur listi af lagfæringum.

Nótur fyrir eldri útgáfur eru á síðunni
[Útgáfunótur fyrir 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nýtt í 2.0

* **Tilkynningar á ferð með bíl, strætó eða lest.** Soundscape þekkir að þú ferð á hraða og lýsir
  ferðalaginu í stað næsta umhverfis.
* **Látið vita þegar þú ferð yfir vatn og járnbrautir.** Ár, skurðir, firðir og járnbrautarlínur eru
  tilkynntar þegar farið er yfir þær, hvort sem þú gengur eða ert á ferð.
* **Betri heimilisföng og staðarheiti.** Staðir sem hafa ekkert eigið heimilisfang fá nú götuna og
  hverfið sem þeir eru í, húsnúmer eru tengd réttri hlið götunnar og strætóstoppistöðvar í Bretlandi
  nota opinber heiti sín.
* **Tuttugu ný tungumál**, samtals eru þau nú 46. Þessi skjölunarvefur hefur einnig verið þýddur.
* **Vakning við brottför.** Svefnstilling getur nú vakið Soundscape aftur þegar þú yfirgefur staðinn
  þar sem þú svæfðir hana.
* **Styttri og eðlilegri vegalengdir**, með stærri einingum þegar þú ferð hratt yfir.
* **Fljótlegri leið út.** *Loka Soundscape* er nú efst í aðalvalmyndinni.
* **Endurbætur á ónettengdum kortum**, meðal annars uppfærsla á þegar sóttu korti á staðnum og kort
  yfir tiltæk svæði á þessum vef.
* **Mikil vinna við aðgengi** með TalkBack, einkum í kringum kynningarskjáina.
* **Afar margar lagfæringar á hruni og stöðugleika.**

Tvennt hefur verið **fjarlægt** í 2.0: raddstýringin og tungumálavalmyndin inni í forritinu. Sjá
[Fjarlægðir eiginleikar](#things-that-have-been-removed) hér að neðan um hvað má gera í staðinn.

---

## Nánar

### Að ferðast með bíl, strætó eða lest

Þetta er stærsta nýjungin fyrir núverandi notendur. Áður hafði Soundscape sárafátt að segja um leið og
þú settist inn í farartæki: hún hélt áfram að lýsa næsta umhverfi, sem á hraða þýddi straumur af hlutum
sem þú varst löngu farin/n fram hjá.

Nú tekur Soundscape eftir því að þú ferð hraðar en gönguhraða og breytir því sem hún segir þér. Það
þarf ekkert að kveikja á neinu og allt fer sjálfkrafa aftur í eðlilegt horf um leið og þú hægir á þér
eða ferð út og gengur.

Á ferðinni heyrir þú:

* **Hvar þú ert**, öðru hverju — veginn sem þú ert á og stefnuna, til dæmis „Á leið norður eftir M8“.
  Vegir með númeri eru tilkynntir með númerinu og Soundscape endurtekur ekki sama veginn í hvert sinn
  sem götuheitið breytist.
* **Bæi og þorp** sem þú stefnir að, ásamt fjarlægð, sem og þau sem þú fjarlægist eða ferð einfaldlega
  fram hjá.
* **Vegamót og afreinar** þegar þú kemur að þeim.
* **Stór kennileiti** sem þú ferð fram hjá, svo sem garða, sjúkrahús, leikvanga og verslunarmiðstöðvar.
* **Strætó-, sporvagna- og lestarstöðvar** sem þú ferð fram hjá. Soundscape nefnir aðeins stöðvarnar
  þín megin við veginn, því þær hinum megin þjóna gagnstæðri átt.
* **Ár, skurði og járnbrautir sem þú ferð yfir.**
* **Göng**, sem skýrir einkum hvers vegna Soundscape er við það að þagna — þar inni er ekkert
  GPS-merki.

Í **lest** áttar Soundscape sig á því að þú ert á járnbraut en ekki vegi og segir þér hvaða byggðir þú
ferð fram hjá og hversu langt þú ert komin/n frá síðustu stöð. Það er erfiðara en það hljómar, því
hraðbrautir og járnbrautarlínur eru oft lagðar hlið við hlið kílómetrum saman, svo góður hluti vinnunnar
í þessari útgáfu fór í að rugla ekki öðru saman við hitt.

Venjulegar tilkynningar fyrir gangandi — verslanir í nágrenninu, gangbrautir og svo framvegis — eru
vísvitandi haldið eftir á ferðinni, og vegalengdirnar sem hlutir eru tilkynntir á hafa verið lengdar
töluvert svo þú fáir að vita af einhverju áður en þú ert komin/n fram hjá því.

### Að fara yfir vatn og járnbrautir

Soundscape segir þér nú þegar þú ferð yfir á, skurð, fjörð, vík eða járnbrautarlínu. Þetta virkar bæði
gangandi og á ferð og nær jafnt yfir að fara undir sem yfir, svo bæði göngubrú og undirgöngum er lýst.

### Betri heimilisföng og staðarheiti

Mikil vinna hefur farið í að Soundscape lýsi stöðum eins og manneskja myndi gera:

* Stöðum sem hafa ekkert eigið heimilisfang er nú lýst með götunni og hverfinu sem þeir eru í, í stað
  þess að vera óljósir.
* Húsnúmer eru tengd réttri hlið götunnar. Áður gat heimilisfang verið tilkynnt frá gangstéttinni
  hinum megin.
* Heimilisfang staðar endurtekur ekki lengur heiti staðarins sjálfs.
* Strætóstoppistöðvar í Bretlandi nota opinber heiti almenningssamgangna, yfirleitt þau sem eru á
  tímatöflunni og á skiltinu við stöðina.
* Ónefndir göngustígar sem liggja meðfram á eða skurði heita nú eftir vatninu sem þeir fylgja.
* Stígum og vegum án heitis er lýst af meira viti og orðin sem notuð eru um þá eru rétt þýdd í stað
  þess að birtast á ensku.

### Tungumál

Tuttugu ný tungumál bættust við í 2.0: arabíska, bengalska, búlgarska, katalónska, króatíska,
tékkneska, hása, ungverska, indónesíska, kóreska, maratí, serbneska, slóvakíska, slóvenska, svahílí,
tamílska, telúgú, taílenska, úrdú og víetnamska. Öll þessi tungumál eru á alfa-stigi og okkur þætti
mjög vænt um athugasemdir um nákvæmni þeirra. Alls er Soundscape nú fáanleg á 46 tungumálum og þessi
skjölunarvefur hefur einnig verið þýddur.

Egypsk arabíska var sameinuð arabísku og lúganda var dregin til baka, þar sem hvorugt hafði nægan
þýddan texta til að gagnast.

Þýðingar eru samfélagsverk og við fögnum aðstoð þinni, eða leiðréttingum þar sem eitthvað les illa.
Hvaða texta sem er má bæta á
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Svefnstilling

Svefnstillingin hefur fengið **vakningu við brottför**. Þegar þú svæfir Soundscape geturðu beðið hana
um að vakna um leið og þú yfirgefur svæðið, sem er gagnlegt þegar þú kemur eitthvað og vilt ró þar til
þú leggur af stað næst.

### Vegalengdir og tal

Talaðar vegalengdir hafa verið styttar og gerðar eðlilegri, og Soundscape skiptir nú yfir í stærri
einingar þegar þú ferð hratt — mílur eða kílómetra í stað langrar talningar í fetum eða metrum. Hvert
tungumál ákveður sjálft hvernig brotavegalengd er sögð, sem áður var þvingað í enskumótað snið.

### Ónettengd kort

Ónettengd kort komu með 1.0 og hafa jafnt og þétt verið bætt:

* Sótt kort má nú uppfæra á staðnum þegar nýrri útgáfa er til, af upplýsingaskjá útdráttarins.
* Kort sem ekki er hægt að nota — til dæmis skemmd niðurhal — eru nú greinilega merkt í stað þess að
  bregðast hljóðlega.
* Niðurhal er áreiðanlegra og skjárinn sýnir hvað er að gerast meðan listi yfir tiltæk kort er sóttur,
  í stað hleðslutákns á öllum skjánum.
* Lokið niðurhal birtist sem lokið aðeins þegar það er raunverulega tilbúið til notkunar.
* Á þessum vef er
  [kort yfir tiltæk svæði]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Aðgengi

Afar mikil vinna hefur farið í hegðun skjálesara, einkum á kynningarskjánum þar sem fókusinn stökk áður
á rangan stað. Aðrar úrbætur eru meðal annars betri lestur á skráarstærðum og tugabrotum, réttar
ábendingar á borð við „ýttu tvisvar til að ...“ í tungumálum sem setja sögnina aftast, og skynsamlegar
ábendingar þar sem engar höfðu verið settar.

### Valmyndir og leiðsögn

* **Loka Soundscape** er nú fyrsti liðurinn í aðalvalmyndinni í stað þess að vera neðar.
* Aðalvalmyndin skilur ekki lengur eftir ræmu af skjánum öðrum megin, sem gaf notendum skjálesara
  ruglingslegt aukasvæði til að snerta.
* Til baka-bending kerfisins sleppir ekki lengur þrepi þegar þú flettir flokkum í Staðir í nágrenninu.
* *Hljóðleiðsögnin* heitir nú **leidd kennsla**.
* Stillingar hafa verið teknar til og *Endurstilla á sjálfgefið* hreinsar nú allt rétt.

### Stöðugleiki

2.0 inniheldur langan lista af lagfærðum hrunum og frostum, þar á meðal að forritið frjósi á
upphafsskjánum, frost við að endurstilla stillingar, hrun vegna skemmds sótts korts, hrun við að opna
leiðarupplýsingar af heimaskjánum, hrun við að skipta um tungumál, og nokkur vandamál sem tilkynnt voru
sjálfkrafa í gegnum Play Store. Hegðun varðandi rafhlöðu og ræsingu hefur einnig verið gerð traustari á
símum sem loka bakgrunnsforritum af hörku.

### Fjarlægðir eiginleikar
{: #things-that-have-been-removed }

* **Raddstýringin** hefur verið fjarlægð. Hún virkaði aldrei nógu áreiðanlega til að halda henni, og
  margmiðlunartakkarnir á heyrnartólum ná að mestu yfir það sama — sjá
  [Hjálp við notkun margmiðlunartakka]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Tungumálavalmyndin inni í forritinu** er farin. Soundscape fylgir nú tungumálinu sem þú hefur stillt
  í símanum, sem er það sem flestir bjuggust við. Til að breyta því skaltu breyta tungumáli símans eða
  velja tungumál fyrir hvert forrit í stillingum símans ef hann býður upp á það.

## Hvernig þú lætur okkur vita af vandamálum

Ef eitthvað er ekki í lagi viljum við gjarnan heyra af því. Sendu tölvupóst á Help Desk á
<soundscapeAndroid@scottishtecharmy.support>, eða spurðu á Slack ef þú ert félagi í STA.

Ef tilkynning var röng eða kom alls ekki hjálpar upptaka af ferðinni þinni okkur gríðarlega — við
getum spilað hana aftur og séð nákvæmlega hvað Soundscape studdist við. Leiðbeiningar eru í
[Að leggja fram staðsetningarupptöku til villuleitar]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Athugasemd um iPhone

Allt hér að ofan snýr að Android-forritinu, en það er gagnlegt að vita hvert afgangurinn af vinnunni í
þessari útgáfu fór. Soundscape keyrir nú einnig á iPhone og bæði forritin eru byggð úr sama sameiginlega
kóðanum — sömu skjáir, sama orðalag og sömu tilkynningar. Nýjung eins og ferðatilkynningarnar hér að
ofan berst því til beggja í einu í stað þess að vera skrifuð tvisvar. Þessi sameiginlegi grunnur skýrir
hvers vegna 2.0 tók svona langan tíma og það er hann sem ætti að láta framtíðarútgáfur berast hraðar á
báða vettvanga. iPhone-forritið er sem stendur fáanlegt í gegnum TestFlight með boði: spurðu á Slack ef
þú ert félagi í STA, eða sendu tölvupóst á Help Desk.
