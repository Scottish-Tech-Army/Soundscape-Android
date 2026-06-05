---
title: Sådan fungerer Soundscape
layout: page
parent: "Brug af Soundscape"
has_toc: false
lang: da
permalink: /users/how-it-works.html
machine-translated: true
---
# Sådan fungerer Soundscape
Formålet med denne side er at give en generel forståelse af, hvordan Soundscape-appen fungerer under motorhjelmen. Du behøver ikke at læse dette for at bruge appen, men der er et par grunde til, at den er skrevet:

1. For at hjælpe interesserede nyankomne til appen med at forstå, hvor dens begrænsninger ligger
1. For at give brugerne en idé om, hvad der ellers kunne være muligt med nye funktioner
1. For at give udviklere et overblik over appens funktion

Der er to teknologier, der gør appen mulig, GPS og OpenStreetMap-data. GPS'en giver os en god idé om, hvor telefonen er, og hvor den har været. OpenStreetMap-data kan derefter bruges til at finde, hvad der er i nærheden, og vi kan bruge det til at beskrive det for brugeren.

## Lydfyr
På de fleste måder er disse de simpleste ting at implementere fra et teknologisk synspunkt. Hvis vi antager, at vi har telefonens placering og en retning for telefonen, kan vi derefter ændre lyden for lydfyret, så det lyder, som om det kommer fra den retning. Vi bruger et bibliotek fra Steam Audio til at udføre lydpositioneringen, som bruger hovedrelaterede overføringsfunktioner (head related transfer functions) for at give den bedst lydende positionering, der er mulig. Det eneste andet, vi gør, er at ændre lydfyrets lyd, så der afspilles en anden lyd afhængigt af vinklen mellem brugerens retning og lydfyrets placering. Vinklerne varierer afhængigt af det valgte lydfyr (Taktil, Lysglimt, Ping osv.), og nogle har et større antal lyde end andre. Og det er lydfyr på det simpleste niveau.

Den eneste yderligere kompleksitet er antagelsen om telefonens placering og den retning, brugeren peger i. Lad os se på disse hver for sig.

### Placering
Den placering, der returneres af GPS, kan have en ret stor fejl, og dette afhænger af, hvor meget af himlen der er synlig for telefonens GPS, og hvor mange træer og høje bygninger der reflekterer GPS-signalet på vej til telefonen.

Den tilgang, vi har taget til filtrering af placeringen, er at bruge det, der er kendt som kortmatchning (map matching). Dette antager, at brugeren højst sandsynligt rejser langs en kortlagt sti eller vej – vi bruger udtrykket 'Way' (vej) til at dække alle veje, spor og stier. Kortmatchning ser på, hvor brugeren har været, og ved hjælp af bevægelsesretningen sammen med lokale kortdata vælger den den mest sandsynlige placering på en Way. Denne tilgang tager ikke kun højde for fejl fra GPS'en, men også fejl i kortdataene. Ikke alle Ways er kortlagt nøjagtigt, og derfor har de også fejl. For at afgøre, hvilken Way der er den mest sandsynlige, som brugeren befinder sig på, overvejer algoritmen:
* Hvor nær en Way er på GPS-placeringen og tidligere GPS-placeringer
* Bevægelsesretningen – bevæger de sig i samme retning som Way'en
* Hvorvidt det er muligt at komme fra den sidst kortmatchede placering til den nye placering via netværket af Ways. Dette er nødvendigt for at udelukke skift mellem Ways, der ikke faktisk er forbundet, f.eks. den ene passerer over den anden via en bro, eller under den via en tunnel.
Kortmatchningen kan beslutte, at der ikke er nogen nærliggende Ways, eller at den ikke er sikker på, hvilken brugeren er på, og i dette tilfælde venter den blot på den næste GPS-placering og forsøger igen, indtil den er sikker.

### Retning
Der er flere retninger, som vi sporer i softwaren:

1. Den retning, telefonen peger i. Vi bruger denne, når telefonen er låst op, og appen er i brug, men også når telefonen er låst, så længe den holdes fladt med skærmen pegende mod himlen. Det er nyttigt at have dette i tankerne, når du lægger din telefon i tasken. Hvis den lægges fladt i bunden af en taske, der holdes oprejst, vil den tilfældige retning, din taske peger i, blive brugt af appen.
1. Den retning, telefonen bevæger sig i.
1. Retningen fra hovedtelefoner med hovedsporing (head tracking). Vi bruger ikke denne i øjeblikket, selvom iOS-appen understøttede den. Vi har teknologien på plads til at tilføje den i fremtiden.

Når telefonen er låst og i en taske, vil appen bruge bevægelsesretningen. Men hvis brugeren ikke bevæger sig, er der ingen retning til rådighed. Når dette sker, bliver lydfyrene mere stille for at indikere, at det ikke er muligt at kende lydfyrets aktuelle retning – brugeren kunne dreje rundt uden at ændre placering.

For nogle anvendelser af retning i appen 'fastlåses' retningen til retningen af den kortmatchede Way, så hvis brugeren går nogenlunde i retningen af Way'en, antages den faktiske retning af Way'en at være korrekt og bruges i disse beregninger.

### Konklusion
Selvom lydfyrene umiddelbart er ligetil, introducerer brugen af kortmatchning til at forsøge at fjerne placerings- og retningsfejl en hel del kompleksitet.

## Kortdata
De kortdata, appen bruger, stammer næsten alle fra OpenStreetMap-projektet. Vi driver en server, der indeholder et kort over hele verden på flere zoomniveauer. Hvert zoomniveau er opdelt i fliser (tiles). Zoomniveau 0 indeholder 1 flise, niveau 1 indeholder 4 fliser, niveau 2 indeholder 16 fliser og så videre op til niveau 14, der indeholder omkring 268 millioner fliser til at dække planeten. Hver flise indeholder flere lag, og hvert lag har punkter, linjer og polygoner, der kan tegnes for at lave et grafisk kort. Det grafiske kort er det, der vises for brugeren i appens GUI. Hvert punkt, hver linje og hver polygon har metadata, der beskriver, hvad det er. Dette er for det meste direkte fra OpenStreetMap-dataene, så en linje kan være en `footway`, der er en `sidewalk`, eller en `road`, der er en `minor`

Dataene omdannes til det grafiske kort via en 'style' (stil), der har regler for, hvordan de forskellige punkter, linjer og polygoner i hvert lag skal tegnes, f.eks. hvordan man tegner en sti, hvordan man tegner en skov, hvordan man tegner et busstoppested. Reglerne kan variere efter zoomniveau, hvilket er grunden til, at efterhånden som du zoomer ind, bliver flere og flere punkter og linjer synlige, som ikke er synlige, når der er zoomet ud, f.eks. busstoppesteder og stier.

Ved at ændre stilen kan vi ændre, hvordan GUI-kortet ser ud, hvilket er der, det 'tilgængelige kort', som vi prøver af, kommer fra. Det sigter mod at have større kontrast og fede linjer og tekst. Stilen er indbygget i appen, så vi behøver ikke at ændre kortet på serveren for at ændre, hvordan det ser ud.

Men hvordan bruger vi kortdataene til lyd?

### Brug af kortdataene til lyd
Vi bruger i øjeblikket en relativt lille mængde af kortdataene til at generere lydbrugerfladen. Næsten hele lyd-UI'en bruger kun fliserne på det maksimale zoomniveau. Appen syr et 2 gange 2-gitter af fliser sammen omkring der, hvor brugeren er, og derefter ser den kun på nogle få af lagene:

* `transportation` - for alle typer Ways, herunder veje, stier, jernbaner og sporvejslinjer.
* `poi` - interessepunkter, f.eks. butikker, sportscentre, bænke, postkasser, busstoppesteder osv.
* `building` - dette er for `poi`, der er kortlagt som mere end blot et punkt, f.eks. store supermarkeder eller rådhuse.

Den samler linjer og polygoner på tværs af flisegrænserne og omdanner alle Ways til forbundne Way-segmenter og vejkryds. Dette er vigtigt, fordi det giver os mulighed for at søge langs en Way for at finde ud af, hvor vi kan komme hen.

Alle de fortolkede data lægges også ind i et let søgbart format, så appen nemt kan finde, hvilke funktioner på kortet der er i nærheden. På dette tidspunkt klassificeres dataene i kategorier. De nuværende kategorier er:

* Veje
* Veje og stier (alle Ways)
* Vejkryds - de punkter, hvor Ways krydser hinanden
* Indgange - dette er punkter på en bygning, der er blevet markeret som en indgang.
* Overgange - vejovergange
* POI'er - alle interessepunkter
* Transitstoppesteder - busstoppesteder, jernbanestationer, sporvognsstoppesteder og så videre.
* Underkategorier af POI'er:
  * Information
  * Objekt
  * Sted
  * Landemærke
  * Mobilitet
  * Sikkerhed
* Bebyggelser og underkategorier (se næste afsnit)
  * Storbyer
  * Byer
  * Landsbyer
  * Småbyer

 Med dette på plads kan appen for enhver placering nemt finde

 * "Alle transitstoppesteder inden for 50 m" eller
 * "Det nærmeste vejkryds foran mig" eller
 * "Den nærmeste småby, landsby, by eller storby"
  
Med dette på plads er oprettelse af lydbeskederne blot et spørgsmål om at forespørge dataene baseret på den aktuelle placering og retning. Efterhånden som brugeren bevæger sig hen over et flisegitter, opdaterer den det, så det centreres omkring den aktuelle placering.

### Flere data
Et af problemerne med vores meget lokale kortdatagitter er, at det betyder, at vi højst kan 'se' omkring 1 km i enhver retning. Det er okay, når vi beskriver, hvad der er foran os, men nogle gange vil vi gerne give mere kontekst. Det vigtigste eksempel på dette er, når appen bruges, og brugeren ikke går.

Når appen registrerer, at brugeren rejser med mere end 5 meter i sekundet, skifter den, hvordan den beskriver verden. I stedet for at udsende lydbesked om hvert vejkryds og hver POI udsender den lydbeskeder sjældnere og kun om nærliggende veje. Problemet med dette er, at det ikke er særlig nyttigt at kende et vejnavn, hvis du ikke ved, hvilken by det er i.

For at forsøge at løse dette fortolker vi nu også kortdataene på et lavere zoomniveau og udtrækker data fra `place`-laget. Dette indeholder navnene på byer, storbyer, kvarterer, landsbyer og så videre. Et problem med at kortlægge ting er, at der ikke altid er en åbenlys grænse mellem disse steder. OpenStreetMap har nogle gange bygrænser i sin database, men selv når det er tilfældet, går den information ofte tabt, når den når frem til vores flisebelagte kort. Det, vi har, er den placering, hvor stednavnene tegnes på kortet. Disse kategoriseres, og derefter finder appen den nærmeste småby, landsby, by eller storby til brugeren og rapporterer det.

For mange storbyer vil det faktiske bynavn aldrig blive udsendt, fordi de fleste storbyer er opdelt i mindre inddelinger som kvarterer, men disse giver ekstra kontekst og er meget nyttige. Husk blot, at fordi appen rapporterer, at du er i nærheden af en gade i et bestemt kvarter, betyder det blot, at etiketten for det kvarter er det nærmeste punkt, og det kan være forkert eller endda på den anden side af en flod.

### Mere kontekst
Jo mere kontekst der kan tilføjes i beskrivelser, jo bedre, så længe det holdes kortfattet og forudsigeligt. Et af de problemer, vi så med at beskrive vejkryds, var, at der ofte var 'unavngivne' Ways involveret. Dette er Ways, der ikke har noget navn. I kortdataene kan disse blot være et spor, en sti eller en stikvej, men uden mere kontekst er det ikke særlig nyttigt i tekstbeskrivelserne. Heldigvis kan vi gøre det bedre, så det, appen gør, er, at hver gang den er ved at annoncere en unavngiven Way, ser den, om den kan finde ud af noget mere kontekst for den.

* **Er det et fortov?**
Mange områder af OpenStreetMap har nu fortove kortlagt separat fra veje. Disse er normalt tagget som `sidewalk`, men de siger normalt ikke, hvilken vej det er, de er fortovet til.

    Når appen støder på et unavngivent fortov, søger den efter en vej, som den mener løber ved siden af det, og bruger den til at navngive fortovet. Dette viser sig at være meget vigtigt for vores lydbeskeder. I stedet for at annoncere hvert fortovsvejkryds, udsendes lydbeskederne, efterhånden som vi bevæger os langs et fortov, som om vi bevægede os langs den tilknyttede vej. I stedet for *"Kører vest ad sti"* har vi *"Kører vest ad Moor Road"*. Brugeren er på det kortlagte fortov, men beskrivelsen giver mere mening.

* **Ender den ved en navngiven Way?**
Meget ofte er der gangstier, der forbinder to veje. Ved at se på begge ender af stien kan vi nemt tilføje den kontekst, så det i den ene retning kan være *"Sti til Moor Road"* og ved nærmelse fra den anden ende kan være *"Sti til Roselea Drive"*. Dette gøres kun, hvor stien ikke deler sig; hvis den deler sig i to unavngivne stier, forsøger vi ikke at tilføje denne kontekst.

* **Ender den nær et mærke?**
Hvis en unavngiven Way starter eller ender nær et mærke, bruges det til at beskrive den, f.eks. *"Sti til stort træ-kryds"*. Brugeren kan tilføje mærker, hvor de vil, og ved at tilføje mærker langs stinetværk kan de tilføje kontekst til en hel rute.

* **Går den ind i eller ud af en POI?**
Hvis en unavngiven Way starter uden for en POI og ender inde i den (eller omvendt), kan vi tilføje den kontekst, f.eks. *"Spor til Lennox Park"*.

* **Ender den nær en indgang?**
Hvis en unavngiven Way starter eller ender nærmere en indgang, kan vi tilføje den kontekst, f.eks. *"Stikvej til Best Buy"*.

* **Ender den nær et landemærke eller sted?**
Hvis en unavngiven Way starter eller ender nærmere et landemærke, kan vi også tilføje den kontekst, f.eks. *"Stikvej til St. Giles Cathedral"*.

* **Er det en blind vej?**
Appen markerer som en blind vej alle unavngivne Ways, der ikke fører nogen steder hen.

* **Passerer den nogen trin?**
Hvis den unavngivne Way passerer over en bro, gennem en tunnel eller op/ned ad trin, bemærkes dette og tilføjes til konteksten. Dette er adskilt fra destinationstagningen, så kontekst såsom *"Sti over bro til Lennox Park"* er mulig.

Disse kontekster tilføjes i rækkefølge, og så er det muligt at have *"Sti til Park Lane via trin"* i den ene retning og *"Sti til Lennox Park via trin"* i den anden retning. Den navngivne gade får prioritet ved at forlade parken, men parken bruges ved indgang til den.

#### Fremtidig kontekst
Der er forskellige yderligere kontekster, vi håber at tilføje i fremtiden, herunder:
* Kontekst for Ways, der følger lineære vandfunktioner, f.eks. *"Sti ved siden af River Dee"*
* Kontekst for Ways, der følger kanten af vandområder *"Sti ved siden af Milngavie-reservoiret"*
* Kontekst for Ways, der følger jernbaner, f.eks. *"Sti ved siden af jernbane"*. Dette kunne endda inkludere navnet på jernbanelinjen
* Tilføj ekstra indhold til broer og tunneller, hvad er de over eller under, f.eks. *"Sti via bro over jernbane til Moor Road"*

## Lydbeskeder
Nu hvor vi har kortdataene i et format, som vi nemt kan bruge, er det at generere lydbeskeder faktisk ret ligetil.

### Lydbeskeder under gang
Når man går, er de lydbeskeder, der kan forekomme (i prioritetsrækkefølge):

1. Beskrivelse af, hvor langt væk den aktuelle destination er
1. Beskrivelse af et kommende vejkryds
1. Beskrivelse af de 5 nærmeste interessepunkter

Alle lydbeskeder er hastighedsbegrænset, så de ikke gentages for ofte. Hvis brugeren holder op med at bevæge sig, vil lydbeskederne stoppe, og selv når man bevæger sig, gentages en lydbesked ikke ved hver ny GPS-placering. Frekvensen er som i iOS-appen:

* Hvert 60. sekund for den aktuelle destination
* Hvert 30. sekund for et kommende vejkryds
* Hvert 60. sekund for et interessepunkt

Lydbeskeder kan filtreres via indstillingsmenuen, og der er bestemt mulighed for at udvide denne adfærd.

### Lydbeskeder ved hurtigere rejse
Når man rejser med mere end 5 meter i sekundet, finder lydbeskeden til den aktuelle destination stadig sted, men lydbeskederne om vejkryds og interessepunkter erstattes af en lydbesked, der beskriver, nogenlunde hvor brugeren er. Dette giver et nærliggende transitstoppested, et interessepunkt, der indeholder os, f.eks. inde i en stor park, eller en nærliggende vej og bebyggelse. Disse bruger de data, der er beskrevet tidligere, og der er åbenlys plads til at tillade tilpasning af dette i fremtiden.

## Mærker og ruter
For det meste er mærker og ruter blot en brugerfladefunktion, der hverken er afhængig af GPS eller egentlig af kortdata. Mærker er navngivne placeringer, som brugeren ønsker at gemme, og ruter er en ordnet liste over disse mærker. Brugerfladen til at oprette begge er taget direkte fra iOS-versionen.

### Afspilning af rute
Afspilning af rute er der, hvor ruter kommer til live. Når en rute afspilles, oprettes der et lydfyr ved det første mærke i ruten. Når brugeren kommer tæt på det mærke, flytter ruten automatisk lydfyret til det næste mærke i ruten. Hvis der ikke er flere mærker, afsluttes afspilningen af ruten.

## Konklusion
Forhåbentlig har det givet en vis indsigt i, hvordan appen fungerer. Appen udvikler sig altid baseret på feedback fra brugere, så kontakt os endelig, hvis der er noget, du synes kunne tilføjes.
