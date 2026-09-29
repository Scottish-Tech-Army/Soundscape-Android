---
title: Udgivelsesnoter
layout: page
nav_order: 5
has_toc: false
lang: da
permalink: /release-notes.html
machine-translated: true
---

# Udgivelsesnoter

Soundscape 2.0 er en stor udgivelse og er i øjeblikket i lukket beta. Den vigtigste ændring er, at
Soundscape nu har noget nyttigt at sige, når du rejser med bil, bus eller tog, og ikke længere kun
når du går. Dertil kommer en mængde mindre arbejde med, hvordan steder beskrives, tyve nye sprog og
en lang liste af rettelser.

Noter til ældre versioner findes på siden
[Udgivelsesnoter for 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nyt i 2.0

* **Meldinger under rejser med bil, bus eller tog.** Soundscape registrerer, at du bevæger dig med
  fart, og beskriver din rejse i stedet for dine umiddelbare omgivelser.
* **Besked når du krydser vand og jernbaner.** Floder, kanaler, fjorde og jernbanestrækninger
  meldes, når du krydser dem — både til fods og undervejs.
* **Vælg, hvor meget Soundscape siger.** Den nye indstilling *Detaljeniveau for lydbeskeder* gør
  Soundscape mere stille på travle steder, og med *Steder der skal annonceres* vælger du, hvilke slags
  steder du hører om. Du kan ændre detaljeniveauet med knapperne på dine høretelefoner, mens du går.
* **Hør, hvor langt der er til næste vejkryds.** Vejkryds annonceres i en ensartet afstand, når du
  nærmer dig, og lydbeskeden siger nu, hvor langt der er til kantstenen.
* **Søg efter en slags sted eller efter koordinater.** Søg efter "apotek" eller "busstoppested" for
  at finde de nærmeste, uanset hvad de hedder, eller indsæt koordinater, et kortlink eller en Plus
  Code.
* **Åbn et sted i en anden kortapp**, fx Google Maps, fra oplysningerne om placeringen eller fra
  listerne.
* **Mere fra lydfyret på startskærmen.** Det viser nu afstand og retning og har skærmlæserhandlinger,
  så du kan få en lydbesked om lydfyret, høre mere om det eller gemme det som mærke.
* **Bedre adresser og stednavne.** Steder uden egen adresse får nu den gade og det område, de ligger
  i, husnumre knyttes til den rigtige side af gaden, og busstoppesteder i Storbritannien bruger deres
  officielle navne.
* **Tyve nye sprog**, så der nu er 46 i alt. Også dette dokumentationswebsted er oversat.
* **Vækning ved afgang.** Dvaletilstand kan nu vække Soundscape igen, når du forlader det sted, hvor
  du satte den i dvale.
* **Kortere, mere naturlige afstande**, med større enheder når du bevæger dig hurtigt.
* **En hurtigere vej ud.** *Afslut Soundscape* ligger nu øverst i hovedmenuen.
* **Forbedringer af offlinekort**, herunder opdatering af et allerede hentet kort og et kort over de
  tilgængelige regioner på dette websted.
* **Meget tilgængelighedsarbejde** med TalkBack, især omkring introduktionsskærmene.
* **Særdeles mange rettelser af nedbrud og stabilitet.**

To ting er **fjernet** i 2.0: stemmestyringen og sprogmenuen inde i appen. Se
[Fjernede funktioner](#things-that-have-been-removed) nedenfor for, hvad du kan gøre i stedet.

---

## Mere udførligt

### At rejse med bil, bus eller tog

Dette er den største nyhed for eksisterende brugere. Tidligere havde Soundscape meget lidt at sige,
så snart du satte dig ind i et køretøj: den blev ved med at beskrive dine umiddelbare omgivelser,
hvilket ved fart betød en strøm af ting, du for længst var kørt forbi.

Soundscape bemærker nu, at du bevæger dig hurtigere end gangtempo, og ændrer det, den fortæller dig.
Der er intet at slå til, og alt vender af sig selv tilbage til det normale, så snart du sætter farten
ned eller stiger ud og går.

Undervejs hører du:

* **Hvor du er**, med jævne mellemrum — vejen, du kører på, og din retning, for eksempel »Kører mod
  nord ad M8«. Veje med et nummer meldes med deres nummer, og Soundscape gentager ikke den samme vej,
  hver gang gadenavnet skifter.
* **Byer og landsbyer**, du kører imod, med afstanden, samt dem, du kører væk fra eller blot passerer.
* **Motorvejskryds og frakørsler**, når du når dem.
* **Store landemærker**, du passerer, såsom parker, hospitaler, stadioner og indkøbscentre.
* **Bus-, sporvogns- og togstoppesteder**, du passerer. Soundscape nævner kun stoppesteder i din side
  af vejen, da dem på den modsatte side betjener den modsatte retning.
* **Floder, kanaler og jernbaner, du krydser.**
* **Tunneler**, hvilket først og fremmest forklarer, hvorfor Soundscape er ved at blive tavs — der er
  intet GPS-signal derinde.

I et **tog** regner Soundscape ud, at du er på en jernbane og ikke en vej, og fortæller dig, hvilke
byer du passerer, og hvor langt du er kommet siden sidste station. Det er sværere, end det lyder, for
motorveje og jernbaner ligger ofte side om side i kilometervis, så en god del af arbejdet i denne
udgivelse gik med ikke at forveksle det ene med det andet.

De almindelige meldinger for gående — butikker i nærheden, fodgængerovergange og så videre — holdes
bevidst tilbage, mens du rejser, og afstandene, hvorpå ting meldes, er strakt betydeligt, så du hører
om noget, før du er kørt forbi det.

### Vejkryds

Det mest almindelige spørgsmål om lydbeskeder ved vejkryds var, hvor langt væk krydset egentlig er.
Nu siger Soundscape det: "Vejkryds 30 meter væk". Afstanden måles til kantstenen på den gade, du skal
til at krydse, og ikke til midten af krydset, for det er der, du faktisk stopper.

Lydbeskeden kommer også på et mere ensartet tidspunkt. Før kunne den komme 45 meter eller 10 meter
før, uden noget der skilte de to ad. Nu venter den, til krydset er omkring 30 meter væk, så afstanden
betyder nogenlunde det samme hver gang.

### At krydse vand og jernbaner

Soundscape fortæller dig nu, når du krydser en flod, en kanal, en fjord, en bugt eller en
jernbanestrækning. Det virker både til fods og undervejs og dækker både at gå under og over, så både
en gangbro og en underføring beskrives.

### Vælg, hvor meget Soundscape siger

Det, vi oftest hører om Soundscape, er, at den siger for meget på travle steder som en bymidte.
Afsnittet *Administrer lydbeskeder* i *Indstillinger* har nu tre indstillinger i stedet for den
gamle liste med kontakter:

* **Detaljeniveau for lydbeskeder** er Lydløs, Stille, Balanceret eller Detaljeret. *Detaljeret* er
  det, Soundscape altid har gjort, og det er udgangspunktet. *Balanceret* udelader mindre stier og
  servicevej og gentager sig sjældnere. *Stille* annoncerer kun gader, vejkryds og landemærker.
  *Lydløs* giver slet ingen automatiske lydbeskeder, mens lydfyr, ruter og knapperne på startskærmen
  fortsat virker. Indstillingen erstatter den gamle kontakt *Tillad lydbeskeder*, og hvis du havde
  slået den fra, står detaljeniveauet nu på Lydløs.
* **Gader og vejkryds** slår lydbeskeder om vejkryds og om den gade, du er på, til eller fra.
* **Steder der skal annonceres** er en liste, du kan sætte flueben i: Alt, Landemærker, Offentlig
  transport, Mad og drikke, Dagligvarer og nærbutikker, Banker og hæveautomater eller Ingen steder.
  Vælg så mange, du vil, fx landemærker og busstoppesteder. Dine mærker annonceres altid.

Det rette detaljeniveau skifter, mens du går, så du behøver ikke gå ind i Indstillinger for at ændre
det. Når du trykker på *Forrige* på dine høretelefoner, sænkes detaljeniveauet ét trin ad gangen, fra
Detaljeret over Balanceret og Stille til Lydløs og så rundt til Detaljeret igen. Det nye niveau
siges hver gang. Det virker i begge tilstande for medieknapperne, og det betyder, at knapperne på
høretelefonerne er ændret en smule:

* I *Originaltilstand* annoncerer *Næste* nu *Omkring mig*, når der ikke afspilles en rute, og *Min
  placering* ligger ikke længere på knapperne. Mens en rute afspilles, flytter *Næste* og *Forrige*
  stadig mellem vejpunkterne.
* I tilstanden *Lydmenu* går *Forrige* ikke længere baglæns gennem menuen. *Næste* går stadig
  gennem den, og *Afspil/Pause* vælger stadig. Mærker og ruter står nu i navneorden i menuen, og når
  du har startet et, går menuen tilbage til toppen i stedet for at efterlade dig dybt nede i listen.

### Søgning

Søgelinjen forstår nu mere end stednavne:

* **Slags steder.** Søg efter "apotek", "toilet", "hæveautomat" og så videre på dit eget sprog, og
  Soundscape viser de nærmeste steder af den slags, uanset hvad de hedder. Steder uden navn, som de
  fleste toiletter og bænke, vises som det, de er, med deres adresse.
* **Koordinater, kortlinks og Plus Codes.** Indsæt et talpar, grader og minutter, et link fra Google
  Maps, Apple Kort eller OpenStreetMap eller en Plus Code, og Soundscape giver dig præcis det sted.
  Et talpar alene kan læses begge veje, så hvor begge giver mening, får du begge tilbudt, det nærmeste
  først.
* **Søgning offline.** Søgningen kigger nu altid i dine downloadede kort ud over online, så den finder
  langt flere steder uden navn. Hvis du søger uden internetforbindelse og ikke har et offlinekort over
  det sted, du er, siger Soundscape det i stedet for bare ikke at finde noget.

### Åbn et sted i en anden app

Oplysninger om placering har en ny knap, **Åbn i kortapp**, som viser kort- og navigationsapps på din
telefon. Sæt flueben i *Brug altid denne app*, så hedder knappen fx *Åbn i Google Maps* og åbner den
med det samme; et langt tryk henter listen frem igen. Listerne *Steder i nærheden* og *Mærker* har
også skærmlæserhandlingerne *Åbn i …* og *Del* ved siden af *Start lydfyr*.

### Lydfyret og mærker

* Lydfyret på startskærmen viser nu sin **afstand og retning**, og en skærmlæser læser det fx som
  "Lydfyr ved Milngavie Library, 390 meter, sydøst". Ruter viser på samme måde afstanden til det
  aktuelle vejpunkt.
* Lydfyret har tre **skærmlæserhandlinger**: *Lydbesked om lydfyr* siger, hvor det er, *Flere
  oplysninger* tilføjer adressen, og *Føj til mærker* gemmer det.
* Du kan igen **flytte et mærke** ved at trække i kortet fra skærmen *Rediger mærke*.

### Bedre adresser og stednavne

Der er lagt meget arbejde i, at Soundscape beskriver steder, som et menneske ville gøre:

* Steder uden egen adresse beskrives nu ud fra den gade og det område, de ligger i, i stedet for at
  forblive vage.
* Husnumre knyttes til den rigtige side af gaden. Tidligere kunne en adresse blive meldt fra det
  modsatte fortov.
* Et steds adresse gentager ikke længere stedets eget navn.
* Busstoppesteder i Storbritannien bruger deres officielle navne fra den kollektive trafik, som regel
  dem, der står på køreplanen og på skiltet ved stoppestedet.
* Unavngivne stier langs en flod eller kanal opkaldes nu efter det vand, de følger.
* Stier og veje uden navn beskrives mere fornuftigt, og de ord, der bruges om dem, er ordentligt
  oversat i stedet for at optræde på engelsk.

### Sprog

Tyve nye sprog er kommet til i 2.0: arabisk, bengali, bulgarsk, catalansk, kroatisk, tjekkisk,
hausa, ungarsk, indonesisk, koreansk, marathi, serbisk, slovakisk, slovensk, swahili, tamil, telugu,
thai, urdu og vietnamesisk. Disse sprog er alle i alfa-stadiet, og vi vil meget gerne have
tilbagemeldinger om, hvor præcise de er. I alt er Soundscape nu tilgængelig på 46 sprog, og dette
dokumentationswebsted er også oversat.

Egyptisk arabisk er lagt sammen med arabisk, og luganda er trukket tilbage, da ingen af dem havde
nok oversat tekst til at være nyttige.

Oversættelser er fælles arbejde, og vi tager gerne imod din hjælp eller dine rettelser, hvor noget
læses dårligt. Enhver tekst kan forbedres på
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Dvaletilstand

Dvaletilstand har fået **vækning ved afgang**. Når du sætter Soundscape i dvale, kan du bede den
vågne igen, så snart du forlader området, hvilket er nyttigt, når du ankommer et sted og vil have ro,
indtil du tager af sted igen.

### Afstande og tale

Talte afstande er blevet kortere og mere naturlige, og Soundscape skifter nu til større enheder, når
du bevæger dig hurtigt — miles eller kilometer i stedet for en lang optælling i fod eller meter.
Hvert sprog bestemmer selv, hvordan en brøkdelsafstand udtales, hvilket tidligere var presset ind i
et engelsk formet mønster.

### Offlinekort

Offlinekort kom med 1.0 og er løbende blevet forbedret:

* Et hentet kort kan nu opdateres på stedet, når en nyere version er tilgængelig, fra
  detaljeskærmen for kortudsnittet.
* Kort, der ikke kan bruges — for eksempel en beskadiget hentning — markeres nu tydeligt i stedet for
  at fejle i stilhed.
* Hentninger er mere pålidelige, og skærmen viser, hvad der sker, mens listen over tilgængelige kort
  hentes, i stedet for en indlæsningsindikator på hele skærmen.
* En færdig hentning vises først som færdig, når den reelt er klar til brug.
* Der findes et [kort over de tilgængelige regioner]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  på dette websted.

### Tilgængelighed

Der er lagt meget stort arbejde i skærmlæseres adfærd, især i introduktionsskærmene, hvor fokus
tidligere sprang det forkerte sted hen. Andre forbedringer omfatter bedre oplæsning af filstørrelser
og decimaltal, korrekte hjælpetekster af typen »dobbelttryk for at ...« på sprog, der placerer
udsagnsordet sidst, og fornuftige hjælpetekster, hvor der slet ingen var.

### Menuer og navigation

* **Afslut Soundscape** er nu det første punkt i hovedmenuen i stedet for at ligge længere nede.
* Hovedmenuen viser ikke længere en stribe af skærmen i den ene side, hvilket gav skærmlæserbrugere
  et forvirrende ekstra område at trykke på.
* Systemets tilbage-bevægelse springer ikke længere et niveau over, når du gennemser kategorier under
  Steder i nærheden.
* *Lydvejledningen* er omdøbt til **guidet vejledning**.
* Indstillingerne er ryddet op, og *Nulstil til standardværdier* rydder nu alt korrekt.

### Stabilitet

2.0 indeholder en lang liste af rettede nedbrud og fastfrysninger, blandt andet at appen frøs på
startskærmen, fastfrysninger ved nulstilling af indstillinger, nedbrud ved et beskadiget hentet kort,
nedbrud ved åbning af rutedetaljer fra startskærmen, nedbrud ved sprogskift samt flere problemer, der
blev indberettet automatisk via Play Butik. Adfærden omkring batteri og opstart er også gjort mere
robust på telefoner, der aggressivt lukker baggrundsapps.

### Fjernede funktioner
{: #things-that-have-been-removed }

* **Stemmestyringen** er fjernet. Den fungerede aldrig pålideligt nok til at beholde, og
  medieknapperne på hovedtelefoner dækker stort set det samme — se
  [Hjælp til brug af medieknapper]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape er også klar til stemmekommandoer via Gemini på Android 16 og nyere, men de virker først,
  når Google frigiver understøttelsen i Gemini.
* **Sprogmenuen inde i appen** er væk. Soundscape følger nu det sprog, du har valgt på din telefon,
  hvilket er, hvad de fleste forventede. Vil du ændre det, så skift telefonens sprog, eller vælg et
  sprog pr. app i telefonens indstillinger, hvis den tilbyder det.

## Sådan fortæller du os om problemer

Er der noget galt, hører vi gerne om det. Skriv til Help Desk på
<soundscapeAndroid@scottishtecharmy.support>, eller spørg på Slack, hvis du er STA-medlem.

Var en melding forkert eller udeblev den, hjælper en optagelse af din tur os enormt — vi kan afspille
den igen og se præcis, hvad Soundscape arbejdede ud fra. Vejledning findes under
[Levering af en positionsoptagelse til fejlfinding]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## En bemærkning om iPhone

Alt ovenstående handler om Android-appen, men det er værd at vide, hvor resten af arbejdet i denne
udgivelse er gået hen. Soundscape kører nu også på iPhone, og begge apps bygges ud fra den samme
fælles kode — de samme skærme, de samme formuleringer og de samme meldinger. En ny funktion som
rejsemeldingerne ovenfor kommer derfor til begge på én gang i stedet for at blive skrevet to gange.
Det fælles fundament er grunden til, at 2.0 tog så lang tid, og det er også det, der skulle få
fremtidige udgivelser til at komme hurtigere på begge platforme. iPhone-appen er i øjeblikket
tilgængelig via TestFlight efter invitation: spørg på Slack, hvis du er STA-medlem, eller skriv til
Help Desk.
