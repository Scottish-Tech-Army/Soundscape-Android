---
title: Slik fungerer Soundscape
layout: page
parent: "Bruke Soundscape"
has_toc: false
lang: nb
permalink: /users/how-it-works.html
machine-translated: true
---
# Slik fungerer Soundscape
Hensikten med denne siden er å gi en generell forståelse av hvordan Soundscape-appen fungerer under panseret. Du trenger ikke å lese dette for å bruke appen, men det er noen grunner til at den er skrevet:

1. For å hjelpe interesserte nykommere til appen med å forstå hvor begrensningene ligger
1. For å gi brukerne en idé om hva annet som kunne være mulig med nye funksjoner
1. For å gi utviklere en oversikt over appens funksjon

Det er to teknologier som gjør appen mulig, GPS og OpenStreetMap-data. GPS gir oss en god idé om hvor telefonen er, og hvor den har vært. OpenStreetMap-data kan så brukes til å finne hva som er i nærheten, og vi kan bruke det til å beskrive det for brukeren.

## Lydsignaler
På de fleste måter er dette de enkleste tingene å implementere fra et teknologisk ståsted. Forutsatt at vi har telefonens posisjon, og en retning for telefonen, kan vi så endre lyden for lydsignalet slik at det høres ut som om det kommer fra den retningen. Vi bruker et bibliotek fra Steam Audio til å utføre lydposisjoneringen, som bruker hoderelaterte overføringsfunksjoner for å gi best mulig posisjoneringslyd. Det eneste andre vi gjør, er å endre lydsignalets lyd slik at en annen lyd spilles av avhengig av vinkelen mellom brukerens retning og lydsignalets posisjon. Vinklene varierer avhengig av valgt lydsignal (Taktil, Lysglimt, Ping osv.), og noen har et større antall lyder enn andre. Og det er lydsignaler på det enkleste nivået.

Den eneste ekstra kompleksiteten er antakelsen om telefonens posisjon og retningen som brukeren peker i. La oss se på disse i tur og orden.

### Posisjon
Posisjonen som GPS gir tilbake, kan ha en ganske stor feilmargin, og dette avhenger av hvor mye av himmelen som er synlig for telefonens GPS, og hvor mange trær og høye bygninger som reflekterer GPS-signalet på vei til telefonen.

Tilnærmingen vi har valgt for å filtrere posisjonen, er å bruke det som kalles kartmatching. Dette antar at brukeren mest sannsynlig beveger seg langs en kartlagt sti eller vei – vi bruker uttrykket «Way» (vei) for å dekke alle veier, traseer og stier. Kartmatching ser på hvor brukeren har vært, og bruker bevegelsesretningen sammen med lokale kartdata til å velge den mest sannsynlige posisjonen på en Way. Denne tilnærmingen tar ikke bare hensyn til feil fra GPS, men også feil i kartdataene. Ikke alle Way-er er kartlagt nøyaktig, og derfor har de feil de også. For å avgjøre hvilken Way som er den mest sannsynlige at brukeren befinner seg på, vurderer algoritmen:
* Hvor nær en Way er til GPS-posisjonen og tidligere GPS-posisjoner
* Reiseretningen – beveger de seg i samme retning som Way-en
* Om det er mulig å komme fra den siste kartmatchede posisjonen til den nye posisjonen via nettverket av Way-er. Dette er nødvendig for å utelukke veksling mellom Way-er som ikke faktisk er sammenkoblet, f.eks. at den ene går over den andre via en bro, eller under den via en tunnel.
Kartmatchingen kan komme frem til at det ikke er noen Way-er i nærheten, eller at den ikke er sikker på hvilken brukeren er på, og i dette tilfellet venter den ganske enkelt på neste GPS-posisjon og prøver igjen til den er sikker.

### Retning
Det er flere retninger som vi sporer i programvaren:

1. Retningen som telefonen peker i. Vi bruker denne når telefonen er ulåst og appen er i bruk, men også når telefonen er låst så lenge den holdes flatt med skjermen vendt mot himmelen. Det er nyttig å huske på dette når du legger telefonen i vesken. Hvis den legges flatt i bunnen av en veske som holdes oppreist, vil den tilfeldige retningen vesken peker i bli brukt av appen.
1. Retningen som telefonen beveger seg i.
1. Retningen fra hodetelefoner med hodesporing. Vi bruker ikke dette for øyeblikket, selv om iOS-appen støttet det. Vi har teknologien på plass for å legge det til i fremtiden.

Når telefonen er låst og i en veske, vil appen bruke reiseretningen. Men hvis brukeren ikke beveger seg, er ingen retning tilgjengelig. Når dette skjer, blir lydsignalene stillere for å indikere at det ikke er mulig å vite den gjeldende retningen til lydsignalet – brukeren kan snu seg rundt uten å endre posisjon.

For noen bruksområder av retning i appen «snappes» retningen til retningen for den kartmatchede Way-en, så hvis brukeren går omtrent i retningen til Way-en, antas den faktiske retningen til Way-en å være riktig og brukes i disse beregningene.

### Konklusjon
Selv om lydsignalene tilsynelatende er enkle, innfører bruken av kartmatching for å forsøke å fjerne posisjons- og retningsfeil en betydelig grad av kompleksitet.

## Kartdata
Kartdataene som appen bruker, stammer nesten alle fra OpenStreetMap-prosjektet. Vi driver en server som inneholder et kart over hele verden på flere zoomnivåer. Hvert zoomnivå er delt opp i fliser. Zoomnivå 0 inneholder 1 flis, nivå 1 inneholder 4 fliser, nivå 2 inneholder 16 fliser og så videre opp til nivå 14, som inneholder rundt 268 millioner fliser for å dekke planeten. Hver flis inneholder flere lag, og hvert lag har punkter, linjer og polygoner som kan tegnes for å lage et grafisk kart. Det grafiske kartet er det som vises til brukeren i appens GUI. Hvert punkt, hver linje og hvert polygon har metadata som beskriver hva det er. Dette er stort sett rett fra OpenStreetMap-dataene, så en linje kan være en `footway` som er et `sidewalk` eller en `road` som er en `minor`

Dataene gjøres om til det grafiske kartet via en «stil» som har regler for hvordan de ulike punktene, linjene og polygonene i hvert lag skal tegnes, f.eks. hvordan en sti skal tegnes, hvordan en skog skal tegnes, hvordan et busstopp skal tegnes. Reglene kan variere etter zoomnivå, som er grunnen til at når du zoomer inn, blir flere og flere punkter og linjer synlige som ikke er synlige når du er zoomet ut, f.eks. busstopp og stier.

Ved å endre stilen kan vi endre hvordan GUI-kartet ser ut, som er der det «tilgjengelige kartet» som vi prøver ut, kommer fra. Det tar sikte på å ha større kontrast og kraftigere linjer og tekst. Stilen er innebygd i appen, så vi trenger ikke å endre kartet på serveren for å endre hvordan det ser ut.

Men hvordan bruker vi kartdataene til lyd?

### Bruke kartdataene til lyd
Vi bruker for øyeblikket en relativt liten mengde av kartdataene til å generere lydbasert brukergrensesnitt. Nesten hele lyd-grensesnittet bruker bare flisene på det maksimale zoomnivået. Appen syr sammen et 2 ganger 2-rutenett av fliser rundt der brukeren er, og ser så på bare noen få av lagene:

* `transportation` – for alle typer Way-er, inkludert veier, stier, jernbaner og trikkelinjer.
* `poi` – interessepunkter, f.eks. butikker, idrettssentre, benker, postkasser, busstopp osv.
* `building` – dette er for `poi` som er kartlagt som mer enn bare et punkt, f.eks. store supermarkeder eller rådhus.

Den knytter sammen linjer og polygoner på tvers av flisgrensene og gjør alle Way-ene om til sammenkoblede Way-segmenter og veikryss. Dette er viktig fordi det lar oss søke langs en Way for å finne ut hvor vi kan komme til.

Alle de tolkede dataene legges også inn i et lettsøkbart format slik at appen enkelt kan finne hvilke trekk ved kartet som er i nærheten. På dette punktet klassifiseres dataene i kategorier. De gjeldende kategoriene er:

* Veier
* Veier og stier (alle Way-er)
* Veikryss – punktene der Way-er krysser hverandre
* Innganger – dette er punkter på en bygning som er merket som en inngang.
* Overganger – veioverganger
* Interessepunkter – alle interessepunkter
* Holdeplasser – busstopp, jernbanestasjoner, trikkestopp og så videre.
* Underkategorier av interessepunkter:
  * Informasjon
  * Objekt
  * Sted
  * Landemerke
  * Mobilitet
  * Sikkerhet
* Bosetninger og underkategorier (se neste avsnitt)
  * Byer
  * Tettsteder
  * Landsbyer
  * Grender

 Med dette på plass kan appen så enkelt finne, for en hvilken som helst posisjon

 * «Alle holdeplasser innenfor 50 m» eller
 * «Det nærmeste veikrysset foran meg» eller
 * «Den nærmeste grenda, landsbyen, tettstedet eller byen»

Med dette på plass er det å lage lydmeldingene bare et spørsmål om å spørre dataene basert på den gjeldende posisjonen og retningen. Etter hvert som brukeren beveger seg over et flisrutenett, oppdaterer den det slik at det sentreres rundt den gjeldende posisjonen.

### Mer data
Et av problemene med vårt svært lokale kartdatarutenett er at det betyr at vi bare kan «se» på det meste rundt 1 km i en hvilken som helst retning. Det er greit for når vi beskriver hva som er foran oss, men noen ganger vil vi gjerne gi mer kontekst. Det viktigste eksempelet på dette er når appen brukes og brukeren ikke går.

Når appen oppdager at brukeren beveger seg raskere enn 5 meter per sekund, bytter den hvordan den beskriver verden. I stedet for å lese opp hvert veikryss og interessepunkt leser den opp sjeldnere og bare veier i nærheten. Problemet med dette er at det å vite et veinavn ikke er særlig nyttig hvis du ikke vet hvilket tettsted det er i.

For å forsøke å løse dette tolker vi nå også kartdataene på et lavere zoomnivå og henter ut data fra `place`-laget. Dette inneholder navnene på tettsteder, byer, nabolag, landsbyer og så videre. Et problem med å kartlegge ting er at det ikke alltid er en åpenbar grense mellom disse stedene. OpenStreetMap har noen ganger bygrenser i databasen sin, men selv når det er tilfellet, går denne informasjonen ofte tapt innen den når frem til vårt flisbaserte kart. Det vi har, er posisjonen der stedsnavnene tegnes på kartet. Disse kategoriseres, og appen finner så den nærmeste grenda, landsbyen, tettstedet eller byen til brukeren og rapporterer det.

For mange byer vil det faktiske bynavnet aldri bli lest opp, fordi de fleste byer er delt inn i mindre inndelinger som nabolag, men disse gir ekstra kontekst som er svært nyttig. Bare husk at fordi appen rapporterer at du er i nærheten av en gate i et bestemt nabolag, betyr det bare at etiketten for det nabolaget er det nærmeste punktet, og det kan være feil eller til og med på den andre siden av en elv.

### Mer kontekst
Jo mer kontekst som kan legges til i beskrivelser, desto bedre, så lenge det holdes konsist og forutsigbart. Et av problemene vi så med å beskrive veikryss, var at det ofte var «navnløse» Way-er involvert. Dette er Way-er som ikke har noe navn. I kartdataene kan disse bare være en trasé, en sti eller en stikkvei, men uten mer kontekst er det ikke særlig nyttig i tekstbeskrivelsene. Heldigvis kan vi gjøre det bedre, så det appen gjør, er at når den er i ferd med å lese opp en navnløs Way, ser den om den kan finne ut mer kontekst for den.

* **Er det et fortau?**
Mange områder i OpenStreetMap har nå fortau kartlagt separat fra veier. Disse er vanligvis merket som `sidewalk`, men de sier normalt ikke hvilken vei de er fortauet til.

    Når appen kommer over et navnløst fortau, søker den etter en vei som den tror går ved siden av det og bruker den til å navngi fortauet. Dette viser seg å være svært viktig for meldingene våre. I stedet for å lese opp hvert fortausveikryss, gjøres meldingene mens vi beveger oss langs et fortau som om vi beveget oss langs den tilhørende veien. I stedet for *«Går mot vest langs sti»* har vi *«Går mot vest langs Moor Road»*. Brukeren er på det kartlagte fortauet, men beskrivelsen gir mer mening.

* **Ender den ved en navngitt Way?**
Svært ofte er det fotgjengerstier som binder to veier sammen. Ved å se på begge endene av stien kan vi enkelt legge til den konteksten, slik at det i én retning kan være *«Sti til Moor Road»* og når man nærmer seg fra den andre enden kan det være *«Sti til Roselea Drive»*. Dette gjøres bare der stien ikke deler seg; hvis den deler seg i to navnløse stier, prøver vi ikke å legge til denne konteksten.

* **Ender den nær en markør?**
Hvis en navnløs Way starter eller ender nær en markør, brukes den til å beskrive den, f.eks. *«Sti til veikryss ved stort tre»*. Brukeren kan legge til markører hvor som helst, og ved å legge til markører langs stinettverk kan de legge til kontekst for en hel rute.

* **Går den inn i eller ut av et interessepunkt?**
Hvis en navnløs Way starter utenfor et interessepunkt og ender inne i det (eller omvendt), kan vi legge til den konteksten, f.eks. *«Trasé til Lennox Park»*.

* **Ender den nær en inngang?**
Hvis en navnløs Way starter eller ender nærmere en inngang, kan vi legge til den konteksten, f.eks. *«Stikkvei til Best Buy»*.

* **Ender den nær et landemerke eller sted?**
Hvis en navnløs Way starter eller ender nærmere et landemerke, kan vi også legge til den konteksten, f.eks. *«Stikkvei til St. Giles Cathedral»*.

* **Er det en blindvei?**
Appen merker som blindvei alle navnløse Way-er som ikke fører noe sted.

* **Passerer den noen trinn?**
Hvis den navnløse Way-en går over en bro, gjennom en tunnel eller opp/ned trinn, blir dette notert og lagt til i konteksten. Dette er adskilt fra destinasjonsmerkingen, så kontekst som *«Sti over bro til Lennox Park»* er mulig.

Disse kontekstene legges til i rekkefølge, og det er derfor mulig å ha *«Sti til Park Lane via trinn»* i én retning og *«Sti til Lennox Park via trinn»* i den andre retningen. Den navngitte gaten får prioritet når man forlater parken, men parken brukes når man går inn i den.

#### Fremtidig kontekst
Det er ulike ekstra kontekster vi håper å legge til i fremtiden, inkludert:
* Kontekst for Way-er som følger lineære vannelementer, f.eks. *«Sti ved siden av River Dee»*
* Kontekst for Way-er som følger kanten av vannmasser *«Sti ved siden av Milngavie-reservoaret»*
* Kontekst for Way-er som følger jernbaner, f.eks. *«Sti ved siden av jernbane»*. Dette kan til og med inkludere navnet på jernbanelinjen
* Legge til ekstra innhold på broer og tunneler, hva de er over eller under, f.eks. *«Sti via bro over jernbane til Moor Road»*

## Lydmeldinger
Nå som vi har kartdataene i et format som vi enkelt kan bruke, er det å generere meldinger virkelig ganske enkelt.

### Meldinger når du går
Når du går, er lydmeldingene som kan forekomme (i prioritert rekkefølge):

1. Beskrive hvor langt unna den gjeldende destinasjonen er
1. Beskrive et kommende veikryss
1. Beskrive de 5 nærmeste interessepunktene

Alle meldingene er frekvensbegrenset slik at de ikke gjentas for ofte. Hvis brukeren slutter å bevege seg, vil meldingene stoppe, og selv når man beveger seg vil en melding ikke gjentas for hver nye GPS-posisjon. Frekvensen er som i iOS-appen:

* Hvert 60. sekund for den gjeldende destinasjonen
* Hvert 30. sekund for et kommende veikryss
* Hvert 60. sekund for et interessepunkt

Meldinger kan filtreres via innstillingsmenyen, og det er absolutt rom for å utvide denne oppførselen.

### Meldinger når du reiser raskere
Når du reiser raskere enn 5 meter per sekund, finner meldingen om den gjeldende destinasjonen fortsatt sted, men veikryss- og interessepunktmeldingene erstattes av en melding som beskriver omtrent hvor brukeren er. Dette gir en holdeplass i nærheten, et interessepunkt som inneholder oss, f.eks. inne i en stor park, eller en vei og bosetning i nærheten. Disse bruker dataene som er beskrevet tidligere, og det er åpenbart rom for å tillate tilpasning av dette i fremtiden.

## Markører og ruter
For det meste er markører og ruter bare en brukergrensesnittfunksjon som verken er avhengig av GPS eller egentlig av kartdata. Markører er navngitte posisjoner som brukeren vil lagre, og ruter er en ordnet liste over disse markørene. Brukergrensesnittet for å opprette begge er hentet direkte fra iOS-versjonen.

### Ruteavspilling
Ruteavspilling er der ruter blir levende. Når en rute spilles av, opprettes det et lydsignal ved den første markøren i ruten. Når brukeren kommer nær den markøren, flytter ruten automatisk lydsignalet til neste markør i ruten. Hvis det ikke er flere markører, avsluttes ruteavspillingen.

## Konklusjon
Forhåpentligvis har dette gitt litt innsikt i hvordan appen fungerer. Appen er alltid under utvikling basert på tilbakemeldinger fra brukere, så ta gjerne kontakt hvis det er noe du mener kunne vært lagt til.
