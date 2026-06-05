---
title: Kuinka Soundscape toimii
layout: page
parent: "Soundscapen käyttö"
has_toc: false
lang: fi
permalink: /users/how-it-works.html
machine-translated: true
---
# Kuinka Soundscape toimii
Tämän sivun tarkoituksena on antaa yleiskäsitys siitä, kuinka Soundscape-sovellus toimii konepellin alla. Sinun ei tarvitse lukea tätä käyttääksesi sovellusta, mutta sen kirjoittamiselle on muutama syy:

1. Auttaa kiinnostuneita sovelluksen uusia käyttäjiä ymmärtämään, missä sen rajoitukset ovat
1. Antaa käyttäjille käsitys siitä, mitä muuta uusilla ominaisuuksilla voitaisiin tehdä
1. Antaa kehittäjille yleiskuvan sovelluksen toiminnasta

On kaksi teknologiaa, jotka tekevät sovelluksen mahdolliseksi: GPS ja OpenStreetMap-data. GPS antaa meille hyvän käsityksen siitä, missä puhelin on ja missä se on ollut. OpenStreetMap-dataa voidaan sitten käyttää löytämään, mitä on lähellä, ja voimme käyttää sitä kuvaamaan sitä käyttäjälle.

## Äänimajakat
Useimmilla tavoilla nämä ovat yksinkertaisimpia asioita toteuttaa teknologian näkökulmasta. Olettaen, että meillä on puhelimen sijainti ja suunta puhelimelle, voimme sitten muuttaa majakan ääntä niin, että se kuulostaa tulevan kyseisestä suunnasta. Käytämme Steam Audion kirjastoa äänen paikantamiseen, joka käyttää pään muotoon liittyviä siirtofunktioita (HRTF) parhaan mahdollisen kuuloisen paikannuksen antamiseksi. Ainoa muu asia, jonka teemme, on muuttaa majakan ääntä niin, että toistetaan eri ääni riippuen käyttäjän suunnan ja majakan sijainnin välisestä kulmasta. Kulmat vaihtelevat valitun majakan mukaan (Kosketus, Leimahdus, Ping jne.), ja joillakin on suurempi määrä ääniä kuin toisilla. Ja siinä on äänimajakat yksinkertaisimmillaan.

Ainoa lisämonimutkaisuus on oletus puhelimen sijainnista ja suunnasta, johon käyttäjä osoittaa. Tarkastellaan näitä vuorollaan.

### Sijainti
GPS:n palauttamassa sijainnissa voi olla melko suuri virhe, ja tämä riippuu siitä, kuinka paljon taivaasta on näkyvissä puhelimen GPS:lle, ja kuinka monta puuta ja korkeaa rakennusta heijastaa GPS-signaalia matkalla puhelimeen.

Lähestymistapa, jonka olemme valinneet sijainnin suodattamiseen, on käyttää niin sanottua karttasovitusta. Tämä olettaa, että käyttäjä todennäköisimmin liikkuu kartoitettua polkua tai tietä pitkin – käytämme termiä 'Way' (väylä) kattamaan kaikki tiet, polut ja reitit. Karttasovitus tarkastelee, missä käyttäjä on ollut, ja käyttäen liikkeen suuntaa yhdessä paikallisen karttadatan kanssa se valitsee todennäköisimmän sijainnin väylällä. Tämä lähestymistapa ei ota huomioon vain GPS:n virheitä, vaan myös karttadatan virheet. Kaikkia väyliä ei ole kartoitettu tarkasti, joten niissäkin on virheitä. Määrittääkseen, mikä on todennäköisin väylä, jolla käyttäjä on, algoritmi ottaa huomioon:
* Kuinka lähellä väylä on GPS-sijaintia ja aiempia GPS-sijainteja
* Liikkeen suunnan – liikkuvatko he samaan suuntaan kuin väylä
* Onko mahdollista päästä viimeksi karttasovitetusta sijainnista uuteen sijaintiin väylien verkoston kautta. Tämä vaaditaan, jotta voidaan sulkea pois vaihtaminen väylien välillä, jotka eivät todellisuudessa ole yhteydessä toisiinsa, esim. yksi kulkee toisen yli sillalla tai sen ali tunnelissa.
Karttasovitus voi päättää, ettei lähellä ole väyliä, tai ettei se ole varma, millä väylällä käyttäjä on, ja tässä tapauksessa se yksinkertaisesti odottaa seuraavaa GPS-sijaintia ja yrittää uudelleen, kunnes se on varma.

### Suunta
On useita suuntia, joita seuraamme ohjelmistossa:

1. Suunta, johon puhelin osoittaa. Käytämme tätä, kun puhelin on lukitsematon ja sovellus on käytössä, mutta myös silloin, kun puhelin on lukittu, kunhan sitä pidetään vaakatasossa näyttö taivasta kohti. Tämä on hyvä pitää mielessä, kun laitat puhelimen laukkuusi. Jos se laitetaan vaakatasossa pystyssä pidettävän laukun pohjalle, sovellus käyttäisi sattumanvaraista suuntaa, johon laukkusi osoittaa.
1. Suunta, johon puhelin liikkuu.
1. Suunta päänseurannalla varustetuista kuulokkeista. Emme tällä hetkellä käytä tätä, vaikka iOS-sovellus tuki sitä. Meillä on teknologia valmiina lisätä se tulevaisuudessa.

Kun puhelin on lukittu ja laukussa, sovellus käyttää liikkeen suuntaa. Jos käyttäjä ei kuitenkaan liiku, suuntaa ei ole saatavilla. Kun näin tapahtuu, äänimajakat hiljenevät osoittaakseen, ettei majakan nykyistä suuntaa ole mahdollista tietää – käyttäjä voisi olla kääntymässä ympäri vaihtamatta sijaintia.

Joissakin sovelluksen suunnan käytöissä suunta 'napsautetaan' karttasovitetun väylän suuntaan, joten jos käyttäjä kävelee suunnilleen väylän suuntaan, väylän todellisen suunnan oletetaan olevan oikea ja sitä käytetään näissä laskelmissa.

### Päätelmä
Vaikka äänimajakat ovat päällisin puolin suoraviivaisia, karttasovituksen käyttö sijainti- ja suuntavirheiden poistamiseksi tuo mukanaan melkoisen määrän monimutkaisuutta.

## Karttadata
Sovelluksen käyttämä karttadata on lähes kokonaan peräisin OpenStreetMap-projektista. Pyöritämme palvelinta, joka sisältää koko maailman kartan useilla zoomaustasoilla. Jokainen zoomaustaso on jaettu ruutuihin (tiles). Zoomaustaso 0 sisältää 1 ruudun, taso 1 sisältää 4 ruutua, taso 2 sisältää 16 ruutua ja niin edelleen aina tasoon 14 saakka, joka sisältää noin 268 miljoonaa ruutua kattaakseen planeetan. Jokainen ruutu sisältää useita tasoja, ja jokaisella tasolla on pisteitä, viivoja ja monikulmioita, jotka voidaan piirtää graafisen kartan muodostamiseksi. Tämä graafinen kartta on se, joka näytetään käyttäjälle sovelluksen käyttöliittymässä. Jokaisella pisteellä, viivalla ja monikulmiolla on metadataa, joka kuvaa, mitä se on. Tämä on enimmäkseen suoraan OpenStreetMap-datasta, joten viiva voi olla `footway` (jalankulkuväylä), joka on `sidewalk` (jalkakäytävä), tai `road` (tie), joka on `minor` (vähäinen)

Data muunnetaan graafiseksi kartaksi 'tyylin' (style) avulla, jolla on säännöt siitä, kuinka piirtää eri pisteet, viivat ja monikulmiot kullakin tasolla, esim. kuinka piirtää polku, kuinka piirtää metsä, kuinka piirtää linja-autopysäkki. Säännöt voivat vaihdella zoomaustason mukaan, mistä syystä lähennettäessä yhä enemmän pisteitä ja viivoja tulee näkyviin, jotka eivät ole näkyvissä loitonnettaessa, esim. linja-autopysäkit ja polut.

Tyyliä muuttamalla voimme muuttaa, miltä käyttöliittymän kartta näyttää, mistä 'esteetön kartta', jota kokeilemme, on peräisin. Se pyrkii suurempaan kontrastiin sekä rohkeampiin viivoihin ja tekstiin. Tyyli on sisäänrakennettu sovellukseen, joten meidän ei tarvitse muuttaa karttaa palvelimella muuttaaksemme, miltä se näyttää.

Mutta kuinka käytämme karttadataa ääneen?

### Karttadatan käyttö ääneen
Käytämme tällä hetkellä suhteellisen pientä määrää karttadatasta ääni-käyttöliittymän luomiseen. Lähes koko ääni-UI käyttää vain suurimman zoomaustason ruutuja. Sovellus liittää yhteen 2 kertaa 2 ruudukon ruuduista käyttäjän ympärillä, ja sitten se tarkastelee vain muutamaa tasoa:

* `transportation` - kaikille väylätyypeille, mukaan lukien tiet, polut, rautatiet ja raitiotiet.
* `poi` - kiinnostavat kohteet, esim. kaupat, urheilukeskukset, penkit, postilaatikot, linja-autopysäkit jne.
* `building` - tämä on `poi`-kohteille, jotka on kartoitettu enempänä kuin vain pisteenä, esim. suuret supermarketit tai kaupungintalot.

Se liittää yhteen viivat ja monikulmiot ruudun rajojen yli ja muuntaa kaikki väylät yhdistetyiksi väyläsegmenteiksi ja risteyksiksi. Tämä on tärkeää, koska se mahdollistaa väylän pitkin etsimisen selvittääksemme, minne voimme päästä.

Kaikki jäsennetty data laitetaan myös helposti haettavaan muotoon, jotta sovellus voi helposti löytää, mitkä kartan piirteet ovat lähellä. Tässä vaiheessa data luokitellaan kategorioihin. Nykyiset kategoriat ovat:

* Tiet
* Tiet ja polut (kaikki väylät)
* Risteykset - pisteet, joissa väylät risteävät
* Sisäänkäynnit - nämä ovat rakennuksen pisteitä, jotka on merkitty sisäänkäynniksi.
* Suojatiet - tienylityskohdat
* POI:t - kaikki kiinnostavat kohteet
* Joukkoliikennepysäkit - linja-autopysäkit, rautatieasemat, raitiovaunupysäkit ja niin edelleen.
* POI:den alakategoriat:
  * Tieto
  * Objekti
  * Paikka
  * Maamerkki
  * Liikkuvuus
  * Turvallisuus
* Asutukset ja alakategoriat (katso seuraava osio)
  * Kaupungit (suuret)
  * Kaupungit
  * Kylät
  * Pikkukylät

 Tämän ollessa paikallaan sovellus voi sitten helposti löytää mille tahansa sijainnille

 * "Kaikki joukkoliikennepysäkit 50 metrin sisällä" tai
 * "Lähin risteys edessäpäin" tai
 * "Lähin pikkukylä, kylä, kaupunki tai suurkaupunki"
  
Tämän ollessa paikallaan ääni-ilmoitusten luominen on vain kysymys datan hakemisesta nykyisen sijainnin ja suunnan perusteella. Kun käyttäjä liikkuu ruudukon poikki, se päivittää sen niin, että se keskittyy nykyisen sijainnin ympärille.

### Lisää dataa
Yksi ongelma erittäin paikallisessa karttadataruudukossamme on se, että voimme 'nähdä' korkeintaan noin 1 km mihin tahansa suuntaan. Se on kunnossa, kun kuvailemme, mitä on edessämme, mutta joskus haluaisimme antaa enemmän kontekstia. Tärkein esimerkki tästä on, kun sovellusta käytetään eikä käyttäjä kävele.

Kun sovellus havaitsee, että käyttäjä liikkuu yli 5 metriä sekunnissa, se vaihtaa tapaa, jolla se kuvailee maailmaa. Sen sijaan, että se ilmoittaisi jokaisesta risteyksestä ja POI:sta, se ilmoittaa harvemmin ja vain lähellä olevista teistä. Tämän ongelma on se, että tien nimen tietäminen ei ole kovin hyödyllistä, jos et tiedä, missä kaupungissa se on.

Tämän ratkaisemiseksi jäsennämme nyt myös karttadatan alemmalla zoomaustasolla ja poimimme dataa `place`-tasolta. Tämä sisältää kaupunkien, suurkaupunkien, kaupunginosien, kylien ja niin edelleen nimet. Yksi asioiden kartoittamisen ongelma on se, ettei näiden paikkojen välillä ole aina selvää rajaa. OpenStreetMapilla on joskus kaupunkien rajat tietokannassaan, mutta vaikka näin olisi, siihen mennessä kun se päätyy ruutukartallemme, tuo tieto on usein kadonnut. Mitä meillä on, on sijainti, johon paikkojen nimet on piirretty kartalle. Nämä luokitellaan, ja sitten sovellus löytää lähimmän pikkukylän, kylän, kaupungin tai suurkaupungin käyttäjälle ja ilmoittaa sen.

Monille suurkaupungeille varsinaista kaupungin nimeä ei koskaan ilmoiteta, koska useimmat suurkaupungit on jaettu pienempiin osiin, kuten kaupunginosiin, mutta ne antavat lisäkontekstia ja ovat erittäin hyödyllisiä. Muista vain, että koska sovellus ilmoittaa, että olet lähellä katua tietyssä kaupunginosassa, se tarkoittaa vain, että kyseisen kaupunginosan nimike on lähin piste, ja se saattaa olla virheellinen tai jopa joen toisella puolella.

### Lisää kontekstia
Mitä enemmän kontekstia kuvauksiin voidaan lisätä, sitä parempi, kunhan se pidetään ytimekkäänä ja ennustettavana. Yksi ongelma, jonka näimme risteysten kuvailussa, oli se, että usein mukana oli 'nimettömiä' väyliä. Nämä ovat väyliä, joilla ei ole nimeä. Karttadatassa nämä saattavat olla vain reitti, polku tai huoltotie, mutta ilman enempää kontekstia se ei ole kovin hyödyllinen tekstikuvauksissa. Onneksi voimme tehdä paremmin, joten sovellus tekee niin, että aina kun se on ilmoittamassa nimettömästä väylästä, se katsoo, voiko se selvittää sille jonkin verran enemmän kontekstia.

* **Onko se jalkakäytävä?**
Monilla OpenStreetMapin alueilla on nyt jalkakäytävät kartoitettu erikseen teistä. Nämä on yleensä merkitty `sidewalk`-merkinnällä, mutta ne eivät tavallisesti kerro, mikä on tie, jonka jalkakäytävä ne ovat.

    Kun sovellus kohtaa nimettömän jalkakäytävän, se etsii tien, jonka se ajattelee kulkevan sen vieressä, ja käyttää sitä jalkakäytävän nimeämiseen. Tämä osoittautuu erittäin tärkeäksi ilmoituksillemme. Sen sijaan, että ilmoittaisimme jokaisesta jalkakäytävän risteyksestä, jalkakäytävää pitkin liikkuessamme ilmoitukset tehdään ikään kuin liikkuisimme siihen liittyvää tietä pitkin. Sen sijaan, että sanottaisiin *"Kuljetaan länteen polkua pitkin"*, meillä on *"Kuljetaan länteen tiellä Moor Road"*. Käyttäjä on kartoitetulla jalkakäytävällä, mutta kuvaus on järkevämpi.

* **Päättyykö se nimettyyn väylään?**
Hyvin usein on jalankulkupolkuja, jotka yhdistävät kaksi tietä toisiinsa. Tarkastelemalla polun molempia päitä voimme helposti lisätä tuon kontekstin niin, että yhteen suuntaan se voisi olla *"Polku tielle Moor Road"* ja toisesta päästä lähestyttäessä se voisi olla *"Polku tielle Roselea Drive"*. Tämä tehdään vain silloin, kun polku ei haaraudu; jos se haarautuu kahdeksi nimettömäksi poluksi, emme yritä lisätä tätä kontekstia.

* **Päättyykö se lähelle merkintää?**
Jos nimetön väylä alkaa tai päättyy lähellä merkintää, sitä käytetään sen kuvaamiseen, esim. *"Polku ison puun risteykseen"*. Käyttäjä voi lisätä merkintöjä minne tahansa haluaa, ja lisäämällä merkintöjä polkuverkostojen varrelle hän voi lisätä kontekstia koko reitille.

* **Meneekö se POI:hin tai poistuuko siitä?**
Jos nimetön väylä alkaa POI:n ulkopuolelta ja päättyy sen sisälle (tai päinvastoin), voimme lisätä tuon kontekstin, esim. *"Reitti puistoon Lennox Park"*. 

* **Päättyykö se lähelle sisäänkäyntiä?**
Jos nimetön väylä alkaa tai päättyy lähemmäs sisäänkäyntiä, voimme lisätä tuon kontekstin, esim. *"Huoltotie kohteeseen Best Buy"*.

* **Päättyykö se lähelle maamerkkiä tai paikkaa?**
Jos nimetön väylä alkaa tai päättyy lähemmäs maamerkkiä, voimme myös lisätä tuon kontekstin, esim. *"Huoltotie kohteeseen St. Giles Cathedral"*.

* **Onko se umpikuja?**
Sovellus merkitsee umpikujaksi kaikki nimettömät väylät, jotka eivät johda minnekään.

* **Ohittaako se portaita?**
Jos nimetön väylä kulkee sillan yli, tunnelin läpi tai portaita ylös/alas, tämä huomioidaan ja lisätään kontekstiin. Tämä on erillistä määränpäämerkinnästä, joten konteksti kuten *"Polku sillan yli puistoon Lennox Park"* on mahdollinen.

Nämä kontekstit lisätään järjestyksessä, joten on mahdollista saada *"Polku kadulle Park Lane portaiden kautta"* yhteen suuntaan ja *"Polku puistoon Lennox Park portaiden kautta"* toiseen suuntaan. Nimetty katu saa etusijan puistosta poistuttaessa, mutta puistoa käytetään sinne saavuttaessa.

#### Tuleva konteksti
On useita lisäkonteksteja, joita toivomme lisäävämme tulevaisuudessa, mukaan lukien:
* Konteksti väylille, jotka seuraavat lineaarisia vesistöjä, esim. *"Polku joen Dee vieressä"*
* Konteksti väylille, jotka seuraavat vesistöjen reunaa *"Polku tekojärven Milngavie vieressä"*
* Konteksti väylille, jotka seuraavat rautateitä, esim. *"Polku rautatien vieressä"*. Tämä voisi jopa sisältää rautatielinjan nimen
* Lisää lisäsisältöä silloille ja tunneleille, minkä yli tai ali ne kulkevat, esim. *"Polku sillan kautta rautatien yli tielle Moor Road"*

## Ääni-ilmoitukset
Nyt kun meillä on karttadata muodossa, jota voimme helposti käyttää, ilmoitusten luominen on todella melko suoraviivaista.

### Ilmoitukset kävellessä
Kävellessä mahdolliset ääni-ilmoitukset ovat (tärkeysjärjestyksessä):

1. Kuvaa, kuinka kaukana nykyinen määränpää on
1. Kuvaa tuleva risteys
1. Kuvaa 5 lähintä kiinnostavaa kohdetta

Kaikki ilmoitukset on nopeusrajoitettu niin, etteivät ne toistu liian usein. Jos käyttäjä lakkaa liikkumasta, ilmoitukset lakkaavat, ja jopa liikkuessa ilmoitus ei toistu jokaisella uudella GPS-sijainnilla. Tiheys iOS-sovelluksen mukaan on:

* 60 sekunnin välein nykyiselle määränpäälle
* 30 sekunnin välein tulevalle risteykselle
* 60 sekunnin välein kiinnostavalle kohteelle

Ilmoituksia voidaan suodattaa asetusvalikon kautta, ja tätä toimintaa on varmasti mahdollista laajentaa.

### Ilmoitukset nopeammin liikuttaessa
Liikuttaessa yli 5 metriä sekunnissa ilmoitus nykyiseen määränpäähän tapahtuu edelleen, mutta risteys- ja kiinnostavien kohteiden ilmoitukset korvataan ilmoituksella, joka kuvaa suunnilleen, missä käyttäjä on. Tämä antaa lähellä olevan joukkoliikennepysäkin, kiinnostavan kohteen, joka sisältää meidät, esim. suuren puiston sisällä, tai lähellä olevan tien ja asutuksen. Nämä käyttävät aiemmin kuvattua dataa, ja tämän mukauttamisen mahdollistamiselle on tulevaisuudessa selvää tilaa.

## Merkinnät ja reitit
Suurimmaksi osaksi merkinnät ja reitit ovat vain käyttöliittymäominaisuus, joka ei juuri nojaa GPS:ään eikä karttadataan. Merkinnät ovat nimettyjä sijainteja, jotka käyttäjä haluaa tallentaa, ja reitit ovat järjestetty luettelo näistä merkinnöistä. Molempien luomisen käyttöliittymä on otettu suoraan iOS-versiosta.

### Reitin toisto
Reitin toisto on se, missä reitit heräävät eloon. Kun reittiä toistetaan, äänimajakka luodaan reitin ensimmäiselle merkinnälle. Kun käyttäjä pääsee lähelle kyseistä merkintää, reitti siirtää äänimajakan automaattisesti reitin seuraavalle merkinnälle. Jos merkintöjä ei ole enää, reitin toisto päättyy.

## Päätelmä
Toivottavasti tämä on antanut jonkin verran näkemystä siitä, kuinka sovellus toimii. Sovellus kehittyy jatkuvasti käyttäjien palautteen perusteella, joten ota yhteyttä, jos jotain mielestäsi voitaisiin lisätä.
