---
title: Cum funcționează Soundscape
layout: page
parent: "Utilizarea Soundscape"
has_toc: false
lang: ro
permalink: /users/how-it-works.html
machine-translated: true
---
# Cum funcționează Soundscape
Scopul acestei pagini este de a oferi o înțelegere generală a modului în care funcționează aplicația Soundscape în spatele scenei. Nu trebuie să citești acest text pentru a folosi aplicația, dar există câteva motive pentru care a fost scris:

1. Pentru a ajuta orice nou-venit interesat de aplicație să înțeleagă unde se află limitările acesteia
1. Pentru a oferi utilizatorilor o idee despre ce altceva ar mai fi posibil cu noi funcționalități
1. Pentru a oferi dezvoltatorilor o privire de ansamblu asupra funcționării aplicației

Există două tehnologii care fac aplicația posibilă: GPS-ul și datele OpenStreetMap. GPS-ul ne oferă o idee bună despre unde se află telefonul și pe unde a fost. Datele OpenStreetMap pot fi apoi folosite pentru a afla ce se află în apropiere și le putem utiliza pentru a descrie acest lucru utilizatorului.

## Balize audio
În majoritatea privințelor, acestea sunt cele mai simple lucruri de implementat din punct de vedere tehnologic. Presupunând că avem locația telefonului și o direcție pentru telefon, putem apoi modifica sunetul balizei astfel încât să pară că vine din acea direcție. Folosim o bibliotecă de la Steam Audio pentru a realiza poziționarea audio, care folosește funcții de transfer legate de cap (head related transfer functions) pentru a oferi cea mai bună poziționare sonoră posibilă. Singurul alt lucru pe care îl facem este să schimbăm sunetul balizei astfel încât să fie redat un sunet diferit în funcție de unghiul dintre direcția utilizatorului și locația balizei. Unghiurile variază în funcție de baliza selectată (Tactile, Flare, Ping etc.) și unele au un număr mai mare de sunete decât altele. Și asta sunt balizele audio la cel mai simplu nivel.

Singura complexitate suplimentară este presupunerea privind locația telefonului și direcția în care este orientat utilizatorul. Să le analizăm pe rând.

### Locația
Locația returnată de GPS poate avea o eroare destul de mare, iar aceasta depinde de cât de mult din cer este vizibil pentru GPS-ul telefonului și de câți copaci și clădiri înalte reflectă semnalul GPS pe drumul către telefon.

Abordarea pe care am adoptat-o pentru filtrarea locației este de a folosi ceea ce se numește potrivirea pe hartă (map matching). Aceasta presupune că utilizatorul se deplasează cel mai probabil de-a lungul unui drum sau a unei căi cartografiate - folosim termenul „Way" pentru a acoperi toate drumurile, potecile și cărările. Potrivirea pe hartă analizează pe unde a fost utilizatorul și, folosind direcția de mișcare împreună cu datele cartografice locale, alege cea mai probabilă locație pe un Way. Această abordare nu ține cont doar de erorile de la GPS, ci și de erorile din datele cartografice. Nu toate Way-urile sunt cartografiate cu acuratețe și, prin urmare, au și ele erori. Pentru a determina care este cel mai probabil Way pe care se află utilizatorul, algoritmul ia în considerare:
* Cât de aproape este un Way de locația GPS și de locațiile GPS anterioare
* Direcția de deplasare - se mișcă în aceeași direcție ca Way-ul
* Dacă este posibil să se ajungă de la ultima locație potrivită pe hartă la noua locație prin rețeaua de Way-uri. Acest lucru este necesar pentru a exclude trecerea între Way-uri care nu sunt de fapt conectate, de exemplu unul trece peste altul pe un pod sau pe sub el printr-un tunel.
Potrivirea pe hartă poate decide că nu există Way-uri în apropiere sau că nu are încredere pe care dintre ele se află utilizatorul, iar în acest caz pur și simplu așteaptă următoarea locație GPS și încearcă din nou până când are încredere.

### Direcția
Există mai multe direcții pe care le urmărim în software:

1. Direcția în care este orientat telefonul. O folosim atunci când telefonul este deblocat și aplicația este în uz, dar și atunci când telefonul este blocat, atâta timp cât este ținut orizontal cu ecranul îndreptat spre cer. Este util să ții cont de acest lucru când îți pui telefonul în geantă. Dacă este pus orizontal pe fundul unei genți ținute în poziție verticală, direcția aleatorie în care este orientată geanta ar fi folosită de aplicație.
1. Direcția în care se deplasează telefonul.
1. Direcția de la căști cu urmărirea mișcării capului. În prezent nu o folosim, deși aplicația iOS o suporta. Avem tehnologia pregătită pentru a o adăuga în viitor.

Când telefonul este blocat și într-o geantă, aplicația va folosi direcția de deplasare. Cu toate acestea, dacă utilizatorul nu se mișcă, atunci nu este disponibilă nicio direcție. Când se întâmplă acest lucru, balizele audio devin mai silențioase pentru a indica faptul că nu este posibil să se cunoască direcția curentă a balizei - utilizatorul s-ar putea întoarce fără a-și schimba locația.

Pentru unele utilizări ale direcției în aplicație, direcția este „aliniată" la direcția Way-ului potrivit pe hartă, deci dacă utilizatorul merge aproximativ în direcția Way-ului, atunci direcția reală a Way-ului este presupusă a fi corectă și este folosită în acele calcule.

### Concluzie
Deși la prima vedere balizele audio sunt simple, utilizarea potrivirii pe hartă pentru a încerca să eliminăm erorile de locație și de direcție introduce o complexitate considerabilă.

## Date cartografice
Datele cartografice folosite de aplicație provin aproape în întregime din proiectul OpenStreetMap. Rulăm un server care conține o hartă a întregii lumi la mai multe niveluri de zoom. Fiecare nivel de zoom este împărțit în dale (tiles). Nivelul de zoom 0 conține 1 dală, nivelul 1 conține 4 dale, nivelul 2 conține 16 dale și așa mai departe până la nivelul 14, care conține aproximativ 268 de milioane de dale pentru a acoperi planeta. Fiecare dală conține mai multe straturi, iar fiecare strat are puncte, linii și poligoane care pot fi desenate pentru a crea o hartă grafică. Acea hartă grafică este ceea ce i se afișează utilizatorului în interfața grafică a aplicației. Fiecare punct, linie și poligon are metadate care descriu ce reprezintă. Acestea provin în mare parte direct din datele OpenStreetMap, deci o linie ar putea fi un `footway` care este un `sidewalk` sau un `road` care este un `minor`

Datele sunt transformate în harta grafică printr-un „stil" care are reguli privind modul de desenare a diferitelor puncte, linii și poligoane din fiecare strat, de exemplu cum se desenează o potecă, cum se desenează o pădure, cum se desenează o stație de autobuz. Regulile pot varia în funcție de nivelul de zoom, motiv pentru care, pe măsură ce mărești zoom-ul, devin vizibile tot mai multe puncte și linii care nu sunt vizibile când zoom-ul este micșorat, de exemplu stații de autobuz și poteci.

Modificând stilul putem schimba aspectul hărții din interfață, de unde provine și „harta accesibilă" pe care o testăm. Aceasta urmărește să aibă un contrast mai mare și linii și texte mai îndrăznețe. Stilul este integrat în aplicație, astfel încât nu trebuie să schimbăm harta de pe server pentru a-i schimba aspectul.

Dar cum folosim datele cartografice pentru audio?

### Folosirea datelor cartografice pentru audio
În prezent folosim o cantitate relativ mică din datele cartografice pentru a genera interfața audio cu utilizatorul. Aproape întreaga interfață audio folosește doar dalele de la nivelul maxim de zoom. Aplicația asamblează o grilă de 2 pe 2 dale în jurul locului în care se află utilizatorul și apoi analizează doar câteva dintre straturi:

* `transportation` - pentru toate tipurile de Way-uri, inclusiv drumuri, poteci, căi ferate și linii de tramvai.
* `poi` - puncte de interes, de exemplu magazine, centre sportive, bănci, cutii poștale, stații de autobuz etc.
* `building` - acesta este pentru `poi` care sunt cartografiate ca fiind mai mult decât un simplu punct, de exemplu supermarketuri mari sau primării.

Acesta unește liniile și poligoanele peste granițele dalelor și transformă toate Way-urile în segmente de Way conectate și intersecții. Acest lucru este important deoarece ne permite să căutăm de-a lungul unui Way pentru a afla unde putem ajunge.

Toate datele analizate sunt puse, de asemenea, într-un format ușor de căutat, astfel încât aplicația să poată găsi cu ușurință ce caracteristici ale hărții se află în apropiere. În acest moment, datele sunt clasificate în categorii. Categoriile actuale sunt:

* Drumuri
* Drumuri și poteci (toate Way-urile)
* Intersecții - punctele în care se intersectează Way-urile
* Intrări - acestea sunt puncte pe o clădire care au fost marcate ca intrare.
* Treceri - treceri de drum
* POI-uri - toate punctele de interes
* Stații de transport - stații de autobuz, gări, stații de tramvai și așa mai departe.
* Subcategorii ale POI-urilor:
  * Informații
  * Obiect
  * Loc
  * Reper
  * Mobilitate
  * Siguranță
* Așezări și subcategorii (vezi secțiunea următoare)
  * Orașe mari
  * Orașe
  * Sate
  * Cătune

 Cu acestea în loc, pentru orice locație aplicația poate găsi apoi cu ușurință

 * „Toate stațiile de transport pe o rază de 50 m" sau
 * „Cea mai apropiată intersecție din fața mea" sau
 * „Cel mai apropiat cătun, sat, oraș sau oraș mare"

Cu acestea în loc, crearea anunțurilor audio este doar o chestiune de interogare a datelor pe baza locației și direcției curente. Pe măsură ce utilizatorul se deplasează pe o grilă de dale, aceasta se actualizează astfel încât să se centreze în jurul locației curente.

### Mai multe date
Una dintre problemele grilei noastre de date cartografice foarte locale este că înseamnă că putem „vedea" cel mult aproximativ 1 km în orice direcție. Acest lucru este în regulă atunci când descriem ce se află în fața noastră, dar uneori am dori să oferim mai mult context. Principalul exemplu este atunci când aplicația este folosită și utilizatorul nu merge pe jos.

Când aplicația detectează că utilizatorul se deplasează cu mai mult de 5 metri pe secundă, schimbă modul în care descrie lumea. În loc să anunțe fiecare intersecție și POI, anunță mai rar și doar drumurile din apropiere. Problema cu acest lucru este că a cunoaște numele unui drum nu este foarte util dacă nu știi în ce oraș se află.

Pentru a încerca să rezolvăm acest lucru, acum analizăm și datele cartografice la un nivel de zoom mai mic și extragem date din stratul `place`. Acesta conține numele orașelor, marilor orașe, cartierelor, satelor și așa mai departe. O problemă cu cartografierea lucrurilor este că nu există întotdeauna o graniță evidentă între aceste locuri. OpenStreetMap are uneori granițele orașelor în baza sa de date, dar chiar și când este cazul, până ajunge la harta noastră în dale, acea informație este adesea pierdută. Ce avem este locația în care numele locurilor sunt desenate pe hartă. Acestea sunt clasificate, iar apoi aplicația va găsi cel mai apropiat cătun, sat, oraș sau oraș mare de utilizator și va raporta acest lucru.

Pentru multe orașe mari, numele real al orașului nu va fi anunțat niciodată, deoarece majoritatea orașelor mari sunt împărțite în diviziuni mai mici, precum cartierele, dar acestea oferă context suplimentar și sunt foarte utile. Reține doar că, deoarece aplicația raportează că te afli în apropierea unei străzi dintr-un anumit cartier, asta înseamnă doar că eticheta pentru acel cartier este cel mai apropiat punct și ar putea fi incorectă sau chiar de cealaltă parte a unui râu.

### Mai mult context
Cu cât se poate adăuga mai mult context în descrieri, cu atât mai bine, atâta timp cât rămâne concis și previzibil. Una dintre problemele pe care le-am observat la descrierea intersecțiilor este că adesea erau implicate Way-uri „fără nume". Acestea sunt Way-uri care nu au nume. În datele cartografice acestea ar putea fi doar o cărare, o potecă sau un drum de serviciu, dar fără mai mult context nu este foarte util în descrierile text. Din fericire, putem face mai bine, așa că ceea ce face aplicația este că ori de câte ori urmează să anunțe un Way fără nume, verifică dacă poate găsi un context suplimentar pentru acesta.

* **Este un trotuar?**
Multe zone din OpenStreetMap au acum trotuarele cartografiate separat de drumuri. Acestea sunt de obicei etichetate ca `sidewalk`, dar de obicei nu indică care este drumul căruia îi aparțin ca trotuar.

    Când aplicația întâlnește un trotuar fără nume, caută un drum despre care crede că merge alături de el și îl folosește pentru a denumi trotuarul. Acest lucru se dovedește a fi foarte important pentru anunțurile noastre. În loc să anunțăm fiecare intersecție de trotuar, pe măsură ce ne deplasăm de-a lungul unui trotuar, anunțurile sunt făcute ca și cum ne-am deplasa de-a lungul drumului asociat. În loc de *„Deplasare spre vest pe potecă"* avem *„Deplasare spre vest pe Moor Road"*. Utilizatorul se află pe trotuarul cartografiat, dar descrierea are mai mult sens.

* **Se termină la un Way denumit?**
Foarte des există poteci pietonale care unesc două drumuri. Privind ambele capete ale potecii putem adăuga cu ușurință acel context, astfel încât într-o direcție ar putea fi *„Potecă spre Moor Road"*, iar apropiindu-ne din celălalt capăt ar putea fi *„Potecă spre Roselea Drive"*. Acest lucru se face doar acolo unde poteca nu se ramifică; dacă se ramifică în două poteci fără nume, atunci nu încercăm să adăugăm acest context.

* **Se termină lângă un Marcaj?**
Dacă un Way fără nume începe sau se termină lângă un Marcaj, acesta este folosit pentru a-l descrie, de exemplu *„Potecă spre joncțiunea cu copacul mare"*. Utilizatorul poate adăuga Marcaje oriunde dorește, iar adăugând Marcaje de-a lungul rețelelor de poteci poate adăuga context unei rute întregi.

* **Intră sau iese dintr-un POI?**
Dacă un Way fără nume începe în afara unui POI și se termină în interiorul acestuia (sau invers), atunci putem adăuga acel context, de exemplu *„Cărare spre Lennox Park"*.

* **Se termină lângă o Intrare?**
Dacă un Way fără nume începe sau se termină mai aproape de o Intrare, atunci putem adăuga acel context, de exemplu *„Drum de serviciu spre Best Buy"*.

* **Se termină lângă un Reper sau un Loc?**
Dacă un Way fără nume începe sau se termină mai aproape de un Reper, atunci putem adăuga și acel context, de exemplu *„Drum de serviciu spre Catedrala St. Giles"*.

* **Este o fundătură?**
Aplicația marchează ca fundătură orice Way fără nume care nu duce nicăieri.

* **Trece pe lângă vreo scară?**
Dacă Way-ul fără nume trece peste un pod, printr-un tunel sau urcă/coboară scări, atunci acest lucru este notat și adăugat la context. Acest lucru este separat de etichetarea destinației, deci un context precum *„Potecă peste pod spre Lennox Park"* este posibil.

Aceste contexte sunt adăugate în ordine și astfel este posibil să avem *„Potecă spre Park Lane prin scări"* într-o direcție și *„Potecă spre Lennox Park prin scări"* în cealaltă direcție. Strada denumită are prioritate la ieșirea din parc, dar parcul este folosit la intrarea în el.

#### Context viitor
Există diverse contexte suplimentare pe care sperăm să le adăugăm în viitor, inclusiv:
* Context pentru Way-uri care urmează caracteristici acvatice liniare, de exemplu *„Potecă lângă râul Dee"*
* Context pentru Way-uri care urmează marginea corpurilor de apă *„Potecă lângă rezervorul Milngavie"*
* Context pentru Way-uri care urmează căile ferate, de exemplu *„Potecă lângă calea ferată"*. Acesta ar putea include chiar și numele liniei de cale ferată
* Adăugarea de conținut suplimentar la poduri și tuneluri, peste sau pe sub ce se află, de exemplu *„Potecă prin pod peste calea ferată spre Moor Road"*

## Anunțuri audio
Acum că avem datele cartografice într-un format pe care îl putem folosi cu ușurință, generarea anunțurilor este într-adevăr destul de simplă.

### Anunțuri la mers pe jos
La mersul pe jos, anunțurile audio care pot avea loc sunt (în ordinea priorității):

1. Descrierea distanței până la destinația curentă
1. Descrierea unei intersecții care urmează
1. Descrierea celor mai apropiate 5 puncte de interes

Toate anunțurile au o limită de frecvență, astfel încât să nu se repete prea des. Dacă utilizatorul se oprește din mers, atunci anunțurile se vor opri, iar chiar și în mișcare un anunț nu se va repeta la fiecare nouă locație GPS. Frecvența, ca în aplicația iOS, este:

* La fiecare 60 de secunde pentru destinația curentă
* La fiecare 30 de secunde pentru o intersecție care urmează
* La fiecare 60 de secunde pentru un punct de interes

Anunțurile pot fi filtrate prin meniul de setări și există cu siguranță posibilitatea de a extinde acest comportament.

### Anunțuri la deplasarea mai rapidă
La deplasarea cu mai mult de 5 metri pe secundă, anunțul către destinația curentă încă are loc, dar anunțurile despre intersecții și puncte de interes sunt înlocuite cu un anunț care descrie aproximativ unde se află utilizatorul. Acesta oferă o stație de transport din apropiere, un punct de interes care ne conține, de exemplu în interiorul unui parc mare, sau un drum și o așezare din apropiere. Acestea folosesc datele descrise mai devreme și există loc evident pentru a permite personalizarea acestui lucru în viitor.

## Marcaje și rute
În cea mai mare parte, marcajele și rutele sunt doar o funcționalitate a interfeței cu utilizatorul care nu se bazează nici pe GPS, nici, de fapt, pe datele cartografice. Marcajele sunt locații denumite pe care utilizatorul dorește să le stocheze, iar rutele sunt o listă ordonată a acelor marcaje. Interfața cu utilizatorul pentru a le crea pe ambele este preluată direct din versiunea iOS.

### Redarea rutei
Redarea rutei este momentul în care rutele prind viață. Când o rută este redată, se creează o baliză audio la primul marcaj din rută. Odată ce utilizatorul se apropie de acel marcaj, ruta mută automat baliza audio la următorul marcaj din rută. Dacă nu mai există alte marcaje, atunci redarea rutei se încheie.

## Concluzie
Sperăm că acest lucru a oferit o oarecare perspectivă asupra modului în care funcționează aplicația. Aplicația se dezvoltă mereu pe baza feedback-ului de la utilizatori, așa că te rugăm să ne contactezi dacă există ceva ce crezi că ar putea fi adăugat.
