---
title: Note di rilascio
layout: page
nav_order: 5
has_toc: false
lang: it
permalink: /release-notes.html
machine-translated: true
---

# Note di rilascio

Soundscape 2.0 è una versione importante ed è attualmente in beta chiusa. La novità principale è che
ora Soundscape ha qualcosa di utile da dire anche quando viaggi in auto, in autobus o in treno, e
non soltanto quando cammini. Ci sono inoltre molti interventi più piccoli su come vengono descritti
i luoghi, venti nuove lingue e un lungo elenco di correzioni.

Le note delle versioni precedenti si trovano nella pagina
[Note di rilascio per la 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novità della versione 2.0

* **Annunci durante i viaggi in auto, autobus o treno.** Soundscape riconosce quando ti muovi a
  velocità sostenuta e descrive il viaggio anziché ciò che ti circonda immediatamente.
* **Avvisi quando attraversi corsi d'acqua e ferrovie.** Fiumi, canali, insenature e linee
  ferroviarie vengono annunciati mentre li attraversi, sia a piedi sia in viaggio.
* **Indirizzi e nomi di luoghi migliori.** I luoghi privi di un indirizzo proprio ricevono ora la
  via e la zona in cui si trovano, i numeri civici vengono associati al lato corretto della strada e
  le fermate degli autobus in Gran Bretagna usano i nomi ufficiali.
* **Venti nuove lingue**, per un totale di 46. Anche questo sito di documentazione è tradotto.
* **Risveglio all'uscita.** La modalità di sospensione può ora risvegliare Soundscape quando lasci
  il luogo in cui l'hai messo a riposo.
* **Distanze più brevi e naturali**, con unità più grandi quando ti muovi velocemente.
* **Un'uscita più rapida.** *Esci da Soundscape* è ora in cima al menu principale.
* **Miglioramenti alle mappe offline**, tra cui l'aggiornamento di una mappa già scaricata e una
  mappa delle regioni disponibili su questo sito.
* **Molto lavoro sull'accessibilità** con TalkBack, in particolare nelle schermate introduttive.
* **Moltissime correzioni di arresti anomali e stabilità.**

Nella versione 2.0 sono state **rimosse** due cose: il controllo vocale e il menu della lingua
all'interno dell'app. Vedi [Elementi rimossi](#things-that-have-been-removed) più avanti per sapere
cosa fare invece.

---

## Più in dettaglio

### Viaggiare in auto, autobus o treno

È la novità più grande per chi già usa l'app. In precedenza Soundscape aveva ben poco da dire una
volta saliti su un veicolo: continuava a descrivere l'ambiente immediato, il che a velocità
sostenuta si traduceva in un flusso di cose ormai superate.

Ora Soundscape si accorge che ti stai muovendo più velocemente del passo d'uomo e cambia ciò che ti
comunica. Non c'è nulla da attivare e tutto torna alla normalità da solo non appena rallenti o scendi
e prosegui a piedi.

Durante il viaggio sentirai:

* **Dove ti trovi**, di tanto in tanto: la strada su cui sei e la direzione di marcia, per esempio
  «In viaggio verso nord lungo la M8». Le strade con un numero vengono annunciate con il loro numero
  e Soundscape non riannuncia la stessa strada a ogni cambio di toponimo.
* **Città e paesi** verso cui ti dirigi, con la distanza, oltre a quelli da cui ti allontani o
  davanti a cui semplicemente passi.
* **Svincoli e uscite autostradali** quando li raggiungi.
* **Grandi punti di riferimento** mentre li superi, come parchi, ospedali, stadi e centri
  commerciali.
* **Fermate di autobus, tram e treno** mentre le superi. Soundscape cita solo le fermate sul tuo
  lato della strada, poiché quelle sul lato opposto servono la direzione contraria.
* **Fiumi, canali e ferrovie che attraversi.**
* **Gallerie**, il che spiega soprattutto perché Soundscape sta per ammutolire: al loro interno non
  c'è segnale GPS.

In **treno** Soundscape capisce che ti trovi su una ferrovia e non su una strada, e ti indica le
località che stai superando e quanta strada hai percorso dall'ultima stazione. Capirlo è più
difficile di quanto sembri, perché autostrade e linee ferroviarie corrono spesso affiancate per
chilometri: buona parte del lavoro di questa versione è servita proprio a non scambiare l'una per
l'altra.

Gli annunci ordinari per chi cammina (negozi vicini, attraversamenti stradali e così via) vengono
volutamente trattenuti durante il viaggio, e le distanze alle quali le cose vengono annunciate sono
molto ampliate, così da avvisarti prima che tu le abbia superate.

### Attraversare corsi d'acqua e ferrovie

Soundscape ora ti avvisa quando attraversi un fiume, un canale, un'insenatura, una baia o una linea
ferroviaria. Funziona sia a piedi sia in viaggio e comprende tanto il passaggio sotto quanto quello
sopra, così vengono descritti sia una passerella sia un sottopasso.

### Indirizzi e nomi di luoghi migliori

Molto lavoro è stato dedicato a far sì che Soundscape descriva i luoghi come farebbe una persona:

* I luoghi privi di un indirizzo proprio vengono ora descritti tramite la via e la zona in cui si
  trovano, anziché restare vaghi.
* I numeri civici vengono associati al lato corretto della strada. In precedenza un indirizzo poteva
  essere segnalato dal marciapiede opposto.
* L'indirizzo di un luogo non ripete più il nome del luogo stesso.
* Le fermate degli autobus in Gran Bretagna usano i nomi ufficiali del trasporto pubblico, di norma
  quelli che compaiono sugli orari e sul palo della fermata.
* I sentieri senza nome che costeggiano un fiume o un canale prendono ora il nome del corso d'acqua
  che seguono.
* Sentieri e strade senza nome sono descritti in modo più sensato e i termini usati sono tradotti
  correttamente anziché comparire in inglese.

### Lingue

Nella versione 2.0 sono state aggiunte venti nuove lingue: arabo, bengalese, bulgaro, catalano,
croato, ceco, hausa, ungherese, indonesiano, coreano, marathi, serbo, slovacco, sloveno, swahili,
tamil, telugu, thai, urdu e vietnamita. Queste lingue sono tutte in fase alfa e ci interessa molto
ricevere riscontri sulla loro accuratezza. In totale Soundscape è ora disponibile in 46 lingue e
anche questo sito di documentazione è stato tradotto.

L'arabo egiziano è confluito nell'arabo e il luganda è stato ritirato, poiché nessuno dei due aveva
testo tradotto sufficiente per essere utile.

Le traduzioni sono un lavoro collettivo e accogliamo volentieri il tuo aiuto, o le tue correzioni
quando qualcosa si legge male. Ogni stringa può essere migliorata su
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Modalità di sospensione

La modalità di sospensione ha guadagnato il **risveglio all'uscita**. Quando metti Soundscape a
riposo puoi chiedergli di risvegliarsi non appena lasci la zona: utile quando arrivi da qualche
parte e vuoi silenzio fino alla prossima partenza.

### Distanze e voce

Le distanze pronunciate sono state accorciate e rese più naturali, e Soundscape passa ora a unità
più grandi quando ti muovi velocemente: miglia o chilometri anziché un lungo conteggio in piedi o
metri. Ogni lingua decide da sé come esprimere una distanza frazionaria, cosa che prima era forzata
in uno schema di stampo inglese.

### Mappe offline

Le mappe offline sono arrivate con la 1.0 e sono state costantemente migliorate:

* Una mappa scaricata può ora essere aggiornata sul posto quando è disponibile una versione più
  recente, dalla schermata dei dettagli dell'estratto.
* Le mappe inutilizzabili, per esempio un download danneggiato, vengono ora chiaramente segnalate
  anziché fallire in silenzio.
* I download sono più affidabili e la schermata mostra cosa sta accadendo mentre viene recuperato
  l'elenco delle mappe disponibili, anziché un indicatore di caricamento a schermo intero.
* Un download completato compare come completato solo quando è davvero pronto all'uso.
* Su questo sito è disponibile una
  [mappa delle regioni disponibili]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Accessibilità

È stato svolto moltissimo lavoro sul comportamento degli screen reader, soprattutto nelle schermate
introduttive, dove in precedenza il focus finiva nel punto sbagliato. Tra gli altri miglioramenti:
una lettura migliore di dimensioni dei file e numeri decimali, suggerimenti corretti del tipo «tocca
due volte per...» nelle lingue che pongono il verbo in fondo e indicazioni sensate dove non ne era
stata impostata alcuna.

### Menu e navigazione

* **Esci da Soundscape** è ora la prima voce del menu principale anziché trovarsi più in basso.
* Il menu principale non lascia più visibile una striscia di schermo su un lato, che offriva a chi
  usa uno screen reader un'ulteriore area su cui toccare, fonte di confusione.
* Il gesto di ritorno di sistema non salta più un livello mentre sfogli le categorie in Luoghi nelle
  vicinanze.
* Il *tutorial audio* è stato rinominato **tutorial guidato**.
* Le impostazioni sono state riordinate e *Ripristina i valori predefiniti* ora cancella davvero
  tutto.

### Stabilità

La versione 2.0 comprende un lungo elenco di correzioni di arresti anomali e blocchi, tra cui il
blocco dell'app sulla schermata iniziale, i blocchi al ripristino delle impostazioni, gli arresti
con una mappa scaricata danneggiata, gli arresti all'apertura dei dettagli di un percorso dalla
schermata principale, gli arresti al cambio di lingua e diversi problemi segnalati automaticamente
tramite il Play Store. Anche il comportamento all'avvio e rispetto alla batteria è stato reso più
robusto sui telefoni che chiudono in modo aggressivo le app in background.

### Elementi rimossi
{: #things-that-have-been-removed }

* **Il controllo vocale** è stato rimosso. Non ha mai funzionato in modo abbastanza affidabile da
  giustificarne il mantenimento e i tasti multimediali delle cuffie coprono in gran parte le stesse
  esigenze: vedi
  [Guida all'uso dei comandi multimediali]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Il menu della lingua nell'app** è scomparso. Soundscape segue ora la lingua impostata sul
  telefono, come la maggior parte delle persone si aspettava. Per cambiarla, modifica la lingua del
  telefono oppure imposta una lingua per singola app nelle impostazioni, se il telefono lo consente.

## Segnalarci un problema

Se qualcosa non va, ci farebbe piacere saperlo. Scrivi all'Help Desk all'indirizzo
<soundscapeAndroid@scottishtecharmy.support>, oppure chiedi su Slack se sei membro della STA.

Se un annuncio è stato sbagliato o non è arrivato, una registrazione del tuo viaggio ci aiuta
moltissimo: possiamo riprodurla e vedere esattamente su quali dati stava lavorando Soundscape. Le
istruzioni si trovano in
[Fornire una registrazione della posizione per il debug]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Una nota sull'iPhone

Tutto quanto precede riguarda l'app Android, ma vale la pena sapere dove è finito il resto del
lavoro di questa versione. Soundscape ora funziona anche su iPhone ed entrambe le app sono costruite
a partire dallo stesso codice condiviso: le stesse schermate, le stesse formulazioni e gli stessi
annunci. Una novità come gli annunci di viaggio descritti sopra arriva così su entrambe
contemporaneamente, anziché essere scritta due volte. Questa base comune spiega perché la 2.0 ha
richiesto tanto tempo ed è ciò che dovrebbe far arrivare più rapidamente le prossime versioni su
entrambe le piattaforme. L'app per iPhone è attualmente disponibile tramite TestFlight su invito:
chiedi su Slack se sei membro della STA, oppure scrivi all'Help Desk.
