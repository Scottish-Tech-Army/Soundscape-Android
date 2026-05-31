---
title: Come funziona Soundscape
layout: page
parent: "Usare Soundscape"
has_toc: false
lang: it
permalink: /users/how-it-works.html
machine-translated: true
---
# Come funziona Soundscape
Lo scopo di questa pagina è fornire una comprensione generale di come funziona l'app Soundscape sotto il cofano. Non è necessario leggerla per usare l'app, ma ci sono alcuni motivi per cui è stata scritta:

1. Per aiutare i nuovi arrivati interessati all'app a capire dove si trovano i suoi limiti
1. Per dare agli utenti un'idea di cos'altro potrebbe essere possibile con nuove funzionalità
1. Per dare agli sviluppatori una panoramica del funzionamento dell'app

Ci sono due tecnologie che rendono possibile l'app: il GPS e i dati di OpenStreetMap. Il GPS ci dà una buona idea di dove si trova il telefono e di dov'è stato. I dati di OpenStreetMap possono poi essere utilizzati per trovare cosa c'è nelle vicinanze e possiamo usarli per descriverlo all'utente.

## Audiofari
In molti modi questi sono gli elementi più semplici da implementare dal punto di vista tecnologico. Supponendo di avere la posizione del telefono e una direzione del telefono, possiamo poi modificare l'audio dell'audiofaro in modo che sembri provenire da quella direzione. Utilizziamo una libreria di Steam Audio per eseguire il posizionamento audio, che usa le funzioni di trasferimento relative alla testa (head related transfer functions) per ottenere il posizionamento dal suono migliore possibile. L'unica altra cosa che facciamo è modificare l'audio dell'audiofaro in modo che venga riprodotto un suono diverso a seconda dell'angolo tra la direzione dell'utente e la posizione dell'audiofaro. Gli angoli variano a seconda dell'audiofaro selezionato (Tattile, Bagliore, Tintinnio ecc.) e alcuni hanno un numero maggiore di suoni rispetto ad altri. E questi sono gli audiofari al livello più semplice.

L'unica complessità aggiuntiva è l'assunzione sulla posizione del telefono e sulla direzione verso cui punta l'utente. Esaminiamole una alla volta.

### Posizione
La posizione restituita dal GPS può avere un errore piuttosto grande, e questo dipende da quanta parte del cielo è visibile al GPS del telefono e da quanti alberi ed edifici alti riflettono il segnale GPS lungo il percorso verso il telefono.

L'approccio che abbiamo adottato per filtrare la posizione è usare ciò che è noto come map matching (corrispondenza con la mappa). Questo presuppone che l'utente sia molto probabilmente in viaggio lungo un percorso o una strada mappata - usiamo il termine 'Way' (via) per indicare tutte le strade, le piste e i sentieri. Il map matching osserva dov'è stato l'utente e, usando la direzione di movimento insieme ai dati di mappatura locali, sceglie la posizione più probabile su una Way. Questo approccio non solo tiene conto degli errori del GPS, ma anche degli errori nei dati della mappa. Non tutte le Way sono mappate accuratamente e quindi anche queste contengono errori. Per determinare quale sia la Way più probabile su cui si trova l'utente, l'algoritmo considera:
* Quanto è vicina una Way alla posizione GPS e alle precedenti posizioni GPS
* La direzione di marcia - si sta muovendo nella stessa direzione della Way
* Se è possibile arrivare dall'ultima posizione corrispondente alla mappa alla nuova posizione attraverso la rete di Way. Questo è necessario per escludere il passaggio tra Way che non sono effettivamente collegate, ad esempio una passa sopra un'altra tramite un ponte, o sotto tramite un tunnel.
Il map matching potrebbe decidere che non ci sono Way nelle vicinanze, oppure non essere certo su quale si trovi l'utente, e in questo caso si limita ad attendere la posizione GPS successiva e riprova finché non è sicuro.

### Direzione
Ci sono diverse direzioni che tracciamo nel software:

1. La direzione verso cui punta il telefono. La usiamo quando il telefono è sbloccato e l'app è in uso, ma anche quando il telefono è bloccato purché sia tenuto piatto con lo schermo rivolto verso il cielo. È utile tenerlo presente quando metti il telefono in borsa. Se è messo piatto sul fondo di una borsa tenuta in verticale, l'app userebbe la direzione casuale verso cui punta la tua borsa.
1. La direzione in cui si sta muovendo il telefono.
1. La direzione dalle cuffie con head tracking. Al momento non la usiamo, anche se l'app iOS la supportava. Abbiamo la tecnologia pronta per aggiungerla in futuro.

Quando il telefono è bloccato e in una borsa, l'app userà la direzione di marcia. Tuttavia, se l'utente non si sta muovendo, nessuna direzione è disponibile. Quando questo accade, gli audiofari diventano più silenziosi per indicare che non è possibile conoscere la direzione attuale dell'audiofaro - l'utente potrebbe girarsi senza cambiare posizione.

Per alcuni usi della direzione nell'app, la direzione viene 'agganciata' alla direzione della Way corrispondente alla mappa, quindi se l'utente sta camminando all'incirca nella direzione della Way, si presume che la direzione effettiva della Way sia corretta e viene usata in quei calcoli.

### Conclusione
Anche se a prima vista gli audiofari sono semplici, l'uso del map matching per cercare di rimuovere gli errori di posizione e direzione introduce una discreta quantità di complessità.

## Dati della mappa
I dati della mappa usati dall'app provengono quasi tutti dal progetto OpenStreetMap. Gestiamo un server che contiene una mappa dell'intero mondo a più livelli di zoom. Ogni livello di zoom è suddiviso in tile (riquadri). Il livello di zoom 0 contiene 1 tile, il livello 1 ne contiene 4, il livello 2 ne contiene 16 e così via fino al livello 14 che contiene circa 268 milioni di tile per coprire il pianeta. Ogni tile contiene più livelli e ogni livello ha punti, linee e poligoni che possono essere disegnati per realizzare una mappa grafica. Quella mappa grafica è ciò che viene mostrato all'utente nella GUI dell'app. Ogni punto, linea e poligono ha metadati che descrivono di cosa si tratta. Questi provengono per lo più direttamente dai dati di OpenStreetMap, quindi una linea potrebbe essere un `footway` che è un `sidewalk` oppure una `road` che è una `minor`

I dati vengono trasformati nella mappa grafica tramite uno 'stile' che ha regole su come disegnare i diversi punti, linee e poligoni in ciascun livello, ad esempio come disegnare un sentiero, come disegnare una foresta, come disegnare una fermata dell'autobus. Le regole possono variare a seconda del livello di zoom, ecco perché man mano che ingrandisci diventano visibili sempre più punti e linee che non sono visibili quando si rimpicciolisce, ad esempio fermate dell'autobus e sentieri.

Modificando lo stile possiamo cambiare l'aspetto della mappa nella GUI, ed è da qui che proviene la 'mappa accessibile' che stiamo sperimentando. Mira ad avere un maggiore contrasto e linee e testi più marcati. Lo stile è integrato nell'app, quindi non dobbiamo modificare la mappa sul server per cambiarne l'aspetto.

Ma come usiamo i dati della mappa per l'audio?

### Usare i dati della mappa per l'audio
Attualmente usiamo una quantità relativamente piccola dei dati di mappatura per generare l'interfaccia utente audio. Quasi tutta l'interfaccia audio usa solo i tile al livello di zoom massimo. L'app unisce una griglia di tile 2 per 2 attorno al punto in cui si trova l'utente e poi esamina solo alcuni dei livelli:

* `transportation` - per tutti i tipi di Way inclusi strade, sentieri, ferrovie e tranvie.
* `poi` - punti di interesse, ad esempio negozi, centri sportivi, panchine, cassette postali, fermate dell'autobus ecc.
* `building` - questo è per i `poi` che sono mappati come qualcosa di più di un semplice punto, ad esempio grandi supermercati o municipi.

Unisce linee e poligoni attraverso i confini dei tile e trasforma tutte le Way in segmenti di Way connessi e incroci. Questo è importante perché ci permette di cercare lungo una Way per scoprire dove possiamo arrivare.

Tutti i dati analizzati vengono inoltre inseriti in un formato facile da cercare, in modo che l'app possa trovare facilmente quali elementi della mappa sono nelle vicinanze. A questo punto i dati vengono classificati in categorie. Le categorie attuali sono:

* Strade
* Strade e sentieri (tutte le Way)
* Incroci - i punti in cui le Way si intersecano
* Ingressi - questi sono punti su un edificio che sono stati contrassegnati come ingresso.
* Attraversamenti - attraversamenti stradali
* POI - tutti i punti di interesse
* Fermate del trasporto pubblico - fermate dell'autobus, stazioni ferroviarie, fermate del tram e così via.
* Sottocategorie dei POI:
  * Informazioni
  * Oggetto
  * Luogo
  * Punto di riferimento
  * Mobilità
  * Sicurezza
* Insediamenti e sottocategorie (vedi sezione successiva)
  * Città
  * Cittadine
  * Paesi
  * Frazioni

 Con questo in atto, per qualsiasi posizione l'app può quindi trovare facilmente

 * "Tutte le fermate del trasporto pubblico entro 50m" oppure
 * "L'incrocio più vicino davanti a me" oppure
 * "La frazione, il paese, la cittadina o la città più vicina"
  
Con questo in atto, creare le notifiche audio è solo una questione di interrogare i dati in base alla posizione e alla direzione attuali. Man mano che l'utente si sposta attraverso una griglia di tile, questa viene aggiornata in modo da centrarsi sulla posizione attuale.

### Più dati
Uno dei problemi della nostra griglia di dati di mappa molto locale è che significa che possiamo 'vedere' al massimo circa 1km in ogni direzione. Va bene quando descriviamo ciò che ci sta di fronte, ma a volte vorremmo fornire più contesto. L'esempio principale di questo è quando l'app viene usata e l'utente non sta camminando.

Quando l'app rileva che l'utente sta viaggiando a più di 5 metri al secondo, cambia il modo in cui descrive il mondo. Invece di notificare ogni incrocio e POI, notifica meno spesso e solo le strade vicine. Il problema è che conoscere il nome di una strada non è molto utile se non si sa in quale cittadina si trova.

Per cercare di affrontare questo, ora analizziamo anche i dati della mappa a un livello di zoom inferiore ed estraiamo i dati dal livello `place`. Questo contiene i nomi di cittadine, città, quartieri, paesi e così via. Un problema nel mappare le cose è che non c'è sempre un confine evidente tra questi luoghi. OpenStreetMap a volte ha i confini delle città nel suo database, ma anche quando è così, nel momento in cui arriva alla nostra mappa a tile quell'informazione spesso si perde. Ciò che abbiamo è la posizione in cui i nomi dei luoghi sono disegnati sulla mappa. Questi vengono categorizzati e poi l'app troverà la frazione, il paese, la cittadina o la città più vicina all'utente e la segnalerà.

Per molte città il nome effettivo della città non verrà mai notificato, perché la maggior parte delle città è divisa in suddivisioni più piccole come i quartieri, ma questi forniscono un contesto aggiuntivo molto utile. Ricorda solo che, poiché l'app segnala che sei vicino a una via in un particolare quartiere, ciò significa solo che l'etichetta di quel quartiere è il punto più vicino e potrebbe essere errata o persino al di là di un fiume.

### Più contesto
Più contesto può essere aggiunto nelle descrizioni, meglio è, purché sia mantenuto conciso e prevedibile. Uno dei problemi che abbiamo riscontrato nel descrivere gli incroci è che spesso erano coinvolte Way 'senza nome'. Queste sono Way che non hanno un nome. Nei dati di mappatura queste potrebbero essere semplicemente una pista, un sentiero o una strada di servizio, ma senza più contesto non sono molto utili nelle descrizioni testuali. Per fortuna, possiamo fare di meglio, quindi ciò che fa l'app è che ogni volta che sta per annunciare una Way senza nome verifica se riesce a trovare qualche contesto in più per essa.

* **È un marciapiede?**
In molte aree di OpenStreetMap i marciapiedi sono ora mappati separatamente dalle strade. Questi sono solitamente etichettati come `sidewalk` ma di norma non indicano qual è la strada di cui sono il marciapiede.

    Quando l'app incontra un marciapiede senza nome, cerca una strada che ritiene scorrere accanto ad esso e la usa per dare un nome al marciapiede. Questo si rivela molto importante per le nostre notifiche. Invece di annunciare ogni incrocio di marciapiede, mentre ci muoviamo lungo un marciapiede le notifiche vengono fatte come se ci stessimo muovendo lungo la strada associata. Invece di *"In viaggio verso ovest lungo il sentiero"* abbiamo *"In viaggio verso ovest lungo Moor Road"*. L'utente si trova sul marciapiede mappato, ma la descrizione ha più senso.

* **Termina in una Way con nome?**
Molto spesso ci sono sentieri pedonali che collegano due strade. Osservando entrambe le estremità del sentiero possiamo aggiungere facilmente quel contesto, così che in una direzione potrebbe essere *"Sentiero verso Moor Road"* e avvicinandosi dall'altra estremità potrebbe essere *"Sentiero verso Roselea Drive"*. Questo viene fatto solo dove il sentiero non si dirama; se si dirama in due sentieri senza nome allora non proviamo ad aggiungere questo contesto.

* **Termina vicino a un Indicatore?**
Se una Way senza nome inizia o termina vicino a un Indicatore, questo viene usato per descriverla, ad esempio *"Sentiero verso l'incrocio del grande albero"*. L'utente può aggiungere Indicatori dove vuole e, aggiungendo Indicatori lungo le reti di sentieri, può aggiungere contesto a un intero percorso.

* **Entra o esce da un POI?**
Se una Way senza nome inizia all'esterno di un POI e termina al suo interno (o viceversa) allora possiamo aggiungere quel contesto, ad esempio *"Pista verso Lennox Park"*.

* **Termina vicino a un Ingresso?**
Se una Way senza nome inizia o termina più vicino a un Ingresso allora possiamo aggiungere quel contesto, ad esempio *"Strada di servizio verso Best Buy"*.

* **Termina vicino a un Punto di riferimento o Luogo?**
Se una Way senza nome inizia o termina più vicino a un Punto di riferimento allora possiamo anche aggiungere quel contesto, ad esempio *"Strada di servizio verso la Cattedrale di St. Giles"*.

* **È un vicolo cieco?**
L'app contrassegna come vicolo cieco qualsiasi Way senza nome che non porta da nessuna parte.

* **Passa per degli scalini?**
Se la Way senza nome passa sopra un ponte, attraverso un tunnel o su/giù per degli scalini, allora questo viene annotato e aggiunto al contesto. Questo è separato dall'etichettatura della destinazione, quindi è possibile un contesto come *"Sentiero sopra il ponte verso Lennox Park"*.

Questi contesti vengono aggiunti in ordine e quindi è possibile avere *"Sentiero verso Park Lane tramite scalini"* in una direzione e *"Sentiero verso Lennox Park tramite scalini"* nell'altra direzione. La via con nome ha la priorità lasciando il parco, ma il parco viene usato quando vi si entra.

#### Contesto futuro
Ci sono vari contesti aggiuntivi che speriamo di aggiungere in futuro, tra cui:
* Contesto per le Way che seguono caratteristiche idriche lineari, ad esempio *"Sentiero accanto al River Dee"*
* Contesto per le Way che seguono il bordo di specchi d'acqua *"Sentiero accanto al bacino idrico di Milngavie"*
* Contesto per le Way che seguono le ferrovie, ad esempio *"Sentiero accanto alla ferrovia"*. Questo potrebbe persino includere il nome della linea ferroviaria
* Aggiungere contenuto extra a ponti e tunnel, cosa c'è sopra o sotto, ad esempio *"Sentiero tramite ponte sopra la ferrovia verso Moor Road"*

## Notifiche audio
Ora che abbiamo i dati della mappa in un formato che possiamo usare facilmente, generare le notifiche è davvero piuttosto semplice.

### Notifiche durante la camminata
Quando si cammina, le notifiche audio che possono verificarsi sono (in ordine di priorità):

1. Descrivere quanto è lontana la destinazione attuale
1. Descrivere un incrocio imminente
1. Descrivere i 5 punti di interesse più vicini

Tutte le notifiche hanno una frequenza limitata in modo da non ripetersi troppo spesso. Se l'utente smette di muoversi, le notifiche si interromperanno e, anche durante il movimento, una notifica non si ripeterà a ogni nuova posizione GPS. La frequenza, come nell'app iOS, è:

* Ogni 60 secondi per la destinazione attuale
* Ogni 30 secondi per un incrocio imminente
* Ogni 60 secondi per un punto di interesse

Le notifiche possono essere filtrate tramite il menu delle impostazioni, e c'è sicuramente margine per ampliare questo comportamento.

### Notifiche durante un viaggio più veloce
Quando si viaggia a più di 5 metri al secondo, la notifica della destinazione attuale ha comunque luogo, ma le notifiche degli incroci e dei punti di interesse vengono sostituite da una notifica che descrive all'incirca dove si trova l'utente. Questa fornisce una fermata del trasporto pubblico nelle vicinanze, un punto di interesse che ci contiene, ad esempio all'interno di un grande parco, oppure una strada e un insediamento nelle vicinanze. Questi usano i dati descritti in precedenza, e c'è un ovvio margine per consentire la personalizzazione di questo in futuro.

## Indicatori e Percorsi
Per la maggior parte gli indicatori e i percorsi sono semplicemente una funzionalità dell'interfaccia utente che non si basa né sul GPS né realmente sui dati della mappa. Gli indicatori sono posizioni con nome che l'utente vuole memorizzare, e i percorsi sono un elenco ordinato di quegli indicatori. L'interfaccia utente per crearli entrambi è presa direttamente dalla versione iOS.

### Riproduzione del percorso
La riproduzione del percorso è dove i percorsi prendono vita. Quando un percorso viene riprodotto, viene creato un audiofaro al primo indicatore del percorso. Una volta che l'utente si avvicina a quell'indicatore, il percorso sposta automaticamente l'audiofaro all'indicatore successivo del percorso. Se non ci sono più indicatori, allora la riproduzione del percorso termina.

## Conclusione
Si spera che questo abbia dato qualche spunto su come funziona l'app. L'app è sempre in evoluzione in base ai feedback degli utenti, quindi mettiti in contatto con noi se c'è qualcosa che pensi possa essere aggiunto.
