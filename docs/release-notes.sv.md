---
title: Versionsinformation
layout: page
nav_order: 5
has_toc: false
lang: sv
permalink: /release-notes.html
machine-translated: true
---

# Versionsinformation

Soundscape 2.0 är en stor version och befinner sig för närvarande i sluten betatestning. Den
viktigaste förändringen är att Soundscape nu har något användbart att säga när du reser med bil,
buss eller tåg, och inte längre bara när du går. Därtill kommer en mängd mindre arbete med hur
platser beskrivs, tjugo nya språk och en lång lista med rättningar.

Information om äldre versioner finns på sidan
[Versionsinformation för 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nyheter i 2.0

* **Meddelanden under resor med bil, buss eller tåg.** Soundscape märker när du rör dig i hastighet
  och beskriver din resa i stället för din omedelbara omgivning.
* **Besked när du korsar vatten och järnvägar.** Floder, kanaler, fjärdar och järnvägslinjer
  annonseras när du korsar dem, både till fots och under resa.
* **Välj hur mycket Soundscape säger.** Den nya inställningen *Detaljnivå för informationsljud* gör
  Soundscape tystare på livliga platser, och med *Platser att läsa upp* väljer du vilka slags platser
  du hör om. Du kan ändra detaljnivån med knapparna på dina hörlurar medan du går.
* **Hör hur långt det är till nästa vägkorsning.** Vägkorsningar läses upp på ett jämnt avstånd när
  du närmar dig, och informationsljudet säger nu hur långt det är till trottoarkanten.
* **Sök efter en typ av plats, eller efter koordinater.** Sök efter ”apotek” eller ”busshållplats” för
  att hitta de närmaste, vad de än heter, eller klistra in koordinater, en kartlänk eller en Plus
  Code.
* **Öppna en plats i en annan kartapp**, till exempel Google Maps, från platsuppgifterna eller från
  listorna.
* **Mer från ljudfyren på startskärmen.** Den visar nu avstånd och riktning, och har
  skärmläsaråtgärder för att läsa upp ljudfyren, höra mer om den eller spara den som platsmarkör.
* **Bättre adresser och ortnamn.** Platser utan egen adress får nu den gata och det område de ligger
  i, husnummer kopplas till rätt sida av gatan, och busshållplatser i Storbritannien använder sina
  officiella namn.
* **Tjugo nya språk**, vilket ger totalt 46. Även den här dokumentationswebbplatsen är översatt.
* **Väckning vid avfärd.** Viloläget kan nu väcka Soundscape igen när du lämnar platsen där du
  försatte det i vila.
* **Kortare, mer naturliga avstånd**, med större enheter när du rör dig snabbt.
* **En snabbare väg ut.** *Avsluta Soundscape* ligger nu överst i huvudmenyn.
* **Förbättringar av offlinekartor**, bland annat uppdatering av en redan hämtad karta och en karta
  över tillgängliga regioner på den här webbplatsen.
* **Mycket tillgänglighetsarbete** med TalkBack, särskilt kring introduktionsskärmarna.
* **Väldigt många rättningar av krascher och stabilitet.**

Två saker har **tagits bort** i 2.0: röststyrningen och språkmenyn inne i appen. Se
[Borttagna funktioner](#things-that-have-been-removed) nedan för vad du kan göra i stället.

---

## Mer utförligt

### Att resa med bil, buss eller tåg

Detta är den största nyheten för befintliga användare. Tidigare hade Soundscape mycket lite att säga
så snart du satte dig i ett fordon: det fortsatte beskriva din omedelbara omgivning, vilket i
hastighet innebar ett flöde av sådant du för länge sedan passerat.

Soundscape märker nu att du färdas snabbare än gångtempo och ändrar vad det berättar. Det finns
inget att slå på, och allt återgår av sig självt till det normala så snart du saktar ned eller stiger
av och går.

Under resan hör du:

* **Var du är**, då och då — vägen du färdas på och din riktning, till exempel »Färd norrut längs
  M8«. Vägar med nummer annonseras med sitt nummer, och Soundscape upprepar inte samma väg varje
  gång gatunamnet ändras.
* **Städer och byar** som du färdas mot, med avståndet, liksom sådana du färdas ifrån eller helt
  enkelt passerar.
* **Motorvägskorsningar och avfarter** när du når dem.
* **Stora landmärken** som du passerar, till exempel parker, sjukhus, arenor och köpcentrum.
* **Buss-, spårvagns- och tåghållplatser** som du passerar. Soundscape nämner bara hållplatser på din
  sida av vägen, eftersom de på motsatt sida betjänar motsatt riktning.
* **Floder, kanaler och järnvägar som du korsar.**
* **Tunnlar**, vilket framför allt förklarar varför Soundscape strax tystnar — det finns ingen
  GPS-signal där inne.

På ett **tåg** räknar Soundscape ut att du befinner dig på en järnväg och inte på en väg, och
berättar vilka orter du passerar och hur långt du kommit sedan senaste station. Det är svårare än det
låter, eftersom motorvägar och järnvägar ofta byggs bredvid varandra i mil efter mil, så en god del
av arbetet i den här versionen gick åt till att inte förväxla det ena med det andra.

De vanliga meddelandena för gående — butiker i närheten, övergångsställen och så vidare — hålls
medvetet tillbaka under resan, och avstånden på vilka saker annonseras har utökats rejält så att du
får veta om något innan du redan passerat det.

### Vägkorsningar

Den vanligaste frågan om informationsljud vid vägkorsningar var hur långt bort korsningen egentligen
är. Nu säger Soundscape det: ”Vägkorsning 30 meter bort”. Avståndet mäts till trottoarkanten på den
gata du ska korsa, inte till mitten av korsningen, eftersom det är där du faktiskt stannar.

Informationsljudet kommer också vid en jämnare punkt. Tidigare kunde det komma 45 meter eller 10 meter
innan, utan något som skilde dem åt. Nu väntar det tills korsningen är ungefär 30 meter bort, så att
avståndet betyder ungefär samma sak varje gång.

### Att korsa vatten och järnvägar

Soundscape talar nu om när du korsar en flod, en kanal, en fjärd, en vik eller en järnvägslinje. Det
fungerar både till fots och under resa och omfattar såväl att gå under som över, så både en gångbro
och en gångtunnel beskrivs.

### Välj hur mycket Soundscape säger

Det vi oftast hör om Soundscape är att den säger för mycket på livliga platser som en stadskärna.
Avsnittet *Hantera informationsljud* i *Inställningar* har nu tre inställningar i stället för den
gamla listan med reglage:

* **Detaljnivå för informationsljud** är Tyst, Grundläggande, Förenklad eller Detaljerad. *Detaljerad* är
  det Soundscape alltid har gjort, och där börjar du. *Förenklad* hoppar över mindre stigar och
  servicevägar och upprepar sig mer sällan. *Grundläggande* läser bara upp gator, vägkorsningar och
  landmärken. *Tyst* ger inga automatiska informationsljud alls, medan ljudfyrar, rutter och knapparna
  på startskärmen fortsätter att fungera. Den ersätter det gamla reglaget *Tillåt informationsljud*,
  och om du hade det avstängt står detaljnivån nu på Tyst.
* **Gator och vägkorsningar** slår på eller av informationsljud om vägkorsningar och om gatan du är
  på.
* **Platser att läsa upp** är en lista att kryssa i: Allt, Landmärken, Kollektivtrafik, Mat och
  dryck, Livsmedelsbutiker och närbutiker, Banker och bankomater eller Inga platser. Kryssa i så många
  du vill, till exempel landmärken och busshållplatser. Dina platsmarkörer läses alltid upp.

Vilken detaljnivå som passar ändras medan du går, så du behöver inte gå in i Inställningar för att
ändra den. När du trycker på *Föregående* på dina hörlurar sänks detaljnivån ett steg i taget, från
Detaljerad via Förenklad och Grundläggande till Tyst, och sedan runt till Detaljerad igen. Den nya nivån
läses upp varje gång. Det fungerar i båda lägena för medieknapparna, och därför har knapparna på
hörlurarna ändrats lite:

* I *Originalläge* läser *Nästa* nu upp *Omkring mig* när ingen rutt spelas, och *Min plats* finns
  inte längre på knapparna. Medan en rutt spelas flyttar *Nästa* och *Föregående* fortfarande mellan
  brytpunkterna.
* I läget *Ljudmeny* går *Föregående* inte längre bakåt i menyn. *Nästa* går fortfarande igenom den
  och *Spela/pausa* väljer fortfarande. Platsmarkörer och rutter står nu i namnordning i menyn, och
  när du har startat en går menyn tillbaka till början i stället för att lämna dig djupt ner i listan.

### Sökning

Sökfältet förstår nu mer än platsnamn:

* **Typer av platser.** Sök efter ”apotek”, ”toalett”, ”bankomat” och så vidare på ditt eget språk,
  så visar Soundscape de närmaste platserna av den typen, vad de än heter. Platser utan namn, som de
  flesta toaletter och bänkar, visas som det de är, med sin adress.
* **Koordinater, kartlänkar och Plus Codes.** Klistra in ett talpar, grader och minuter, en länk från
  Google Maps, Apple Kartor eller OpenStreetMap eller en Plus Code, så ger Soundscape dig exakt den
  platsen. Ett ensamt talpar kan läsas åt båda hållen, så där båda är rimliga får du båda, den
  närmaste först.
* **Sökning offline.** Sökningen letar nu alltid i dina nedladdade kartor utöver online, och hittar
  då många fler platser utan namn. Om du söker utan internetanslutning och inte har någon offlinekarta
  över där du är, säger Soundscape det i stället för att bara inte hitta något.

### Öppna en plats i en annan app

Platsuppgifterna har en ny knapp, **Öppna i kartapp**, som listar kart- och navigeringsapparna på din
telefon. Kryssa i *Använd alltid den här appen* så heter knappen till exempel *Öppna i Google Maps*
och öppnar den direkt; ett långt tryck tar fram listan igen. Listorna *Platser i närheten* och
*Platsmarkörer* har också skärmläsaråtgärderna *Öppna i …* och *Dela*, bredvid *Ställ in ljudfyr*.

### Ljudfyren och platsmarkörer

* Ljudfyren på startskärmen visar nu sitt **avstånd och sin riktning**, och en skärmläsare läser den
  till exempel som ”Ljudfyr på Milngavie Library, 390 meter, sydost”. Rutter visar på samma sätt
  avståndet till den aktuella brytpunkten.
* Ljudfyren har tre **skärmläsaråtgärder**: *Läs upp ljudfyr* säger var den är, *Mer info* lägger
  till adressen och *Lägg till i platsmarkörer* sparar den.
* Du kan åter **flytta en platsmarkör** genom att dra i kartan från skärmen *Redigera platsmarkör*.

### Bättre adresser och ortnamn

Mycket arbete har lagts på att Soundscape ska beskriva platser som en människa skulle göra:

* Platser utan egen adress beskrivs nu med gatan och området de ligger i, i stället för att förbli
  vaga.
* Husnummer kopplas till rätt sida av gatan. Tidigare kunde en adress rapporteras från trottoaren
  mitt emot.
* En plats adress upprepar inte längre platsens eget namn.
* Busshållplatser i Storbritannien använder sina officiella kollektivtrafiknamn, vanligen de som står
  i tidtabellen och på skylten vid hållplatsen.
* Namnlösa gångstigar längs en flod eller kanal namnges nu efter vattnet de följer.
* Stigar och vägar utan namn beskrivs mer vettigt, och orden som används är ordentligt översatta i
  stället för att dyka upp på engelska.

### Språk

Tjugo nya språk har lagts till i 2.0: arabiska, bengali, bulgariska, katalanska, kroatiska,
tjeckiska, hausa, ungerska, indonesiska, koreanska, marathi, serbiska, slovakiska, slovenska,
swahili, tamil, telugu, thailändska, urdu och vietnamesiska. Alla dessa språk befinner sig i
alfastadiet och vi vill gärna ha återkoppling på hur korrekta de är. Totalt finns Soundscape nu på
46 språk, och den här dokumentationswebbplatsen har också översatts.

Egyptisk arabiska har förts samman med arabiska, och luganda har dragits tillbaka, eftersom ingetdera
hade tillräckligt med översatt text för att vara användbart.

Översättningar är gemensamt arbete och vi tar gärna emot din hjälp, eller dina rättelser där något
läses illa. Varje textsträng kan förbättras på
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Viloläge

Viloläget har fått **väckning vid avfärd**. När du försätter Soundscape i vila kan du be det vakna
igen så snart du lämnar området, vilket är praktiskt när du kommer fram någonstans och vill ha lugn
tills du ger dig av igen.

### Avstånd och tal

Talade avstånd har kortats och gjorts mer naturliga, och Soundscape växlar nu till större enheter när
du färdas snabbt — miles eller kilometer i stället för en lång uppräkning i fot eller meter. Varje
språk avgör själv hur ett avstånd i delar uttrycks, något som tidigare tvingades in i ett
engelskformat mönster.

### Offlinekartor

Offlinekartor kom med 1.0 och har förbättrats stadigt:

* En hämtad karta kan nu uppdateras på plats när en nyare version finns, från kartutsnittets
  detaljskärm.
* Kartor som inte går att använda — till exempel en skadad hämtning — markeras nu tydligt i stället
  för att misslyckas i tysthet.
* Hämtningar är mer tillförlitliga, och skärmen visar vad som pågår medan listan över tillgängliga
  kartor hämtas, i stället för en laddningsindikator över hela skärmen.
* En avslutad hämtning visas som klar först när den verkligen är redo att användas.
* Det finns en [karta över tillgängliga regioner]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  på den här webbplatsen.

### Tillgänglighet

Mycket stort arbete har lagts på skärmläsarnas beteende, särskilt i introduktionsskärmarna där fokus
tidigare hoppade till fel ställe. Andra förbättringar är bättre uppläsning av filstorlekar och
decimaltal, korrekta tips av typen »dubbeltryck för att ...« på språk som placerar verbet sist, och
vettiga tips där inga alls hade angetts.

### Menyer och navigering

* **Avsluta Soundscape** är nu första posten i huvudmenyn i stället för att ligga längre ned.
* Huvudmenyn visar inte längre en remsa av skärmen på ena sidan, vilket gav skärmläsaranvändare ett
  förvirrande extra område att trycka på.
* Systemets bakåtgest hoppar inte längre över en nivå när du bläddrar bland kategorier i Platser i
  närheten.
* *Ljudhandledningen* har bytt namn till **guidad handledning**.
* Inställningarna har städats upp, och *Återställ standardvärden* rensar nu allt ordentligt.

### Stabilitet

2.0 innehåller en lång lista med rättade krascher och låsningar, bland annat att appen frös på
startskärmen, låsningar vid återställning av inställningar, krascher vid en skadad hämtad karta,
krascher när ruttdetaljer öppnades från startskärmen, krascher vid språkbyte samt flera problem som
rapporterats automatiskt via Play Butik. Beteendet kring batteri och uppstart har också gjorts mer
robust på telefoner som aggressivt stänger bakgrundsappar.

### Borttagna funktioner
{: #things-that-have-been-removed }

* **Röststyrningen** har tagits bort. Den fungerade aldrig tillräckligt tillförlitligt för att
  behållas, och mediaknapparna på hörlurar täcker till stor del samma behov — se
  [Hjälp om användning av mediaknappar]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape är också redo för röstkommandon via Gemini på Android 16 och senare, men de fungerar
  inte förrän Google släpper stödet för dem i Gemini.
* **Språkmenyn inne i appen** är borta. Soundscape följer nu det språk du ställt in på telefonen,
  vilket är vad de flesta förväntade sig. Vill du ändra det byter du telefonens språk eller anger ett
  språk per app i telefonens inställningar, om den erbjuder det.

## Att berätta för oss om problem

Om något inte stämmer vill vi gärna veta det. Skriv till Help Desk på
<soundscapeAndroid@scottishtecharmy.support>, eller fråga på Slack om du är STA-medlem.

Om ett meddelande blev fel eller uteblev hjälper en inspelning av din resa oss enormt — vi kan spela
upp den igen och se exakt vad Soundscape utgick från. Anvisningar finns under
[Att lämna en positionsinspelning för felsökning]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## En anmärkning om iPhone

Allt ovanstående gäller Android-appen, men det är värt att veta vart resten av arbetet i den här
versionen tog vägen. Soundscape körs nu även på iPhone, och båda apparna byggs från samma delade kod
— samma skärmar, samma formuleringar och samma meddelanden. En nyhet som resemeddelandena ovan når
därför båda samtidigt i stället för att skrivas två gånger. Den gemensamma grunden är skälet till att
2.0 tog så lång tid, och det är den som bör göra att framtida versioner kommer snabbare på båda
plattformarna. iPhone-appen är för närvarande tillgänglig via TestFlight efter inbjudan: fråga på
Slack om du är STA-medlem, eller skriv till Help Desk.
