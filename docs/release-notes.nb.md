---
title: Versjonsmerknader
layout: page
nav_order: 5
has_toc: false
lang: nb
permalink: /release-notes.html
machine-translated: true
---

# Versjonsmerknader

Soundscape 2.0 er en stor utgivelse og er for tiden i lukket beta. Den viktigste endringen er at
Soundscape nå har noe nyttig å si når du reiser med bil, buss eller tog, og ikke lenger bare når du
går. I tillegg kommer mye mindre arbeid med hvordan steder beskrives, tjue nye språk og en lang liste
med feilrettinger.

Merknader for eldre versjoner finnes på siden
[Versjonsmerknader for 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Nytt i 2.0

* **Meldinger under reiser med bil, buss eller tog.** Soundscape merker når du beveger deg i fart og
  beskriver reisen din i stedet for de nære omgivelsene.
* **Beskjed når du krysser vann og jernbaner.** Elver, kanaler, fjorder og jernbanelinjer kunngjøres
  når du krysser dem, både til fots og underveis.
* **Bedre adresser og stedsnavn.** Steder uten egen adresse får nå gaten og området de ligger i,
  husnumre knyttes til riktig side av gaten, og bussholdeplasser i Storbritannia bruker de offisielle
  navnene sine.
* **Tjue nye språk**, slik at det nå er 46 til sammen. Også dette dokumentasjonsnettstedet er
  oversatt.
* **Vekking ved avreise.** Hvilemodus kan nå vekke Soundscape igjen når du forlater stedet der du
  satte den i hvile.
* **Kortere, mer naturlige avstander**, med større enheter når du beveger deg raskt.
* **En raskere vei ut.** *Avslutt Soundscape* ligger nå øverst i hovedmenyen.
* **Forbedringer av frakoblede kart**, blant annet oppdatering av et allerede nedlastet kart og et
  kart over tilgjengelige regioner på dette nettstedet.
* **Mye tilgjengelighetsarbeid** med TalkBack, særlig rundt introduksjonsskjermene.
* **Svært mange rettinger av krasj og stabilitet.**

To ting er **fjernet** i 2.0: talestyringen og språkmenyen inne i appen. Se
[Fjernede funksjoner](#things-that-have-been-removed) nedenfor for hva du kan gjøre i stedet.

---

## Mer utfyllende

### Å reise med bil, buss eller tog

Dette er den største nyheten for eksisterende brukere. Tidligere hadde Soundscape svært lite å si så
snart du satte deg inn i et kjøretøy: den fortsatte å beskrive de nære omgivelsene, noe som i fart
betydde en strøm av ting du for lengst hadde passert.

Soundscape merker nå at du beveger deg raskere enn gangfart og endrer det den forteller deg. Det er
ingenting å slå på, og alt går tilbake til det normale av seg selv så snart du senker farten eller
går av og går videre til fots.

Underveis hører du:

* **Hvor du er**, med jevne mellomrom — veien du kjører på og retningen din, for eksempel «Kjører
  nordover langs M8». Veier med nummer kunngjøres med nummeret sitt, og Soundscape gjentar ikke den
  samme veien hver gang gatenavnet skifter.
* **Byer og tettsteder** du kjører mot, med avstanden, samt de du kjører fra eller bare passerer.
* **Motorveikryss og avkjørsler** når du når dem.
* **Store landemerker** du passerer, som parker, sykehus, stadioner og kjøpesentre.
* **Buss-, trikke- og togholdeplasser** du passerer. Soundscape nevner bare holdeplassene på din side
  av veien, siden de på motsatt side betjener motsatt retning.
* **Elver, kanaler og jernbaner du krysser.**
* **Tunneler**, noe som først og fremst forklarer hvorfor Soundscape er i ferd med å bli stille — det
  er ikke GPS-signal der inne.

På et **tog** finner Soundscape ut at du er på en jernbane og ikke en vei, og forteller deg hvilke
steder du passerer og hvor langt du har kommet siden forrige stasjon. Det er vanskeligere enn det
høres ut, for motorveier og jernbanelinjer bygges ofte ved siden av hverandre i mil etter mil, så en
god del av arbeidet i denne utgivelsen gikk med til å ikke forveksle det ene med det andre.

De vanlige meldingene for gående — butikker i nærheten, fotgjengeroverganger og så videre — holdes
bevisst tilbake mens du reiser, og avstandene ting kunngjøres på er strukket betydelig, slik at du
får vite om noe før du allerede har passert det.

### Å krysse vann og jernbaner

Soundscape forteller deg nå når du krysser en elv, en kanal, en fjord, en bukt eller en
jernbanelinje. Det virker både til fots og underveis, og dekker både å gå under og over, slik at både
en gangbro og en undergang beskrives.

### Bedre adresser og stedsnavn

Det er lagt ned mye arbeid i at Soundscape skal beskrive steder slik et menneske ville gjort:

* Steder uten egen adresse beskrives nå med gaten og området de ligger i, i stedet for å forbli vage.
* Husnumre knyttes til riktig side av gaten. Tidligere kunne en adresse bli meldt fra fortauet på
  motsatt side.
* Adressen til et sted gjentar ikke lenger stedets eget navn.
* Bussholdeplasser i Storbritannia bruker de offisielle navnene fra kollektivtrafikken, vanligvis de
  som står på rutetabellen og på skiltet ved holdeplassen.
* Navnløse gangstier langs en elv eller kanal oppkalles nå etter vannet de følger.
* Stier og veier uten navn beskrives mer fornuftig, og ordene som brukes om dem er ordentlig oversatt
  i stedet for å vises på engelsk.

### Språk

Tjue nye språk er lagt til i 2.0: arabisk, bengali, bulgarsk, katalansk, kroatisk, tsjekkisk, hausa,
ungarsk, indonesisk, koreansk, marathi, serbisk, slovakisk, slovensk, swahili, tamil, telugu, thai,
urdu og vietnamesisk. Alle disse språkene er i alfastadiet, og vi ønsker gjerne tilbakemelding om hvor
nøyaktige de er. Til sammen er Soundscape nå tilgjengelig på 46 språk, og dette
dokumentasjonsnettstedet er også oversatt.

Egyptisk arabisk er slått sammen med arabisk, og luganda er trukket tilbake, siden ingen av dem hadde
nok oversatt tekst til å være nyttige.

Oversettelser er fellesarbeid, og vi tar gjerne imot hjelpen din, eller rettelser der noe leses
dårlig. Enhver tekst kan forbedres på
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Hvilemodus

Hvilemodus har fått **vekking ved avreise**. Når du setter Soundscape i hvile, kan du be den våkne
igjen så snart du forlater området, noe som er nyttig når du kommer fram et sted og vil ha ro til du
drar videre.

### Avstander og tale

Talte avstander er kortet ned og gjort mer naturlige, og Soundscape går nå over til større enheter når
du beveger deg raskt — miles eller kilometer i stedet for en lang opptelling i fot eller meter. Hvert
språk avgjør selv hvordan en brøkdelsavstand uttrykkes, noe som tidligere var presset inn i et
engelskformet mønster.

### Frakoblede kart

Frakoblede kart kom med 1.0 og er stadig blitt forbedret:

* Et nedlastet kart kan nå oppdateres på stedet når en nyere versjon finnes, fra detaljskjermen for
  kartutsnittet.
* Kart som ikke kan brukes — for eksempel en skadet nedlasting — merkes nå tydelig i stedet for å
  feile i stillhet.
* Nedlastinger er mer pålitelige, og skjermen viser hva som skjer mens listen over tilgjengelige kart
  hentes, i stedet for en lasteindikator over hele skjermen.
* En fullført nedlasting vises som fullført først når den faktisk er klar til bruk.
* Det finnes et [kart over tilgjengelige regioner]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  på dette nettstedet.

### Tilgjengelighet

Det er lagt ned svært mye arbeid i hvordan skjermlesere oppfører seg, særlig i introduksjonsskjermene
der fokus tidligere hoppet til feil sted. Andre forbedringer omfatter bedre opplesing av
filstørrelser og desimaltall, riktige hint av typen «dobbelttrykk for å ...» på språk som setter
verbet sist, og fornuftige hint der det ikke var satt noen i det hele tatt.

### Menyer og navigasjon

* **Avslutt Soundscape** er nå det første punktet i hovedmenyen i stedet for å ligge lenger ned.
* Hovedmenyen viser ikke lenger en stripe av skjermen på den ene siden, noe som ga skjermleserbrukere
  et forvirrende ekstra område å trykke på.
* Systemets tilbakebevegelse hopper ikke lenger over et nivå når du blar gjennom kategorier i Steder i
  nærheten.
* *Lydveiledningen* har byttet navn til **veiledet opplæring**.
* Innstillingene er ryddet, og *Tilbakestill til standardverdier* tømmer nå alt riktig.

### Stabilitet

2.0 inneholder en lang liste med rettede krasj og fastlåsinger, blant annet at appen frøs på
startskjermen, fastlåsinger ved tilbakestilling av innstillinger, krasj ved et skadet nedlastet kart,
krasj ved åpning av rutedetaljer fra startskjermen, krasj ved språkbytte samt flere problemer som ble
rapportert automatisk via Play-butikken. Oppførselen rundt batteri og oppstart er også gjort mer
robust på telefoner som aggressivt lukker apper i bakgrunnen.

### Fjernede funksjoner
{: #things-that-have-been-removed }

* **Talestyringen** er fjernet. Den fungerte aldri pålitelig nok til å beholdes, og medieknappene på
  hodetelefoner dekker stort sett det samme — se
  [Hjelp til bruk av medieknapper]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Språkmenyen inne i appen** er borte. Soundscape følger nå språket du har valgt på telefonen, som
  er det de fleste ventet. Vil du endre det, bytter du telefonens språk eller angir et språk per app i
  telefonens innstillinger, dersom den tilbyr det.

## Å melde fra om problemer

Er det noe som ikke stemmer, vil vi gjerne høre om det. Skriv til Help Desk på
<soundscapeAndroid@scottishtecharmy.support>, eller spør på Slack hvis du er STA-medlem.

Var en melding feil eller uteble den, hjelper et opptak av reisen din oss enormt — vi kan spille det
av på nytt og se nøyaktig hva Soundscape jobbet ut fra. Veiledning finnes under
[Levere et posisjonsopptak for feilsøking]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## En merknad om iPhone

Alt ovenfor handler om Android-appen, men det er verdt å vite hvor resten av arbeidet i denne
utgivelsen tok veien. Soundscape kjører nå også på iPhone, og begge appene bygges fra den samme delte
koden — de samme skjermene, de samme formuleringene og de samme meldingene. En nyhet som
reisemeldingene ovenfor kommer derfor til begge samtidig i stedet for å skrives to ganger. Det felles
grunnlaget er grunnen til at 2.0 tok så lang tid, og det er også det som bør gjøre at framtidige
utgivelser kommer raskere på begge plattformene. iPhone-appen er for tiden tilgjengelig via
TestFlight etter invitasjon: spør på Slack hvis du er STA-medlem, eller skriv til Help Desk.
