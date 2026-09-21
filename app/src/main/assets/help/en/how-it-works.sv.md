---
title: Så fungerar Soundscape
layout: page
parent: "Använda Soundscape"
has_toc: false
lang: sv
permalink: /users/how-it-works.html
machine-translated: true
---
# Så fungerar Soundscape
Syftet med den här sidan är att ge en allmän förståelse för hur Soundscape-appen fungerar under huven. Du behöver inte läsa det här för att använda appen, men det finns några anledningar till att den har skrivits:

1. För att hjälpa intresserade nykomlingar till appen att förstå var dess begränsningar finns
1. För att ge användarna en uppfattning om vad mer som kan vara möjligt med nya funktioner
1. För att ge utvecklare en översikt över appens funktion

Det finns två tekniker som gör appen möjlig: GPS och OpenStreetMap-data. GPS:en ger oss en god uppfattning om var telefonen är och var den har varit. OpenStreetMap-data kan sedan användas för att hitta vad som finns i närheten och vi kan använda det för att beskriva det för användaren.

## Ljudfyrar
På de flesta sätt är dessa de enklaste sakerna att implementera ur ett tekniskt perspektiv. Förutsatt att vi har telefonens plats och en riktning för telefonen kan vi sedan förändra ljudet för fyren så att det låter som om det kommer från den riktningen. Vi använder ett bibliotek från Steam Audio för att utföra ljudpositioneringen, som använder huvudrelaterade överföringsfunktioner (head related transfer functions) för att ge bästa möjliga ljudpositionering. Det enda andra vi gör är att ändra fyrljudet så att ett annat ljud spelas beroende på vinkeln mellan användarens riktning och fyrens plats. Vinklarna varierar beroende på vald fyr (Taktil, Bloss, Ping osv.) och vissa har ett större antal ljud än andra. Och det är ljudfyrar på den enklaste nivån.

Den enda ytterligare komplexiteten är antagandet om telefonens plats och den riktning som användaren pekar i. Låt oss titta på dessa i tur och ordning.

### Plats
Platsen som returneras av GPS kan ha ett ganska stort fel, och detta beror på hur mycket av himlen som är synlig för telefonens GPS, och hur många träd och höga byggnader som reflekterar GPS-signalen på vägen till telefonen.

Det tillvägagångssätt vi har valt för att filtrera platsen är att använda det som kallas kartmatchning (map matching). Detta utgår från att användaren med största sannolikhet rör sig längs en kartlagd stig eller väg – vi använder termen ”Way” (väg) för att täcka alla vägar, leder och stigar. Kartmatchning tittar på var användaren har varit och väljer, med hjälp av rörelseriktningen tillsammans med lokala kartdata, den mest sannolika platsen på en Way. Detta tillvägagångssätt tar inte bara hänsyn till fel från GPS:en, utan även fel i kartdatan. Inte alla Ways är kartlagda korrekt och därför har de också fel. För att avgöra vilken som är den mest sannolika Way som användaren befinner sig på tar algoritmen hänsyn till:
* Hur nära en Way ligger GPS-platsen och tidigare GPS-platser
* Färdriktningen – rör de sig i samma riktning som Way
* Huruvida det är möjligt att ta sig från den senast kartmatchade platsen till den nya platsen via nätverket av Ways. Detta krävs för att utesluta växling mellan Ways som inte faktiskt är sammanlänkade, t.ex. att en passerar över en annan via en bro, eller under den via en tunnel.
Kartmatchningen kan komma fram till att det inte finns några närliggande Ways, eller att den inte är säker på vilken användaren befinner sig på, och i det fallet väntar den helt enkelt på nästa GPS-plats och försöker igen tills den är säker.

### Riktning
Det finns flera riktningar som vi spårar i programvaran:

1. Riktningen som telefonen pekar i. Vi använder denna när telefonen är upplåst och appen används, men även när telefonen är låst så länge den hålls plant med skärmen pekande mot himlen. Det är bra att tänka på detta när du lägger telefonen i din väska. Om den läggs plant i botten av en väska som hålls upprätt skulle den slumpmässiga riktning som din väska pekar i användas av appen.
1. Riktningen som telefonen färdas i.
1. Riktningen från hörlurar med huvudspårning (head tracking). Vi använder för närvarande inte detta, även om iOS-appen stödde det. Vi har tekniken på plats för att lägga till det i framtiden.

När telefonen är låst och i en väska kommer appen att använda färdriktningen. Men om användaren inte rör sig finns ingen riktning tillgänglig. När detta händer blir ljudfyrarna tystare för att indikera att det inte är möjligt att veta fyrens aktuella riktning – användaren kan vrida sig runt utan att byta plats.

För vissa användningar av riktning i appen ”snäpps” riktningen till riktningen för den kartmatchade Way:en, så om användaren går ungefär i riktningen för Way:en antas den faktiska riktningen för Way:en vara korrekt och används i dessa beräkningar.

### Slutsats
Även om ljudfyrarna vid första anblicken är enkla, introducerar användningen av kartmatchning för att försöka ta bort plats- och riktningsfel en hel del komplexitet.

## Kartdata
Kartdatan som används av appen härstammar nästan helt från OpenStreetMap-projektet. Vi driver en server som innehåller en karta över hela världen på flera zoomnivåer. Varje zoomnivå är uppdelad i rutor (tiles). Zoomnivå 0 innehåller 1 ruta, nivå 1 innehåller 4 rutor, nivå 2 innehåller 16 rutor och så vidare upp till nivå 14 som innehåller omkring 268 miljoner rutor för att täcka planeten. Varje ruta innehåller flera lager och varje lager har punkter, linjer och polygoner som kan ritas för att skapa en grafisk karta. Den grafiska kartan är det som visas för användaren i appens grafiska gränssnitt. Varje punkt, linje och polygon har metadata som beskriver vad det är. Detta kommer mestadels direkt från OpenStreetMap-data, så en linje kan vara en `footway` som är en `sidewalk` eller en `road` som är en `minor`

Datan omvandlas till den grafiska kartan via en ”stil” (style) som har regler för hur man ritar de olika punkterna, linjerna och polygonerna i varje lager, t.ex. hur man ritar en stig, hur man ritar en skog, hur man ritar en busshållplats. Reglerna kan variera per zoomnivå, vilket är anledningen till att fler och fler punkter och linjer blir synliga när du zoomar in som inte är synliga när du är utzoomad, t.ex. busshållplatser och stigar.

Genom att ändra stilen kan vi ändra hur kartan i gränssnittet ser ut, vilket är var den ”tillgängliga karta” som vi testar kommer ifrån. Den syftar till att ha större kontrast och fetare linjer och text. Stilen är inbyggd i appen, så vi behöver inte ändra kartan på servern för att ändra hur den ser ut.

Men hur använder vi kartdatan för ljud?

### Att använda kartdatan för ljud
Vi använder för närvarande en relativt liten mängd av kartdatan för att generera ljudgränssnittet. Nästan hela ljud-UI:t använder bara rutorna på den maximala zoomnivån. Appen syr ihop ett 2 gånger 2-rutnät av rutor runt där användaren befinner sig och tittar sedan på bara några få av lagren:

* `transportation` – för alla typer av Ways inklusive vägar, stigar, järnvägar och spårvägar.
* `poi` – intressepunkter, t.ex. butiker, idrottsanläggningar, bänkar, brevlådor, busshållplatser osv.
* `building` – detta är för `poi` som är kartlagda som mer än bara en punkt, t.ex. stora stormarknader eller stadshus.

Det förbinder linjer och polygoner över rutgränserna och omvandlar alla Ways till sammanlänkade Way-segment och korsningar. Detta är viktigt eftersom det gör att vi kan söka längs en Way för att ta reda på vart vi kan komma.

All tolkad data placeras också i ett lättsökt format så att appen enkelt kan hitta vilka kartfunktioner som finns i närheten. Vid det här laget klassificeras datan i kategorier. De nuvarande kategorierna är:

* Vägar
* Vägar och stigar (alla Ways)
* Vägkorsningar – de punkter där Ways korsar varandra
* Entréer – dessa är punkter på en byggnad som har markerats som en entré.
* Övergångsställen – vägövergångar
* POI:er – alla intressepunkter
* Kollektivtrafikhållplatser – busshållplatser, järnvägsstationer, spårvagnshållplatser och så vidare.
* Underkategorier av POI:er:
  * Information
  * Objekt
  * Plats
  * Landmärke
  * Mobilitet
  * Säkerhet
* Orter och underkategorier (se nästa avsnitt)
  * Storstäder
  * Städer
  * Byar
  * Småbyar

 Med detta på plats kan appen sedan enkelt hitta, för vilken plats som helst,

 * ”Alla kollektivtrafikhållplatser inom 50 m” eller
 * ”Närmaste vägkorsningen framför mig” eller
 * ”Närmaste småby, by, stad eller storstad”
  
Med detta på plats är skapandet av ljudinformationen bara en fråga om att fråga datan baserat på den aktuella platsen och riktningen. När användaren rör sig över ett rutnät uppdaterar appen det så att det centreras runt den aktuella platsen.

### Mer data
Ett av problemen med vårt mycket lokala kartdatarutnät är att det innebär att vi som mest bara kan ”se” omkring 1 km i någon riktning. Det är okej för när vi beskriver vad som finns framför oss, men ibland skulle vi vilja ge mer sammanhang. Det huvudsakliga exemplet på detta är när appen används och användaren inte går.

När appen upptäcker att användaren färdas i mer än 5 meter per sekund byter den hur den beskriver världen. Istället för att läsa upp varje vägkorsning och POI läser den upp mer sällan och bara närliggande vägar. Problemet med detta är att det inte är särskilt användbart att veta ett vägnamn om du inte vet vilken stad det ligger i.

För att försöka lösa detta tolkar vi nu även kartdatan på en lägre zoomnivå och extraherar data från `place`-lagret. Detta innehåller namnen på städer, storstäder, stadsdelar, byar och så vidare. Ett problem med att kartlägga saker är att det inte alltid finns en uppenbar gräns mellan dessa platser. OpenStreetMap har ibland stadsgränser i sin databas, men även när så är fallet är den informationen ofta förlorad när den når vår rutindelade karta. Det vi har är platsen där platsnamnen ritas på kartan. Dessa kategoriseras och sedan hittar appen den närmaste småbyn, byn, staden eller storstaden till användaren och rapporterar den.

För många storstäder kommer det faktiska storstadsnamnet aldrig att läsas upp, eftersom de flesta storstäder är indelade i mindre indelningar som stadsdelar, men dessa ger extra sammanhang och är mycket användbara. Kom bara ihåg att bara för att appen rapporterar att du är nära en gata i en viss stadsdel betyder det bara att etiketten för den stadsdelen är den närmaste punkten, och den kan vara felaktig eller till och med på andra sidan en flod.

### Mer sammanhang
Ju mer sammanhang som kan läggas till i beskrivningarna desto bättre, så länge det hålls koncist och förutsägbart. Ett av problemen vi såg med att beskriva vägkorsningar var att det ofta var ”namnlösa” Ways inblandade. Dessa är Ways som inte har något namn. I kartdatan kan dessa bara vara en led, en stig eller en servicegata, men utan mer sammanhang är det inte särskilt användbart i textbeskrivningarna. Lyckligtvis kan vi göra bättre, så vad appen gör är att den, närhelst den ska tillkännage en namnlös Way, ser om den kan lista ut mer sammanhang för den.

* **Är det en trottoar?**
Många områden i OpenStreetMap har nu trottoarer kartlagda separat från vägar. Dessa taggas vanligtvis som `sidewalk` men de säger normalt inte vilken väg de är trottoaren till.

    När appen stöter på en namnlös trottoar söker den efter en väg som den tror löper bredvid den och använder den för att namnge trottoaren. Det visar sig vara mycket viktigt för vår information. Istället för att tillkännage varje trottoarkorsning, när vi rör oss längs en trottoar, görs informationen som om vi rörde oss längs den tillhörande vägen. Istället för *”Färdas västerut längs stig”* har vi *”Färdas västerut längs Moor Road”*. Användaren är på den kartlagda trottoaren, men beskrivningen är mer logisk.

* **Slutar den vid en namngiven Way?**
Mycket ofta finns det gångvägar som förbinder två vägar. Genom att titta på båda ändarna av stigen kan vi enkelt lägga till det sammanhanget så att det i en riktning kan vara *”Stig till Moor Road”* och vid ankomst från den andra änden kan det vara *”Stig till Roselea Drive”*. Detta görs bara där stigen inte delar sig; om den delar sig i två namnlösa stigar försöker vi inte lägga till detta sammanhang.

* **Slutar den nära en platsmarkör?**
Om en namnlös Way börjar eller slutar nära en platsmarkör används den för att beskriva den, t.ex. *”Stig till korsningen vid det stora trädet”*. Användaren kan lägga till platsmarkörer var de vill, och genom att lägga till platsmarkörer längs stignätverk kan de lägga till sammanhang för en hel rutt.

* **Går den in i eller ut ur en POI?**
Om en namnlös Way börjar utanför en POI och slutar inuti den (eller tvärtom) kan vi lägga till det sammanhanget, t.ex. *”Led till Lennox Park”*. 

* **Slutar den nära en entré?**
Om en namnlös Way börjar eller slutar närmare en entré kan vi lägga till det sammanhanget, t.ex. *”Servicegata till Best Buy”*.

* **Slutar den nära ett landmärke eller en plats?**
Om en namnlös Way börjar eller slutar närmare ett landmärke kan vi också lägga till det sammanhanget, t.ex. *”Servicegata till St. Giles Cathedral”*.

* **Är det en återvändsgränd?**
Appen markerar som återvändsgränd alla namnlösa Ways som inte leder någonstans.

* **Passerar den några trappor?**
Om den namnlösa Way:en passerar över en bro, genom en tunnel eller upp/ner för trappor noteras detta och läggs till i sammanhanget. Detta är separat från destinationstaggningen så sammanhang som *”Stig över bro till Lennox Park”* är möjligt.

Dessa sammanhang läggs till i ordning och så är det möjligt att ha *”Stig till Park Lane via trappor”* i en riktning och *”Stig till Lennox Park via trappor”* i den andra riktningen. Den namngivna gatan får prioritet på väg ut ur parken, men parken används vid inträde i den.

#### Framtida sammanhang
Det finns olika ytterligare sammanhang som vi hoppas kunna lägga till i framtiden, inklusive:
* Sammanhang för Ways som följer linjära vattendrag, t.ex. *”Stig bredvid River Dee”*
* Sammanhang för Ways som följer kanten av vattenytor, *”Stig bredvid Milngavie-reservoaren ”*
* Sammanhang för Ways som följer järnvägar, t.ex. *”Stig bredvid järnväg”*. Detta kan till och med inkludera namnet på järnvägslinjen
* Lägg till extra innehåll till broar och tunnlar, vad är de över eller under, t.ex. *”Stig via bro över järnväg till Moor Road”*

## Ljudinformation
Nu när vi har kartdatan i ett format som vi enkelt kan använda är det verkligen ganska enkelt att generera ljudinformation.

### Ljudinformation vid promenad
Vid promenad är de ljudinformationer som kan inträffa (i prioritetsordning):

1. Beskriv hur långt bort den aktuella destinationen är
1. Beskriv en kommande vägkorsning
1. Beskriv de 5 närmaste intressepunkterna

All ljudinformation är frekvensbegränsad så att den inte upprepas för ofta. Om användaren slutar röra sig stoppas informationen, och även när användaren rör sig upprepas inte en information vid varje ny GPS-plats. Frekvensen, som i iOS-appen, är:

* Var 60:e sekund för den aktuella destinationen
* Var 30:e sekund för en kommande vägkorsning
* Var 60:e sekund för en intressepunkt

Ljudinformation kan filtreras via inställningsmenyn, och det finns definitivt utrymme att vidga detta beteende.

### Ljudinformation vid snabbare färd
Vid färd i mer än 5 meter per sekund sker fortfarande informationen om den aktuella destinationen, men informationen om vägkorsningar och intressepunkter ersätts av en information som beskriver ungefär var användaren befinner sig. Detta ger en närliggande kollektivtrafikhållplats, en intressepunkt som omsluter oss, t.ex. inuti en stor park, eller en närliggande väg och ort. Dessa använder datan som beskrevs tidigare, och det finns uppenbart utrymme för att tillåta anpassning av detta i framtiden.

## Platsmarkörer och rutter
För det mesta är platsmarkörer och rutter bara en gränssnittsfunktion som varken förlitar sig på GPS eller egentligen på kartdata. Platsmarkörer är namngivna platser som användaren vill spara, och rutter är en ordnad lista över dessa platsmarkörer. Gränssnittet för att skapa båda är taget direkt från iOS-versionen.

### Uppspelning av rutter
Uppspelning av rutter är där rutter får liv. När en rutt spelas upp skapas en ljudfyr vid den första platsmarkören i rutten. När användaren kommer nära den platsmarkören flyttar rutten automatiskt ljudfyren till nästa platsmarkör i rutten. Om det inte finns fler platsmarkörer avslutas uppspelningen av rutten.

## Slutsats
Förhoppningsvis har det gett lite insikt i hur appen fungerar. Appen utvecklas alltid baserat på feedback från användare, så hör gärna av dig om det finns något du tycker skulle kunna läggas till.
