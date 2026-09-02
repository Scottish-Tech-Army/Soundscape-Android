---
title: Note de lansare
layout: page
nav_order: 5
has_toc: false
lang: ro
permalink: /release-notes.html
machine-translated: true
---

# Note de lansare

Soundscape 2.0 este o versiune importantă și se află în prezent în beta închisă. Principala schimbare
este că Soundscape are acum ceva util de spus și atunci când călătoriți cu mașina, autobuzul sau
trenul, nu doar când mergeți pe jos. Există de asemenea multă muncă de mai mică amploare privind modul
în care sunt descrise locurile, douăzeci de limbi noi și o listă lungă de remedieri.

Notele pentru versiunile mai vechi se află pe pagina
[Note de lansare pentru 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Noutăți în 2.0

* **Anunțuri în timpul călătoriei cu mașina, autobuzul sau trenul.** Soundscape recunoaște când vă
  deplasați cu viteză și descrie călătoria în loc de împrejurimile imediate.
* **Anunț când traversați ape și căi ferate.** Râurile, canalele, golfurile și liniile de cale ferată
  sunt anunțate pe măsură ce le traversați, atât pe jos, cât și în deplasare.
* **Adrese și denumiri de locuri mai bune.** Locurile fără adresă proprie primesc acum strada și zona
  în care se află, numerele de casă sunt asociate părții corecte a străzii, iar stațiile de autobuz
  din Marea Britanie folosesc denumirile lor oficiale.
* **Douăzeci de limbi noi**, ajungând la 46 în total. Și acest site de documentație a fost tradus.
* **Trezire la plecare.** Modul de repaus poate acum trezi Soundscape când părăsiți locul în care
  l-ați pus în repaus.
* **Distanțe mai scurte și mai firești**, cu unități mai mari când vă deplasați rapid.
* **O ieșire mai rapidă.** *Ieșire din Soundscape* se află acum în partea de sus a meniului principal.
* **Îmbunătățiri ale hărților offline**, inclusiv actualizarea pe loc a unei hărți deja descărcate și
  o hartă a regiunilor disponibile pe acest site.
* **Multă muncă de accesibilitate** cu TalkBack, mai ales în jurul ecranelor introductive.
* **Foarte multe remedieri de blocări și de stabilitate.**

Două lucruri au fost **eliminate** în 2.0: controlul vocal și meniul de limbă din aplicație. Vedeți
mai jos [Funcții eliminate](#things-that-have-been-removed) pentru ce puteți face în schimb.

---

## Mai în detaliu

### Călătoria cu mașina, autobuzul sau trenul

Aceasta este cea mai mare noutate pentru utilizatorii existenți. Anterior, Soundscape avea foarte
puține de spus imediat ce urcați într-un vehicul: continua să descrie împrejurimile imediate, ceea ce
la viteză însemna un șir de lucruri pe lângă care trecuserăți deja.

Soundscape observă acum că vă deplasați mai repede decât în pas de mers și schimbă ceea ce vă
comunică. Nu este nimic de activat, iar totul revine de la sine la normal imediat ce încetiniți sau
coborâți și mergeți pe jos.

În timpul călătoriei veți auzi:

* **Unde vă aflați**, din când în când — drumul pe care circulați și direcția, de exemplu „Deplasare
  spre nord pe M8”. Drumurile cu număr sunt anunțate prin numărul lor, iar Soundscape nu reanunță
  același drum de fiecare dată când se schimbă numele străzii.
* **Orașele și satele** spre care vă îndreptați, cu distanța, precum și cele de care vă îndepărtați
  sau pe lângă care doar treceți.
* **Nodurile și ieșirile de autostradă** pe măsură ce ajungeți la ele.
* **Repere mari** pe lângă care treceți, cum ar fi parcuri, spitale, stadioane și centre comerciale.
* **Stații de autobuz, tramvai și tren** pe lângă care treceți. Soundscape menționează doar stațiile
  de pe partea dumneavoastră a drumului, întrucât cele de pe partea opusă deservesc sensul contrar.
* **Râuri, canale și căi ferate pe care le traversați.**
* **Tuneluri**, ceea ce explică mai ales de ce Soundscape este pe cale să tacă — înăuntru nu există
  semnal GPS.

Într-un **tren**, Soundscape își dă seama că vă aflați pe o cale ferată, nu pe un drum, și vă spune pe
lângă ce localități treceți și cât ați parcurs de la ultima stație. Acest lucru este mai greu decât
pare, deoarece autostrăzile și liniile ferate sunt adesea construite alături kilometri întregi, așa că
o bună parte din munca acestei versiuni a mers în a nu confunda una cu cealaltă.

Anunțurile obișnuite pentru pietoni — magazine din apropiere, treceri de pietoni și așa mai departe —
sunt reținute intenționat în timpul călătoriei, iar distanțele la care lucrurile sunt anunțate au fost
mult mărite, ca să aflați despre ceva înainte de a-l fi depășit.

### Traversarea apelor și a căilor ferate

Soundscape vă spune acum când traversați un râu, un canal, un golf, o baie sau o linie de cale ferată.
Funcționează atât pe jos, cât și în deplasare, și acoperă atât trecerea pe dedesubt, cât și pe
deasupra, astfel încât sunt descrise deopotrivă un pod pietonal și un pasaj subteran.

### Adrese și denumiri de locuri mai bune

S-a lucrat mult pentru ca Soundscape să descrie locurile așa cum ar face-o o persoană:

* Locurile fără adresă proprie sunt acum descrise prin strada și zona în care se află, în loc să rămână
  vagi.
* Numerele de casă sunt asociate părții corecte a străzii. Anterior, o adresă putea fi raportată de pe
  trotuarul opus.
* Adresa unui loc nu mai repetă numele locului însuși.
* Stațiile de autobuz din Marea Britanie folosesc denumirile oficiale din transportul public, de
  regulă cele de pe orar și de pe indicatorul stației.
* Potecile fără nume care merg de-a lungul unui râu sau canal sunt acum denumite după apa pe care o
  urmează.
* Potecile și drumurile fără nume sunt descrise mai judicios, iar cuvintele folosite pentru ele sunt
  traduse corespunzător, în loc să apară în engleză.

### Limbi

În 2.0 au fost adăugate douăzeci de limbi noi: arabă, bengaleză, bulgară, catalană, croată, cehă,
hausa, maghiară, indoneziană, coreeană, marathi, sârbă, slovacă, slovenă, swahili, tamilă, telugu,
thailandeză, urdu și vietnameză. Toate aceste limbi sunt în stadiu alfa și ne dorim mult păreri despre
acuratețea lor. În total, Soundscape este acum disponibil în 46 de limbi, iar acest site de
documentație a fost de asemenea tradus.

Araba egipteană a fost integrată în arabă, iar luganda a fost retrasă, întrucât niciuna nu avea
suficient text tradus pentru a fi utilă.

Traducerile sunt muncă a comunității și primim cu plăcere ajutorul dumneavoastră sau corecturile
acolo unde ceva se citește prost. Orice text poate fi îmbunătățit la
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Modul de repaus

Modul de repaus a primit **trezirea la plecare**. Când puneți Soundscape în repaus, îi puteți cere să
se trezească imediat ce părăsiți zona, ceea ce este util când ajungeți undeva și vreți liniște până
când porniți din nou.

### Distanțe și vorbire

Distanțele rostite au fost scurtate și făcute mai firești, iar Soundscape trece acum la unități mai
mari când vă deplasați rapid — mile sau kilometri în locul unei numărători lungi în picioare sau metri.
Fiecare limbă decide singură cum se spune o distanță fracționară, ceea ce anterior era forțat într-un
tipar de formă engleză.

### Hărți offline

Hărțile offline au apărut în 1.0 și au fost îmbunătățite constant:

* O hartă descărcată poate fi acum actualizată pe loc atunci când există o versiune mai nouă, din
  ecranul de detalii al extrasului.
* Hărțile care nu pot fi folosite — de exemplu o descărcare deteriorată — sunt acum marcate clar, în
  loc să eșueze în tăcere.
* Descărcările sunt mai fiabile, iar ecranul arată ce se întâmplă în timp ce lista hărților disponibile
  este preluată, în loc de un indicator de încărcare pe tot ecranul.
* O descărcare finalizată apare ca finalizată doar când este cu adevărat gata de utilizare.
* Pe acest site există o
  [hartă a regiunilor disponibile]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Accesibilitate

S-a lucrat foarte mult la comportamentul cititoarelor de ecran, mai ales în ecranele introductive unde
focalizarea sărea anterior în locul greșit. Alte îmbunătățiri includ citirea mai bună a dimensiunilor
fișierelor și a numerelor zecimale, indicații corecte de tipul „atingeți de două ori pentru...” în
limbile care pun verbul la final și indicații utile acolo unde nu fusese setată niciuna.

### Meniuri și navigare

* **Ieșire din Soundscape** este acum primul element din meniul principal, în loc să fie mai jos.
* Meniul principal nu mai lasă vizibilă o fâșie a ecranului într-o parte, care le oferea utilizatorilor
  de cititoare de ecran o zonă suplimentară derutantă de atins.
* Gestul de revenire al sistemului nu mai sare peste un nivel când parcurgeți categorii în Locuri din
  apropiere.
* *Tutorialul audio* a fost redenumit **tutorial ghidat**.
* Setările au fost ordonate, iar *Resetare la valorile implicite* șterge acum totul corect.

### Stabilitate

Versiunea 2.0 include o listă lungă de blocări și înghețări remediate, printre care înghețarea
aplicației pe ecranul de pornire, înghețări la resetarea setărilor, blocări în cazul unei hărți
descărcate deteriorate, blocări la deschiderea detaliilor unui traseu din ecranul principal, blocări la
schimbarea limbii, precum și mai multe probleme raportate automat prin Play Store. Comportamentul
privind bateria și pornirea a fost de asemenea făcut mai robust pe telefoanele care închid agresiv
aplicațiile din fundal.

### Funcții eliminate
{: #things-that-have-been-removed }

* **Controlul vocal** a fost eliminat. Nu a funcționat niciodată suficient de fiabil pentru a merita
  păstrat, iar butoanele media de pe căști acoperă în mare parte aceleași nevoi — vedeți
  [Ajutor privind utilizarea comenzilor media]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Meniul de limbă din aplicație** a dispărut. Soundscape urmează acum limba setată pe telefon, ceea
  ce majoritatea oamenilor se așteptau oricum. Pentru a o schimba, modificați limba telefonului sau
  setați o limbă per aplicație în setările acestuia, dacă oferă această opțiune.

## Cum ne semnalați problemele

Dacă ceva nu este în regulă, ne-ar plăcea să aflăm. Scrieți la Help Desk la
<soundscapeAndroid@scottishtecharmy.support> sau întrebați pe Slack dacă sunteți membru STA.

Dacă un anunț a fost greșit sau nu s-a produs, o înregistrare a călătoriei ne ajută enorm — o putem
reda și vedea exact cu ce date lucra Soundscape. Instrucțiunile se află la
[Furnizarea unei înregistrări de locație pentru depanare]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## O notă despre iPhone

Tot ce s-a spus mai sus privește aplicația Android, dar merită știut unde a mers restul muncii din
această versiune. Soundscape rulează acum și pe iPhone, iar ambele aplicații sunt construite din
același cod comun — aceleași ecrane, aceleași formulări și aceleași anunțuri. O noutate precum
anunțurile de călătorie de mai sus ajunge astfel la amândouă deodată, în loc să fie scrisă de două ori.
Această bază comună explică de ce 2.0 a durat atât de mult și ar trebui să facă viitoarele versiuni să
ajungă mai repede pe ambele platforme. Aplicația pentru iPhone este în prezent disponibilă prin
TestFlight, pe bază de invitație: întrebați pe Slack dacă sunteți membru STA sau scrieți la Help Desk.
