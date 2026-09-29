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
* **Scegli quanto ti dice Soundscape.** La nuova impostazione *Dettaglio delle notifiche* rende
  Soundscape più discreto nei luoghi affollati, e *Luoghi da annunciare* ti permette di scegliere di
  quali tipi di luogo sentire parlare. Puoi cambiare il dettaglio con i pulsanti delle cuffie mentre
  cammini.
* **Sapere quanto dista il prossimo incrocio.** Gli incroci vengono annunciati a una distanza
  costante mentre ti avvicini, e la notifica ora dice quanto dista il bordo del marciapiede.
* **Cercare un tipo di luogo, o delle coordinate.** Cerca «farmacia» o «fermata dell'autobus» per
  trovare le più vicine, comunque si chiamino, oppure incolla delle coordinate, un link di una mappa
  o un Plus Code.
* **Aprire un luogo in un'altra app di mappe**, come Google Maps, dai dettagli della posizione o dagli
  elenchi.
* **Più informazioni dall'audiofaro nella schermata principale.** Ora mostra distanza e direzione, e
  ha azioni per lo screen reader per annunciare l'audiofaro, saperne di più o salvarlo come
  indicatore.
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

### Incroci

La domanda più frequente sulle notifiche degli incroci era a che distanza si trovi davvero
l'incrocio. Ora Soundscape te lo dice: «Incrocio a 30 metri». La distanza è misurata fino al bordo
del marciapiede della strada che stai per attraversare, non fino al centro dell'incrocio, perché è lì
che ti fermi davvero.

Anche il momento della notifica è più costante. Prima poteva arrivare a 45 metri o a 10 metri, senza
nulla che permettesse di distinguerli. Ora aspetta che l'incrocio sia a circa 30 metri, così la
distanza significa più o meno la stessa cosa ogni volta.

### Attraversare corsi d'acqua e ferrovie

Soundscape ora ti avvisa quando attraversi un fiume, un canale, un'insenatura, una baia o una linea
ferroviaria. Funziona sia a piedi sia in viaggio e comprende tanto il passaggio sotto quanto quello
sopra, così vengono descritti sia una passerella sia un sottopasso.

### Scegliere quanto ti dice Soundscape

La cosa che ci sentiamo dire più spesso su Soundscape è che parla troppo nei luoghi affollati come un
centro città. La sezione *Gestisci notifiche* delle *Impostazioni* ha ora tre impostazioni al posto
della vecchia lista di interruttori:

* **Dettaglio delle notifiche** può essere Silenzioso, Discreto, Bilanciato o Dettagliato.
  *Dettagliato* è quello che Soundscape ha sempre fatto, ed è il punto di partenza. *Bilanciato*
  tralascia i sentieri minori e le strade di servizio e si ripete meno spesso. *Discreto* annuncia
  solo strade, incroci e punti di riferimento. *Silenzioso* non fa nessuna notifica automatica, mentre
  audiofari, percorsi e pulsanti della schermata principale continuano a funzionare. Sostituisce il
  vecchio interruttore *Consenti notifiche*: se lo avevi disattivato, troverai il Dettaglio delle
  notifiche impostato su Silenzioso.
* **Strade e incroci** attiva o disattiva le notifiche sugli incroci e sulla strada in cui ti trovi.
* **Luoghi da annunciare** è un elenco da spuntare: Tutto, Punti di riferimento, Trasporto pubblico,
  Cibi e bevande, Generi alimentari e minimarket, Banche e sportelli bancomat oppure Nessun luogo.
  Spuntane quanti vuoi, per esempio punti di riferimento e fermate dell'autobus. I tuoi indicatori
  vengono sempre annunciati.

Il dettaglio giusto cambia mentre cammini, quindi non devi entrare nelle Impostazioni per
modificarlo. Premendo *Indietro* sulle cuffie il Dettaglio delle notifiche scende di un livello alla
volta, da Dettagliato a Bilanciato, Discreto e Silenzioso, e poi torna a Dettagliato. Il nuovo
livello viene detto ogni volta. Funziona in entrambe le modalità dei controlli di riproduzione, e
per questo i pulsanti delle cuffie sono cambiati un po':

* In *Modalità originale*, *Avanti* ora annuncia *Intorno a me* quando non c'è nessun percorso in
  corso, e *La mia posizione* non è più sui pulsanti. Durante un percorso, *Avanti* e *Indietro*
  continuano a passare da un waypoint all'altro.
* In modalità *Menu audio*, *Indietro* non torna più indietro nel menu. *Avanti* continua a
  scorrerlo e *Riproduci/Pausa* continua a selezionare. Indicatori e percorsi nel menu sono ora
  elencati per nome, e dopo averne avviato uno il menu torna all'inizio invece di lasciarti in fondo
  all'elenco.

### Ricerca

La barra di ricerca ora capisce più dei soli nomi di luoghi:

* **Tipi di luogo.** Cerca «farmacia», «bagno», «bancomat» e così via, nella tua lingua, e Soundscape
  elenca i luoghi più vicini di quel tipo, comunque si chiamino. I luoghi senza nome, come la maggior
  parte dei bagni e delle panchine, sono elencati per ciò che sono, con il loro indirizzo.
* **Coordinate, link di mappe e Plus Code.** Incolla una coppia di numeri, gradi e minuti, un link di
  Google Maps, Apple Mappe o OpenStreetMap, oppure un Plus Code, e Soundscape ti dà quel punto
  esatto. Una semplice coppia di numeri si può leggere nei due sensi, quindi quando entrambi hanno
  senso ti vengono proposti tutti e due, il più vicino per primo.
* **Ricerca offline.** La ricerca ora guarda sempre anche nelle mappe scaricate oltre che online,
  e trova così molti più luoghi senza nome. Se cerchi senza connessione a internet e non hai una
  mappa offline del luogo in cui ti trovi, Soundscape te lo dice invece di non trovare semplicemente
  nulla.

### Aprire un luogo in un'altra app

I dettagli della posizione hanno un nuovo pulsante, **Apri in un'app di mappe**, che elenca le app di
mappe e di navigazione del telefono. Spunta *Usa sempre questa app* e il pulsante diventa, per
esempio, *Apri in Google Maps*, e la apre subito; una pressione prolungata fa tornare l'elenco. Gli
elenchi *Luoghi nelle vicinanze* e *Indicatori* hanno anche le azioni per lo screen reader *Apri in…*
e *Condividi*, accanto ad *Avvia audiofaro*.

### L'audiofaro e gli indicatori

* L'audiofaro nella schermata principale ora mostra **distanza e direzione**, e uno screen reader lo
  legge per esempio come «Audiofaro su Milngavie Library, 390 metri, sud est». I percorsi mostrano
  allo stesso modo la distanza dal waypoint attuale.
* L'audiofaro ha tre **azioni per lo screen reader**: *Annuncia audiofaro* dice dove si trova,
  *Ulteriori informazioni* aggiunge l'indirizzo e *Aggiungi a indicatori* lo salva.
* Puoi di nuovo **spostare un indicatore** trascinando la mappa dalla schermata *Modifica
  indicatore*.

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
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

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
  Soundscape è anche pronto per i comandi vocali con Gemini su Android 16 e versioni successive, ma
  non funzioneranno finché Google non ne rilascerà il supporto in Gemini.
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
