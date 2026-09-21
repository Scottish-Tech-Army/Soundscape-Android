---
title: Bilješke o izdanju
layout: page
nav_order: 5
has_toc: false
lang: hr
permalink: /release-notes.html
machine-translated: true
---

# Bilješke o izdanju

Soundscape 2.0 veliko je izdanje i trenutačno je u zatvorenoj beta verziji. Glavna je promjena to što
Soundscape sada ima nešto korisno za reći i kada putujete automobilom, autobusom ili vlakom, a ne samo
kada hodate. Uz to je obavljeno mnogo manjih zahvata u načinu opisivanja mjesta, dodano je dvadeset
novih jezika i dug je popis ispravaka.

Bilješke za starija izdanja nalaze se na stranici
[Bilješke o izdanjima 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novosti u verziji 2.0

* **Obavijesti tijekom putovanja automobilom, autobusom ili vlakom.** Soundscape prepoznaje da se
  krećete brzinom i opisuje vaše putovanje umjesto neposredne okoline.
* **Obavijest kada prelazite vode i željezničke pruge.** Rijeke, kanali, zaljevi i željezničke pruge
  najavljuju se dok ih prelazite — i pješice i u vožnji.
* **Bolje adrese i nazivi mjesta.** Mjesta bez vlastite adrese sada dobivaju ulicu i područje u kojem
  se nalaze, kućni brojevi pridružuju se ispravnoj strani ulice, a autobusna stajališta u Velikoj
  Britaniji koriste svoje službene nazive.
* **Dvadeset novih jezika**, čime ih je ukupno 46. Prevedeno je i ovo dokumentacijsko web-mjesto.
* **Buđenje pri odlasku.** Način mirovanja sada može probuditi Soundscape kada napustite mjesto na
  kojem ste ga uspavali.
* **Kraće, prirodnije udaljenosti**, s većim jedinicama kada se krećete brzo.
* **Brži izlaz.** *Izađi iz Soundscapea* sada je na vrhu glavnog izbornika.
* **Poboljšanja izvanmrežnih karata**, uključujući ažuriranje već preuzete karte i kartu dostupnih
  regija na ovom web-mjestu.
* **Mnogo rada na pristupačnosti** s TalkBackom, osobito oko uvodnih zaslona.
* **Vrlo mnogo ispravaka rušenja i stabilnosti.**

U verziji 2.0 **uklonjene** su dvije stvari: glasovno upravljanje i izbornik jezika unutar aplikacije.
Pogledajte niže [Uklonjene značajke](#things-that-have-been-removed) za to što možete učiniti umjesto
toga.

---

## Detaljnije

### Putovanje automobilom, autobusom ili vlakom

Ovo je najveća novost za postojeće korisnike. Prije je Soundscape imao vrlo malo toga za reći čim
biste ušli u vozilo: nastavljao je opisivati neposrednu okolinu, što je pri brzini značilo niz stvari
pokraj kojih ste odavno prošli.

Soundscape sada primjećuje da se krećete brže od hoda i mijenja ono što vam govori. Ništa se ne mora
uključivati, a sve se samo vraća u normalu čim usporite ili izađete i nastavite pješice.

Tijekom putovanja čut ćete:

* **Gdje se nalazite**, s vremena na vrijeme — cestu kojom se krećete i smjer, primjerice „Vožnja
  prema sjeveru cestom M8”. Ceste s brojem najavljuju se svojim brojem, a Soundscape ne ponavlja istu
  cestu svaki put kad se promijeni naziv ulice.
* **Gradove i sela** prema kojima idete, s udaljenošću, kao i one od kojih se udaljavate ili pokraj
  kojih jednostavno prolazite.
* **Čvorišta i izlaze s autoceste** kada do njih dođete.
* **Velike orijentire** pokraj kojih prolazite, poput parkova, bolnica, stadiona i trgovačkih centara.
* **Autobusna, tramvajska i željeznička stajališta** pokraj kojih prolazite. Soundscape spominje samo
  stajališta na vašoj strani ceste jer ona na suprotnoj strani služe suprotnom smjeru.
* **Rijeke, kanale i željezničke pruge koje prelazite.**
* **Tunele**, što ponajviše objašnjava zašto će Soundscape uskoro utihnuti — unutra nema GPS signala.

U **vlaku** Soundscape prepoznaje da ste na željezničkoj pruzi, a ne na cesti, i govori vam pokraj
kojih mjesta prolazite i koliko ste prešli od posljednjeg kolodvora. To je teže nego što zvuči jer se
autoceste i željezničke pruge često grade jedna uz drugu kilometrima, pa je dobar dio rada u ovom
izdanju otišao na to da se jedno ne zamijeni s drugim.

Uobičajene obavijesti za pješake — obližnje trgovine, pješački prijelazi i tako dalje — namjerno se
zadržavaju dok putujete, a udaljenosti na kojima se stvari najavljuju znatno su povećane kako biste za
nešto saznali prije nego što to prođete.

### Prelaženje voda i željezničkih pruga

Soundscape vam sada govori kada prelazite rijeku, kanal, zaljev, uvalu ili željezničku prugu. To radi
i pješice i u vožnji te obuhvaća i prolazak ispod i iznad, pa se opisuju i pješački most i podvožnjak.

### Bolje adrese i nazivi mjesta

Mnogo je rada uloženo u to da Soundscape opisuje mjesta onako kako bi to učinio čovjek:

* Mjesta bez vlastite adrese sada se opisuju ulicom i područjem u kojem se nalaze, umjesto da ostanu
  neodređena.
* Kućni brojevi pridružuju se ispravnoj strani ulice. Prije je adresa mogla biti prijavljena s
  nasuprotnog pločnika.
* Adresa mjesta više ne ponavlja naziv samog mjesta.
* Autobusna stajališta u Velikoj Britaniji koriste službene nazive javnog prijevoza, obično one s
  voznog reda i s oznake na stajalištu.
* Neimenovane staze koje idu uz rijeku ili kanal sada se nazivaju prema vodi koju prate.
* Staze i ceste bez naziva opisuju se smislenije, a riječi koje se za njih koriste uredno su prevedene
  umjesto da se pojavljuju na engleskom.

### Jezici

U verziji 2.0 dodano je dvadeset novih jezika: arapski, bengalski, bugarski, katalonski, hrvatski,
češki, hausa, mađarski, indonezijski, korejski, marathi, srpski, slovački, slovenski, svahili,
tamilski, telugu, tajlandski, urdu i vijetnamski. Svi su ti jezici u alfa fazi i vrlo nam je stalo do
povratnih informacija o njihovoj točnosti. Ukupno je Soundscape sada dostupan na 46 jezika, a
prevedeno je i ovo dokumentacijsko web-mjesto.

Egipatski arapski spojen je s arapskim, a luganda je povučena jer nijedan nije imao dovoljno
prevedenog teksta da bi bio koristan.

Prijevodi su rad zajednice i rado ćemo primiti vašu pomoć ili ispravke ondje gdje se nešto loše čita.
Svaki se tekst može poboljšati na
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Način mirovanja

Način mirovanja dobio je **buđenje pri odlasku**. Kada uspavate Soundscape, možete ga zamoliti da se
probudi čim napustite područje, što je korisno kada negdje stignete i želite mir dok ponovno ne
krenete.

### Udaljenosti i govor

Izgovorene su udaljenosti skraćene i zvuče prirodnije, a Soundscape sada prelazi na veće jedinice kada
se krećete brzo — milje ili kilometre umjesto dugog brojanja u stopama ili metrima. Svaki jezik sam
odlučuje kako izgovoriti djelomičnu udaljenost, što je prije bilo utisnuto u obrazac engleskog oblika.

### Izvanmrežne karte

Izvanmrežne karte stigle su s verzijom 1.0 i stalno se poboljšavaju:

* Preuzeta se karta sada može ažurirati na mjestu kada je dostupna novija verzija, sa zaslona s
  pojedinostima isječka.
* Karte koje se ne mogu upotrijebiti — primjerice oštećeno preuzimanje — sada su jasno označene umjesto
  da tiho zakažu.
* Preuzimanja su pouzdanija, a zaslon pokazuje što se događa dok se dohvaća popis dostupnih karata,
  umjesto pokazivača učitavanja preko cijelog zaslona.
* Dovršeno se preuzimanje prikazuje kao dovršeno tek kada je uistinu spremno za upotrebu.
* Na ovom web-mjestu postoji
  [karta dostupnih regija]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Pristupačnost

Uloženo je vrlo mnogo rada u ponašanje čitača zaslona, osobito na uvodnim zaslonima gdje je fokus prije
skakao na pogrešno mjesto. Ostala poboljšanja uključuju bolje čitanje veličina datoteka i decimalnih
brojeva, ispravne natuknice tipa „dvaput dodirnite za...” u jezicima koji glagol stavljaju na kraj te
smislene natuknice ondje gdje ih uopće nije bilo.

### Izbornici i navigacija

* **Izađi iz Soundscapea** sada je prva stavka glavnog izbornika umjesto da bude niže.
* Glavni izbornik više ne ostavlja vidljivu traku zaslona sa strane, što je korisnicima čitača zaslona
  davalo zbunjujuće dodatno područje za dodir.
* Sistemska gesta natrag više ne preskače razinu dok pregledavate kategorije u Mjestima u blizini.
* *Zvučni vodič* preimenovan je u **vođeni vodič**.
* Postavke su pospremljene, a *Vrati na zadano* sada ispravno briše sve.

### Stabilnost

Verzija 2.0 uključuje dug popis ispravljenih rušenja i zamrzavanja, među njima zamrzavanje aplikacije
na početnom zaslonu, zamrzavanja pri vraćanju postavki, rušenja uz oštećenu preuzetu kartu, rušenja pri
otvaranju pojedinosti rute s početnog zaslona, rušenja pri promjeni jezika te nekoliko problema
prijavljenih automatski putem Trgovine Play. Ponašanje vezano uz bateriju i pokretanje također je
učvršćeno na telefonima koji agresivno zatvaraju pozadinske aplikacije.

### Uklonjene značajke
{: #things-that-have-been-removed }

* **Glasovno upravljanje** uklonjeno je. Nikada nije radilo dovoljno pouzdano da bi ga se zadržalo, a
  medijske tipke na slušalicama pokrivaju uvelike isto — pogledajte
  [Pomoć za upotrebu medijskih tipki]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Izbornik jezika unutar aplikacije** nestao je. Soundscape sada slijedi jezik postavljen na vašem
  telefonu, što je većina ljudi ionako očekivala. Za promjenu promijenite jezik telefona ili u
  njegovim postavkama odredite jezik po aplikaciji, ako to nudi.

## Kako nam prijaviti probleme

Ako nešto nije u redu, rado ćemo čuti. Pišite Help Desku na
<soundscapeAndroid@scottishtecharmy.support> ili pitajte na Slacku ako ste član STA-a.

Ako je obavijest bila pogrešna ili je izostala, snimka vašeg putovanja iznimno nam pomaže — možemo je
reproducirati i vidjeti točno s čime je Soundscape radio. Upute se nalaze u odjeljku
[Dostavljanje snimke lokacije za otklanjanje pogrešaka]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Napomena o iPhoneu

Sve gore navedeno odnosi se na Android aplikaciju, ali vrijedi znati kamo je otišao ostatak rada u ovom
izdanju. Soundscape sada radi i na iPhoneu, a obje se aplikacije grade iz istog zajedničkog koda —
isti zasloni, iste formulacije i iste obavijesti. Novost poput gornjih putnih obavijesti tako stiže na
obje odjednom umjesto da se piše dvaput. Ta zajednička osnova razlog je zašto je 2.0 trajala tako dugo
i ona bi trebala učiniti da buduća izdanja brže stižu na obje platforme. Aplikacija za iPhone
trenutačno je dostupna putem TestFlighta uz poziv: pitajte na Slacku ako ste član STA-a ili pišite Help
Desku.
