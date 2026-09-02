---
title: Julkaisutiedot
layout: page
nav_order: 5
has_toc: false
lang: fi
permalink: /release-notes.html
machine-translated: true
---

# Julkaisutiedot

Soundscape 2.0 on suuri julkaisu ja on tällä hetkellä suljetussa beta-vaiheessa. Tärkein muutos on,
että Soundscapella on nyt jotain hyödyllistä sanottavaa myös silloin, kun matkustat autolla, bussilla
tai junalla, eikä enää vain kävellessäsi. Lisäksi paikkojen kuvaamiseen on tehty paljon pienempää
työtä, mukana on kaksikymmentä uutta kieltä ja pitkä lista korjauksia.

Vanhempien versioiden tiedot löytyvät sivulta
[Julkaisutiedot 1.x-versioille]({{ "/v1.0-release-notes.html" | relative_url }}).

## Uutta versiossa 2.0

* **Kuulutukset autolla, bussilla tai junalla matkustettaessa.** Soundscape tunnistaa, kun liikut
  vauhdilla, ja kuvaa matkaasi välittömän ympäristösi sijaan.
* **Ilmoitus vesistöjen ja rautateiden ylityksestä.** Joet, kanavat, vuonot ja rautatiet kuulutetaan,
  kun ylität ne — sekä kävellen että matkalla.
* **Paremmat osoitteet ja paikannimet.** Paikat, joilla ei ole omaa osoitetta, saavat nyt kadun ja
  alueen, jolla ne sijaitsevat, talonnumerot yhdistetään kadun oikeaan puoleen, ja bussipysäkit
  Isossa-Britanniassa käyttävät virallisia nimiään.
* **Kaksikymmentä uutta kieltä**, joten niitä on nyt yhteensä 46. Myös tämä dokumentaatiosivusto on
  käännetty.
* **Herätys lähdettäessä.** Lepotila voi nyt herättää Soundscapen, kun poistut paikasta, jossa asetit
  sen lepäämään.
* **Lyhyemmät, luontevammat etäisyydet**, suuremmilla yksiköillä kun liikut nopeasti.
* **Nopeampi uloskäynti.** *Poistu Soundscapesta* on nyt päävalikon ylimpänä.
* **Parannuksia offline-karttoihin**, muun muassa jo ladatun kartan päivittäminen ja kartta
  saatavilla olevista alueista tällä sivustolla.
* **Paljon saavutettavuustyötä** TalkBackin parissa, erityisesti aloitusnäytöissä.
* **Erittäin paljon kaatumis- ja vakauskorjauksia.**

Versiossa 2.0 on **poistettu** kaksi asiaa: ääniohjaus ja sovelluksen sisäinen kielivalikko. Katso
alta [Poistetut ominaisuudet](#things-that-have-been-removed), mitä voit tehdä niiden sijaan.

---

## Tarkemmin

### Matkustaminen autolla, bussilla tai junalla

Tämä on suurin uutuus nykyisille käyttäjille. Aiemmin Soundscapella oli hyvin vähän sanottavaa heti
kun nousit ajoneuvoon: se jatkoi välittömän ympäristön kuvaamista, mikä vauhdissa tarkoitti virtaa
asioita, jotka olit jo ohittanut.

Soundscape huomaa nyt, että liikut kävelyvauhtia nopeammin, ja muuttaa sitä mitä se kertoo. Mitään ei
tarvitse kytkeä päälle, ja kaikki palaa itsestään normaaliksi heti kun hidastat tai nouset ulos ja
kävelet.

Matkan aikana kuulet:

* **Missä olet**, aika ajoin — tien jolla ajat ja suuntasi, esimerkiksi »Matkalla pohjoiseen tietä M8
  pitkin«. Numeroidut tiet kuulutetaan numerollaan, eikä Soundscape kuuluta samaa tietä uudelleen aina
  kun kadunnimi vaihtuu.
* **Kaupungit ja kylät**, joita kohti matkaat, etäisyyden kera, sekä ne joista loitonnet tai jotka
  vain ohitat.
* **Moottoritieliittymät ja -rampit** saapuessasi niiden kohdalle.
* **Suuret maamerkit** ohittaessasi ne, kuten puistot, sairaalat, stadionit ja kauppakeskukset.
* **Bussi-, raitiovaunu- ja juna-asemat** ohittaessasi ne. Soundscape mainitsee vain oman puolesi
  pysäkit, koska vastakkaisen puolen pysäkit palvelevat vastakkaista suuntaa.
* **Joet, kanavat ja rautatiet, jotka ylität.**
* **Tunnelit**, mikä lähinnä selittää, miksi Soundscape on hiljenemäisillään — sisällä ei ole
  GPS-signaalia.

**Junassa** Soundscape päättelee, että olet rautatiellä eikä maantiellä, ja kertoo, mitä paikkakuntia
ohitat ja kuinka pitkälle olet päässyt edellisestä asemasta. Tämän päättely on vaikeampaa kuin miltä
kuulostaa, sillä moottoritiet ja rautatiet kulkevat usein rinnakkain kilometrikaupalla, joten hyvä osa
tämän julkaisun työstä meni siihen, ettei toista sekoiteta toiseen.

Tavalliset kävelijän kuulutukset — lähikaupat, suojatiet ja niin edelleen — pidätetään tarkoituksella
matkan ajaksi, ja etäisyyksiä, joilla asioista kerrotaan, on pidennetty huomattavasti, jotta kuulet
asiasta ennen kuin olet jo ohittanut sen.

### Vesistöjen ja rautateiden ylitys

Soundscape kertoo nyt, kun ylität joen, kanavan, vuonon, lahden tai rautatien. Tämä toimii sekä
kävellen että matkalla, ja kattaa yhtä lailla alitse kuin ylitse kulkemisen, joten sekä kävelysilta
että alikulku kuvataan.

### Paremmat osoitteet ja paikannimet

Paljon työtä on tehty sen eteen, että Soundscape kuvaa paikkoja niin kuin ihminen kuvaisi:

* Paikat, joilla ei ole omaa osoitetta, kuvataan nyt kadun ja alueen perusteella, jolla ne sijaitsevat,
  sen sijaan että jäisivät epämääräisiksi.
* Talonnumerot yhdistetään kadun oikeaan puoleen. Aiemmin osoite saatettiin ilmoittaa vastapäiseltä
  jalkakäytävältä.
* Paikan osoite ei enää toista paikan omaa nimeä.
* Bussipysäkit Isossa-Britanniassa käyttävät virallisia joukkoliikennenimiään, yleensä niitä jotka
  näkyvät aikataulussa ja pysäkin kyltissä.
* Nimettömät kävelypolut joen tai kanavan varrella nimetään nyt sen vesistön mukaan, jota ne seuraavat.
* Nimettömät polut ja tiet kuvataan järkevämmin, ja niistä käytetyt sanat on käännetty kunnolla sen
  sijaan että ne näkyisivät englanniksi.

### Kielet

Versioon 2.0 on lisätty kaksikymmentä uutta kieltä: arabia, bengali, bulgaria, katalaani, kroatia,
tšekki, hausa, unkari, indonesia, korea, marathi, serbia, slovakki, sloveeni, swahili, tamili, telugu,
thai, urdu ja vietnam. Kaikki nämä kielet ovat alfa-vaiheessa, ja toivomme palautetta niiden
tarkkuudesta. Kaikkiaan Soundscape on nyt saatavilla 46 kielellä, ja myös tämä dokumentaatiosivusto on
käännetty.

Egyptin arabia on yhdistetty arabiaan ja luganda on vedetty pois, sillä kummallakaan ei ollut
tarpeeksi käännettyä tekstiä ollakseen hyödyllinen.

Käännökset ovat yhteisön työtä, ja otamme mielellämme vastaan apuasi tai korjauksia, jos jokin
lukeutuu huonosti. Mitä tahansa tekstiä voi parantaa osoitteessa
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Lepotila

Lepotila on saanut **herätyksen lähdettäessä**. Kun asetat Soundscapen lepäämään, voit pyytää sitä
heräämään heti kun poistut alueelta. Tämä on hyödyllistä, kun saavut jonnekin ja haluat rauhaa siihen
asti kunnes lähdet taas liikkeelle.

### Etäisyydet ja puhe

Puhuttuja etäisyyksiä on lyhennetty ja tehty luontevammiksi, ja Soundscape siirtyy nyt suurempiin
yksiköihin, kun liikut nopeasti — maileihin tai kilometreihin pitkän jalka- tai metrilaskennan sijaan.
Kukin kieli päättää itse, miten murto-osaetäisyys sanotaan; aiemmin tämä oli pakotettu englannin
muotoiseen kaavaan.

### Offline-kartat

Offline-kartat tulivat versiossa 1.0 ja niitä on parannettu tasaisesti:

* Ladattu kartta voidaan nyt päivittää paikallaan, kun uudempi versio on saatavilla, karttaotteen
  tietonäytöstä.
* Kartat joita ei voi käyttää — esimerkiksi vioittunut lataus — merkitään nyt selvästi sen sijaan että
  ne epäonnistuisivat hiljaisesti.
* Lataukset ovat luotettavampia, ja näyttö kertoo mitä tapahtuu saatavilla olevien karttojen listaa
  haettaessa, koko näytön latausanimaation sijaan.
* Valmis lataus näkyy valmiina vasta kun se on todella käyttövalmis.
* Tällä sivustolla on
  [kartta saatavilla olevista alueista]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Saavutettavuus

Ruudunlukijoiden toimintaan on tehty erittäin paljon työtä, erityisesti aloitusnäytöissä, joissa
kohdistus hyppäsi aiemmin väärään paikkaan. Muita parannuksia ovat tiedostokokojen ja desimaalilukujen
parempi lukeminen, oikeat »kaksoisnapauta ...« -vihjeet kielissä joissa verbi on lopussa, sekä
järkevät vihjeet siellä missä niitä ei ollut lainkaan.

### Valikot ja navigointi

* **Poistu Soundscapesta** on nyt päävalikon ensimmäinen kohta sen sijaan että olisi alempana.
* Päävalikko ei enää jätä näkyviin kaistaletta näytöstä toiselle reunalle, mikä antoi ruudunlukijan
  käyttäjille hämmentävän ylimääräisen kosketusalueen.
* Järjestelmän paluuele ei enää ohita tasoa, kun selaat luokkia Lähellä olevat paikat -näkymässä.
* *Ääniopas* on nimetty uudelleen **ohjatuksi oppaaksi**.
* Asetukset on siistitty, ja *Palauta oletusarvot* tyhjentää nyt kaiken kunnolla.

### Vakaus

Versio 2.0 sisältää pitkän listan korjattuja kaatumisia ja jumiutumisia, muun muassa sovelluksen
jumiutumisen aloitusnäyttöön, jumiutumiset asetuksia palautettaessa, kaatumiset vioittuneen ladatun
kartan kanssa, kaatumiset avattaessa reitin tietoja aloitusnäytöstä, kaatumiset kieltä vaihdettaessa
sekä useita Play Kaupan kautta automaattisesti raportoituja ongelmia. Myös akun ja käynnistyksen
käyttäytymistä on tehty kestävämmäksi puhelimissa, jotka sulkevat taustasovelluksia aggressiivisesti.

### Poistetut ominaisuudet
{: #things-that-have-been-removed }

* **Ääniohjaus** on poistettu. Se ei koskaan toiminut riittävän luotettavasti säilytettäväksi, ja
  kuulokkeiden medianäppäimet kattavat suurelta osin saman — katso
  [Ohjeet medianäppäinten käyttöön]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Sovelluksen sisäinen kielivalikko** on poistunut. Soundscape noudattaa nyt puhelimeesi asetettua
  kieltä, mitä useimmat odottivatkin. Vaihtaaksesi sitä muuta puhelimen kieltä tai aseta
  sovelluskohtainen kieli puhelimen asetuksissa, jos se on mahdollista.

## Ongelmista kertominen

Jos jokin ei ole kohdallaan, kuulisimme siitä mielellämme. Kirjoita Help Deskiin osoitteeseen
<soundscapeAndroid@scottishtecharmy.support> tai kysy Slackissa, jos olet STA:n jäsen.

Jos kuulutus oli väärä tai jäi tulematta, matkasi tallenne auttaa meitä valtavasti — voimme toistaa
sen ja nähdä tarkalleen, mihin Soundscape perusti toimintansa. Ohjeet löytyvät kohdasta
[Sijaintitallenteen toimittaminen virheenjäljitystä varten]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Huomio iPhonesta

Kaikki edellä sanottu koskee Android-sovellusta, mutta on hyödyllistä tietää, mihin loput tämän
julkaisun työstä meni. Soundscape toimii nyt myös iPhonessa, ja molemmat sovellukset rakennetaan
samasta jaetusta koodista — samat näytöt, samat sanamuodot ja samat kuulutukset. Uusi ominaisuus kuten
yllä kuvatut matkakuulutukset saapuu näin molempiin yhtä aikaa sen sijaan että se kirjoitettaisiin
kahdesti. Tämä yhteinen perusta selittää, miksi 2.0 kesti niin kauan, ja sen pitäisi saada tulevat
julkaisut ilmestymään nopeammin molemmille alustoille. iPhone-sovellus on tällä hetkellä saatavilla
TestFlightin kautta kutsulla: kysy Slackissa, jos olet STA:n jäsen, tai kirjoita Help Deskiin.
