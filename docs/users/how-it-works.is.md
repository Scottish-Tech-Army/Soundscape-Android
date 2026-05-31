---
title: Hvernig Soundscape virkar
layout: page
parent: "Að nota Soundscape"
has_toc: false
lang: is
permalink: /users/how-it-works.html
machine-translated: true
---
# Hvernig Soundscape virkar
Markmið þessarar síðu er að gefa almennan skilning á því hvernig Soundscape-forritið virkar undir yfirborðinu. Þú þarft ekki að lesa þetta til að nota forritið, en það eru nokkrar ástæður fyrir því að þetta hefur verið skrifað:

1. Til að hjálpa áhugasömum nýliðum að skilja hvar takmarkanir forritsins liggja
1. Til að gefa notendum hugmynd um hvað annað gæti verið mögulegt með nýjum eiginleikum
1. Til að gefa forriturum yfirlit yfir virkni forritsins

Það eru tvær tæknieiningar sem gera forritið mögulegt, GPS og OpenStreetMap-gögn. GPS gefur okkur góða hugmynd um hvar síminn er og hvar hann hefur verið. OpenStreetMap-gögn má svo nota til að finna hvað er nálægt og við getum notað þau til að lýsa því fyrir notandanum.

## Hljóðvitar
Á flesta vegu eru þessir hlutir einfaldastir í útfærslu frá tæknilegu sjónarmiði. Ef við gerum ráð fyrir að við höfum staðsetningu símans og stefnu hans, þá getum við breytt hljóði hljóðvitans þannig að það hljómi eins og það komi úr þeirri átt. Við notum bókasafn frá Steam Audio til að framkvæma hljóðstaðsetninguna, sem notar yfirfærslufall tengt höfði (head related transfer functions) til að gefa bestu mögulegu staðsetningu hljóðsins. Það eina annað sem við gerum er að breyta hljóði hljóðvitans þannig að mismunandi hljóð er spilað eftir horninu milli stefnu notandans og staðsetningar hljóðvitans. Hornin breytast eftir því hvaða hljóðviti er valinn (Taktíl, Blossi, Ping o.s.frv.) og sumir hafa fleiri hljóð en aðrir. Og þetta eru hljóðvitar á einfaldasta stigi.

Eina viðbótarflækjustigið er forsendan um staðsetningu símans og þá átt sem notandinn beinir sér í. Skoðum þetta hvort fyrir sig.

### Staðsetning
Staðsetningin sem GPS skilar getur haft talsvert mikla skekkju, og þetta fer eftir því hversu mikið af himninum er sýnilegt GPS-tæki símans og hversu mörg tré og háar byggingar endurkasta GPS-merkinu á leiðinni til símans.

Aðferðin sem við höfum valið til að sía staðsetninguna er að nota það sem kallast kortasamsvörun (map matching). Þetta gerir ráð fyrir að notandinn sé líklegast á ferð eftir kortlagðri leið eða vegi - við notum hugtakið „Leið“ (Way) til að ná yfir alla vegi, slóða og stíga. Kortasamsvörun skoðar hvar notandinn hefur verið og notar stefnu hreyfingarinnar ásamt staðbundnum kortagögnum til að velja líklegustu staðsetninguna á Leið. Þessi aðferð tekur ekki aðeins tillit til skekkju frá GPS, heldur einnig skekkju í kortagögnunum. Ekki eru allar Leiðir kortlagðar nákvæmlega og því hafa þær líka skekkju. Til að ákvarða hver er líklegasta Leiðin sem notandinn er á tekur algrímið tillit til:
* Hversu nálægt Leið er GPS-staðsetningunni og fyrri GPS-staðsetningum
* Ferðastefnunni - eru þeir á hreyfingu í sömu átt og Leiðin
* Hvort það sé mögulegt að komast frá síðustu kortasamsvöruðu staðsetningunni til nýju staðsetningarinnar um net Leiða. Þetta er nauðsynlegt til að útiloka að skipt sé á milli Leiða sem eru ekki í raun tengdar, t.d. þar sem ein liggur yfir aðra um brú, eða undir hana um göng.
Kortasamsvörunin getur ákveðið að engar Leiðir séu nálægt, eða að hún sé ekki viss um hverja notandinn er á, og í því tilviki bíður hún einfaldlega eftir næstu GPS-staðsetningu og reynir aftur þar til hún er viss.

### Stefna
Það eru nokkrar stefnur sem við fylgjumst með í hugbúnaðinum:

1. Stefnan sem síminn beinist í. Við notum þetta þegar síminn er ólæstur og forritið er í notkun, en einnig þegar síminn er læstur svo lengi sem honum er haldið flötum með skjáinn vísandi upp að himni. Það er gagnlegt að hafa þetta í huga þegar þú setur símann í töskuna þína. Ef hann er settur flatur í botninn á tösku sem stendur upprétt, myndi forritið nota þá tilviljanakenndu átt sem taskan vísar í.
1. Stefnan sem síminn er á ferð í.
1. Stefnan frá heyrnartólum með höfuðrakningu (head tracking). Við notum þetta ekki sem stendur, þó að iOS-forritið hafi stutt það. Við höfum tæknina til staðar til að bæta því við í framtíðinni.

Þegar síminn er læstur og í tösku, þá notar forritið ferðastefnuna. Hins vegar, ef notandinn er ekki á hreyfingu þá er engin stefna tiltæk. Þegar þetta gerist verða hljóðvitarnir hljóðlátari til að gefa til kynna að ekki sé hægt að vita núverandi stefnu hljóðvitans - notandinn gæti verið að snúa sér við án þess að breyta staðsetningu.

Fyrir sumar notkanir á stefnu í forritinu er stefnan „smelld“ (snapped) að stefnu kortasamsvöruðu Leiðarinnar, svo ef notandinn gengur nokkurn veginn í stefnu Leiðarinnar þá er gert ráð fyrir að raunveruleg stefna Leiðarinnar sé rétt og hún notuð í þeim útreikningum.

### Niðurstaða
Þótt hljóðvitarnir virðist við fyrstu sýn einfaldir, þá kynnir notkun kortasamsvörunar, til að reyna að fjarlægja skekkjur í staðsetningu og stefnu, talsverða flækju.

## Kortagögn
Kortagögnin sem forritið notar eiga nær öll uppruna sinn í OpenStreetMap-verkefninu. Við rekum netþjón sem inniheldur kort af öllum heiminum á mörgum aðdráttarstigum. Hverju aðdráttarstigi er skipt upp í reiti (tiles). Aðdráttarstig 0 inniheldur 1 reit, stig 1 inniheldur 4 reiti, stig 2 inniheldur 16 reiti og svo framvegis upp í stig 14 sem inniheldur um 268 milljónir reita til að þekja plánetuna. Hver reitur inniheldur mörg lög og hvert lag hefur punkta, línur og marghyrninga sem hægt er að teikna til að búa til myndrænt kort. Það myndræna kort er það sem birt er notandanum í myndvitsmunaviðmóti forritsins. Hver punktur, lína og marghyrningur hefur lýsigögn sem lýsa því hvað það er. Þetta kemur að mestu beint úr OpenStreetMap-gögnunum þannig að lína gæti verið `footway` sem er `sidewalk` eða `road` sem er `minor`

Gögnunum er breytt í myndræna kortið með „stíl“ (style) sem hefur reglur um hvernig á að teikna mismunandi punkta, línur og marghyrninga í hverju lagi, t.d. hvernig á að teikna stíg, hvernig á að teikna skóg, hvernig á að teikna strætóstopp. Reglurnar geta verið mismunandi eftir aðdráttarstigi, sem er ástæðan fyrir því að þegar þú dregur að verða fleiri og fleiri punktar og línur sýnilegar sem eru ekki sýnilegar þegar dregið er frá, t.d. strætóstopp og stígar.

Með því að breyta stílnum getum við breytt því hvernig viðmótskortið lítur út, og þaðan kemur „aðgengilega kortið“ sem við erum að prófa. Því er ætlað að hafa meiri birtuskil og djarfari línur og texta. Stíllinn er innbyggður í forritið, svo við þurfum ekki að breyta kortinu á netþjóninum til að breyta því hvernig það lítur út.

En hvernig notum við kortagögnin fyrir hljóð?

### Notkun kortagagna fyrir hljóð
Við notum sem stendur tiltölulega lítið magn af kortagögnunum til að búa til hljóðviðmótið. Nær allt hljóðviðmótið notar aðeins reitina á hæsta aðdráttarstigi. Forritið saumar saman 2 sinnum 2 rist af reitum í kringum þar sem notandinn er og skoðar svo aðeins fá af lögunum:

* `transportation` - fyrir allar tegundir Leiða þar á meðal vegi, stíga, járnbrautir og sporvagnaleiðir.
* `poi` - áhugaverðir staðir, t.d. verslanir, íþróttamiðstöðvar, bekkir, póstkassar, strætóstopp o.s.frv.
* `building` - þetta er fyrir `poi` sem eru kortlagðir sem meira en bara punktur, t.d. stórir matvörumarkaðir eða ráðhús.

Það tengir saman línur og marghyrninga yfir reitamörkin og breytir öllum Leiðunum í tengda Leiðarbúta og gatnamót. Þetta er mikilvægt því það gerir okkur kleift að leita eftir Leið til að komast að því hvert við getum farið.

Öll greindu gögnin eru einnig sett í snið sem auðvelt er að leita í svo forritið geti auðveldlega fundið hvaða eiginleikar kortsins eru nálægir. Á þessum tímapunkti eru gögnin flokkuð í flokka. Núverandi flokkar eru:

* Vegir
* Vegir og stígar (allar Leiðir)
* Gatnamót - punktarnir þar sem Leiðir mætast
* Inngangar - þetta eru punktar á byggingu sem hafa verið merktir sem inngangur.
* Gangbrautir - gangbrautir yfir vegi
* Áhugaverðir staðir (POIs) - allir áhugaverðir staðir
* Almenningssamgöngustopp - strætóstopp, járnbrautarstöðvar, sporvagnastopp og svo framvegis.
* Undirflokkar áhugaverðra staða (POIS):
  * Upplýsingar
  * Hlutur
  * Staður
  * Kennileiti
  * Hreyfanleiki
  * Öryggi
* Byggðir og undirflokkar (sjá næsta kafla)
  * Borgir
  * Bæir
  * Þorp
  * Smáþorp

 Þegar þetta er til staðar getur forritið, fyrir hvaða staðsetningu sem er, auðveldlega fundið

 * „Öll almenningssamgöngustopp innan 50m“ eða
 * „Næstu gatnamót fyrir framan mig“ eða
 * „Næsta smáþorp, þorp, bæ eða borg“

Þegar þetta er til staðar er það að búa til hljóðtilkynningarnar bara spurning um að gera fyrirspurn í gögnin út frá núverandi staðsetningu og stefnu. Þegar notandinn færist yfir reitarist uppfærir hún sig svo hún sé miðuð við núverandi staðsetningu.

### Meiri gögn
Eitt af vandamálunum við mjög staðbundnu kortagagnaristina okkar er að það þýðir að við getum í mesta lagi „séð“ um 1km í hverja átt. Það er í lagi þegar við erum að lýsa því sem er fyrir framan okkur, en stundum viljum við gefa meira samhengi. Helsta dæmið um þetta er þegar forritið er notað og notandinn er ekki að ganga.

Þegar forritið greinir að notandinn er á ferð á meira en 5 metra hraða á sekúndu, breytir það því hvernig það lýsir heiminum. Í stað þess að tilkynna um hver einustu gatnamót og áhugaverðan stað tilkynnir það sjaldnar og aðeins um nálæga vegi. Vandamálið við þetta er að það er ekki mjög gagnlegt að vita nafn á vegi ef þú veist ekki í hvaða bæ hann er.

Til að reyna að bregðast við þessu greinum við nú einnig kortagögnin á lægra aðdráttarstigi og drögum út gögn úr `place` laginu. Þetta inniheldur nöfn bæja, borga, hverfa, þorpa og svo framvegis. Eitt vandamál við að kortleggja hluti er að það eru ekki alltaf augljós mörk milli þessara staða. OpenStreetMap hefur stundum borgarmörk í gagnagrunninum sínum, en jafnvel þegar svo er þá tapast þær upplýsingar oft þegar þær komast inn í reitaskipta kortið okkar. Það sem við höfum er staðsetningin þar sem nöfn staðanna eru teiknuð á kortið. Þessi eru flokkuð og svo finnur forritið næsta smáþorp, þorp, bæ eða borg við notandann og tilkynnir það.

Fyrir margar borgir verður raunverulegt borgarnafn aldrei tilkynnt, því flestum borgum er skipt í smærri einingar eins og hverfi, en þau gefa aukasamhengi sem er mjög gagnlegt. Mundu bara að þótt forritið tilkynni að þú sért nálægt götu í tilteknu hverfi þá þýðir það bara að merkimiðinn fyrir það hverfi er næsti punktur og hann gæti verið rangur eða jafnvel handan við á.

### Meira samhengi
Því meira samhengi sem hægt er að bæta við lýsingar, því betra, svo lengi sem það er haldið hnitmiðuðu og fyrirsjáanlegu. Eitt af vandamálunum sem við sáum við að lýsa gatnamótum er að oft áttu „nafnlausar“ Leiðir hlut að máli. Þetta eru Leiðir sem hafa ekkert nafn. Í kortagögnunum gætu þetta verið bara slóði, stígur eða þjónustuvegur, en án meira samhengis er það ekki mjög gagnlegt í textalýsingum. Sem betur fer getum við gert betur, svo það sem forritið gerir er að í hvert sinn sem það er við það að tilkynna um nafnlausa Leið athugar það hvort það geti fundið út meira samhengi fyrir hana.

* **Er það gangstétt?**
Mörg svæði í OpenStreetMap hafa nú gangstéttir kortlagðar aðskildar frá vegum. Þessar eru yfirleitt merktar sem `sidewalk` en þær segja venjulega ekki hvaða vegur það er sem þær eru gangstéttin við.

    Þegar forritið rekst á nafnlausa gangstétt leitar það að vegi sem það heldur að liggi við hliðina á henni og notar hann til að nefna gangstéttina. Þetta reynist mjög mikilvægt fyrir tilkynningarnar okkar. Í stað þess að tilkynna um hver einustu gangstéttargatnamót, þegar við förum eftir gangstétt eru tilkynningarnar gerðar eins og við værum á ferð eftir tengda veginum. Í stað *„Á ferð í vestur eftir stíg“* fáum við *„Á ferð í vestur eftir Moor Road“*. Notandinn er á kortlögðu gangstéttinni, en lýsingin er skiljanlegri.

* **Endar hún við nefnda Leið?**
Mjög oft eru gönguleiðir sem tengja saman tvo vegi. Með því að skoða báða enda stígsins getum við auðveldlega bætt við því samhengi þannig að í aðra áttina gæti það verið *„Stígur að Moor Road“* og þegar nálgast er frá hinum endanum gæti það verið *„Stígur að Roselea Drive“*. Þetta er aðeins gert þar sem stígurinn klofnar ekki; ef hann klofnar í tvo nafnlausa stíga þá reynum við ekki að bæta þessu samhengi við.

* **Endar hún nálægt Merki?**
Ef nafnlaus Leið byrjar eða endar nálægt Merki, þá er það notað til að lýsa henni, t.d. *„Stígur að stóra trésgatnamótum“*. Notandinn getur bætt Merkjum við hvar sem hann vill, og með því að bæta Merkjum við stíganet getur hann bætt samhengi við heila leið.

* **Liggur hún inn í eða út úr áhugaverðum stað (POI)?**
Ef nafnlaus Leið byrjar utan áhugaverðs staðar og endar inni í honum (eða öfugt) þá getum við bætt því samhengi við, t.d. *„Slóði að Lennox Park“*.

* **Endar hún nálægt Inngangi?**
Ef nafnlaus Leið byrjar eða endar nær Inngangi þá getum við bætt því samhengi við, t.d. *„Þjónustuvegur að Best Buy“*.

* **Endar hún nálægt Kennileiti eða Stað?**
Ef nafnlaus Leið byrjar eða endar nær Kennileiti þá getum við einnig bætt því samhengi við, t.d. *„Þjónustuvegur að St. Giles Cathedral“*.

* **Er það botnlangi?**
Forritið merkir sem botnlanga allar nafnlausar Leiðir sem leiða ekki neitt.

* **Liggur hún framhjá einhverjum þrepum?**
Ef nafnlausa Leiðin liggur yfir brú, í gegnum göng eða upp/niður þrep þá er þetta tekið fram og bætt við samhengið. Þetta er aðskilið frá áfangastaðamerkingunni svo samhengi eins og *„Stígur yfir brú að Lennox Park“* er mögulegt.

Þessu samhengi er bætt við í röð og því er hægt að hafa *„Stígur að Park Lane um þrep“* í aðra áttina og *„Stígur að Lennox Park um þrep“* í hina áttina. Nefnda gatan fær forgang þegar farið er frá garðinum, en garðurinn er notaður þegar farið er inn í hann.

#### Framtíðarsamhengi
Það eru ýmis viðbótarsamhengi sem við vonumst til að bæta við í framtíðinni, þar á meðal:
* Samhengi fyrir Leiðir sem fylgja línulegum vatnaeinkennum, t.d. *„Stígur við hlið River Dee“*
* Samhengi fyrir Leiðir sem fylgja jaðri vatnasvæða *„Stígur við hlið Milngavie reservoir“*
* Samhengi fyrir Leiðir sem fylgja járnbrautum, t.d. *„Stígur við hlið járnbrautar“*. Þetta gæti jafnvel innihaldið nafn járnbrautarlínunnar
* Bæta auknu innihaldi við brýr og göng, hvað eru þær yfir eða undir, t.d. *„Stígur um brú yfir járnbraut að Moor Road“*

## Hljóðtilkynningar
Nú þegar við höfum kortagögnin á sniði sem við getum auðveldlega notað er það að búa til tilkynningar í raun nokkuð einfalt.

### Tilkynningar þegar gengið er
Þegar gengið er eru hljóðtilkynningarnar sem geta orðið (í forgangsröð):

1. Lýsa hversu langt í burtu núverandi áfangastaður er
1. Lýsa væntanlegum gatnamótum
1. Lýsa 5 næstu áhugaverðu stöðum

Allar tilkynningarnar eru takmarkaðar að tíðni svo þær endurtaki sig ekki of oft. Ef notandinn hættir að hreyfa sig þá hætta tilkynningarnar, og jafnvel á hreyfingu mun tilkynning ekki endurtaka sig við hverja nýja GPS-staðsetningu. Tíðnin, eins og í iOS-forritinu, er:

* Á 60 sekúndna fresti fyrir núverandi áfangastað
* Á 30 sekúndna fresti fyrir væntanleg gatnamót
* Á 60 sekúndna fresti fyrir áhugaverðan stað

Hægt er að sía tilkynningar í gegnum stillingavalmyndina, og það er vissulega svigrúm til að víkka út þessa hegðun.

### Tilkynningar þegar ferðast er hraðar
Þegar ferðast er á meira en 5 metra hraða á sekúndu á tilkynningin um núverandi áfangastað sér enn stað, en gatnamóta- og áhugaverðra-staða-tilkynningarnar eru leystar af hólmi með tilkynningu sem lýsir nokkurn veginn hvar notandinn er. Þetta gefur nálægt almenningssamgöngustopp, áhugaverðan stað sem inniheldur okkur, t.d. inni í stórum garði, eða nálægan veg og byggð. Þessar nota gögnin sem lýst var áður, og það er augljóst svigrúm til að leyfa sérstillingu á þessu í framtíðinni.

## Merki og leiðir
Að mestu leyti eru merki og leiðir bara eiginleiki notendaviðmóts sem reiðir sig hvorki á GPS né í raun á kortagögn. Merki eru nefndar staðsetningar sem notandinn vill geyma, og leiðir eru raðaður listi af þeim merkjum. Notendaviðmótið til að búa hvort tveggja til er tekið beint úr iOS-útgáfunni.

### Spilun leiðar
Spilun leiðar er þar sem leiðir lifna við. Þegar leið er spiluð er hljóðviti búinn til við fyrsta merkið á leiðinni. Þegar notandinn kemst nálægt því merki færir leiðin sjálfkrafa hljóðvitann á næsta merki á leiðinni. Ef það eru engin fleiri merki, þá lýkur spilun leiðarinnar.

## Niðurstaða
Vonandi hefur þetta gefið nokkra innsýn í hvernig forritið virkar. Forritið er alltaf í þróun út frá endurgjöf frá notendum svo vinsamlegast hafðu samband ef það er eitthvað sem þú telur að mætti bæta við.
