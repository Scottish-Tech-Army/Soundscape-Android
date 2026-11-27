---
title: Versijų aprašai
layout: page
nav_order: 5
has_toc: false
lang: lt
permalink: /release-notes.html
machine-translated: true
---

# Versijų aprašai

Soundscape 2.0 – didelė versija, kuri dabar „Android“ sistemoje yra atvirojo beta testavimo etape. Tai reiškia, kad beta
versiją gali gauti bet kas, tiesiog atidarę [Soundscape Play parduotuvėje](https://play.google.com/store/apps/details?id=org.scottishtecharmy.soundscape).
Svarbiausias pokytis – Soundscape dabar turi ką naudingo pasakyti ir tada, kai važiuojate automobiliu, autobusu ar
traukiniu, o ne tik einant pėsčiomis. Be to, daug smulkesnio darbo atlikta aprašant vietas, pridėta
dvidešimt naujų kalbų ir ištaisyta daugybė klaidų.

Senesnių versijų aprašai pateikti puslapyje
[1.x versijų aprašai]({{ "/v1.0-release-notes.html" | relative_url }}).

## Pakeitimai nuo pirmosios oficialios versijos 2.0.95

Ištaisėme kelias problemas, apie kurias buvo pranešta po 2.0.95 versijos, todėl pranešimai patikimesni, o
adresai tikslesni:

* Pėsčiųjų pranešimai prasideda greičiau, kai išlipate iš autobuso ar automobilio, o Soundscape greičiau
  pastebi, kad jūsų traukinys išvažiavo iš stoties.
* Pranešimai apie sankryžas aiškesni ir tikslesni, taip pat kai einate šaligatviu, kuris žemėlapyje
  pažymėtas atskirai nuo kelio. Apie dvi kelių metrų atstumu viena nuo kitos esančias sankryžas, vedančias
  į tą pačią vietą, dabar pranešama tik vieną kartą.
* Soundscape praneša, kai kelias baigiasi vartais, pro kuriuos negalite praeiti.
* Takai prie stočių aprašomi geriau, pavyzdžiui, *Takas, veda į: Peronas*, o bendro naudojimo takai, tokie kaip
  takai palei kanalus, paskelbiami jums į juos įėjus, užuot laikius juos kelio šaligatviu.
* *Mano vieta* nebenutyla esant prastam interneto ryšiui.
* Ištaisyta žemėlapių be interneto atsisiuntimo problema.

Kai kas veikia šiek tiek kitaip. Soundscape dabar gali naudoti telefono žingsniamatį, kad nustatytų,
kada einate, todėl greičiau pastebi, kad išlipote iš transporto priemonės ar sustojote; pirmą kartą
atidarius šią versiją, bus paprašyta naujo leidimo *Fizinis aktyvumas* (iPhone – *Judėjimas ir fizinis
aktyvumas*). Balsai dabar sugrupuoti pagal kalbą, palietus balsą grojamas pavyzdys, o pasirinkus
*Automatiškai* naudojamas numatytasis jūsų kalbos balsas. Meniu punkto *Share recording of travel* nebeliko:
kai įjungtas nustatymas *Įjungti kelionės įrašymą*, *Kreiptis pagalbos* paklausia, ar pridėti įrašytą
kelionę, kuri dabar visada apima paskutinę valandą.

## Kas naujo 2.0 versijoje

* **Pranešimai važiuojant automobiliu, autobusu ar traukiniu.** Soundscape atpažįsta, kad judate dideliu
  greičiu, ir aprašo jūsų kelionę, o ne artimiausią aplinką.
* **Pranešama, kai kertate vandenį ir geležinkelį.** Apie upes, kanalus, įlankas ir geležinkelio linijas
  pranešama jas kertant, nesvarbu, ar einate pėsčiomis, ar važiuojate.
* **Pasirinkite, kiek Soundscape kalba.** Naujas nustatymas *Pranešimų išsamumas* leidžia Soundscape
  tyliau elgtis judriose vietose, o *Apie kurias vietas pranešti* leidžia pasirinkti, apie kokias vietas
  girdite. Išsamumą galite keisti einant, ausinių mygtukais.
* **Sužinokite, kiek toli kita sankryža.** Apie sankryžas pranešama vienodu atstumu, kai artėjate, o
  pranešime dabar pasakoma, kiek toli yra šaligatvio kraštas.
* **Ieškokite vietos tipo arba koordinačių.** Ieškokite „vaistinė“ arba „autobusų stotelė“ ir rasite
  artimiausias, nesvarbu, kaip jos vadinasi, arba įklijuokite koordinates, žemėlapio nuorodą ar plus kodą.
* **Atskiri Žymių ir Maršrutų mygtukai.** Ekranas *Žymės ir maršrutai* su dviem skirtukais padalytas į
  ekraną *Žymės* ir ekraną *Maršrutai*, kurių kiekvienas turi savo mygtuką pagrindiniame ekrane. Žymių
  ekrane yra mygtukas *Kurti*, kuriuo galite išsaugoti dabartinę vietą ar netoliese esančią vietą.
* **Atidarykite vietą kitoje žemėlapių programoje**, pavyzdžiui, Google Maps, iš *Vietos informacija* ar sąrašų.
* **Daugiau apie švyturį pagrindiniame ekrane.** Dabar rodomas atstumas ir kryptis, o ekrano skaitytuvo
  veiksmais galima išgirsti, kur švyturys, sužinoti apie jį daugiau arba išsaugoti jį kaip žymę.
* **Geresni adresai ir vietų pavadinimai.** Vietos, neturinčios savo adreso, dabar gauna gatvę ir
  vietovę, kurioje yra, namų numeriai priskiriami teisingai gatvės pusei, o autobusų stotelės
  Didžiojoje Britanijoje vadinamos oficialiais pavadinimais.
* **Dvidešimt naujų kalbų**, iš viso jų 46. Išverstas ir dokumentacijos tinklalapis.
* **Pažadinti išėjus.** Kai užmigdote Soundscape, naujas mygtukas *Pažadinti išėjus* programėlę
  tik užsnūdina, ir ji pati pabunda, kai išeinate.
* **Trumpesni, natūralesni atstumai**, didesniais vienetais, kai judate greitai.
* **Greitesnis išėjimas.** *Išjungti Soundscape* dabar yra pagrindinio meniu viršuje.
* **Žemėlapių be interneto patobulinimai**, įskaitant atsisiųsto žemėlapio atnaujinimą vietoje ir
  šiame tinklalapyje esantį prieinamų regionų žemėlapį.
* **Daug prieinamumo darbų** TalkBack srityje, ypač pirmojo paleidimo ekranuose.
* **Labai daug strigčių ir stabilumo pataisymų.**

Balso valdymo funkcija 2.0 versijoje **pašalinta**. Ką daryti vietoj jos, žr. skyrių
[Kas pašalinta](#things-that-have-been-removed) žemiau.

---

## Išsamiau

### Kelionė automobiliu, autobusu ar traukiniu

Tai didžiausia nauja funkcija esamiems naudotojams. Anksčiau Soundscape turėjo labai mažai ką pasakyti,
kai įlipdavote į transporto priemonę – programėlė toliau aprašydavo artimiausią aplinką, o dideliu greičiu
tai reiškė srautą dalykų, pro kuriuos jau buvote pravažiavę.

Dabar Soundscape pastebi, kad judate greičiau nei einant, ir keičia tai, ką jums sako. Nieko įjungti
nereikia, ir viskas savaime grįžta į įprastą režimą, kai sulėtėjate arba išlipate ir einate pėsčiomis.

Važiuodami išgirsite:

* **Kur esate**, kas kurį laiką – kuriuo keliu važiuojate ir kokia kryptimi, pavyzdžiui,
  „Važiuojate į šiaurę: M8“. Numeruoti keliai paskelbiami numeriu, ir Soundscape nekartoja to paties kelio
  kaskart, kai pasikeičia jo gatvės pavadinimas. Kelias paskelbiamas tik tada, kai juo pavažiuojate
  trumpą atstumą, todėl posūkių seka per gyvenamąjį kvartalą nesukelia pranešimo kiekviename kampe.
* **Miestus ir kaimus**, link kurių važiuojate, su atstumu, taip pat tuos, nuo kurių tolstate arba pro
  kuriuos tiesiog pravažiuojate.
* **Greitkelio sankryžas ir nuovažas**, kai prie jų privažiuojate, toje važiuojamojoje dalyje, kuria
  važiuojate – ne tas, kurios yra šalia esančiame kelyje ar kitoje greitkelio pusėje.
* **Didelius orientyrus**, pro kuriuos važiuojate, pavyzdžiui, parkus, ligonines, stadionus ir prekybos centrus.
* **Autobusų, tramvajų ir traukinių stoteles**, pro kurias važiuojate. Soundscape mini tik stoteles jūsų
  kelio pusėje, nes priešingoje pusėje esančios aptarnauja priešingą kryptį.
* **Upes, kanalus ir geležinkelius, kuriuos kertate.**
* **Tunelius** – daugiausia tam, kad būtų aišku, kodėl Soundscape tuoj nutils: tunelyje nėra GPS signalo.

**Traukinyje** Soundscape nustato, kad esate geležinkelyje, o ne kelyje, ir praneša, pro kuriuos miestus
važiuojate ir kiek nuvažiavote nuo paskutinės stoties. Tai nustatyti sunkiau, nei atrodo, nes greitkeliai
ir geležinkelio linijos dažnai nutiesti greta vienas kito daugelį kilometrų, todėl nemažai šios versijos
darbo skirta tam, kad vienas nebūtų supainiotas su kitu.

Įprasti pėsčiųjų pranešimai – apie netoliese esančias parduotuves, perėjas ir pan. – važiuojant
sąmoningai sulaikomi, o atstumai, kuriais apie objektus pranešama, gerokai padidinami, kad apie objektą
išgirstumėte prieš jį pravažiuodami.

### Sankryžos

Dažniausiai apie pranešimus apie sankryžas būdavo klausiama, kiek toli sankryža iš tikrųjų yra.
Dabar Soundscape tai pasako: „Iki sankryžos 30 metrų“. Atstumas matuojamas iki kelio, kurį ketinate
kirsti, šaligatvio krašto, o ne iki sankryžos vidurio, nes ten iš tikrųjų sustosite.

Pranešimas taip pat pasigirsta labiau nuspėjamu momentu. Anksčiau jis galėjo pasigirsti už 45 metrų arba
už 10 metrų, ir nebuvo kaip jų atskirti. Dabar jis laukia, kol iki sankryžos lieka maždaug 30 metrų, todėl
atstumas kiekvieną kartą reiškia maždaug tą patį.

### Vandens ir geležinkelių kirtimas

Dabar Soundscape praneša, kai kertate upę, kanalą, įlanką ar geležinkelio liniją. Tai veikia ir einant
pėsčiomis, ir važiuojant, ir apima ne tik ėjimą virš, bet ir po apačia, todėl aprašomas ir pėsčiųjų
tiltas, ir požeminė perėja.

### Kiek Soundscape kalba

Dažniausiai apie Soundscape girdime, kad judriose vietose, pavyzdžiui, miesto centre, programėlė kalba
per daug. *Nustatymų* skyriuje *Pranešimų valdymas* vietoj senojo jungiklių sąrašo dabar yra trys
nustatymai:

* **Pranešimų išsamumas**: Tylus, Esminis, Supaprastintas arba Išsamus. *Išsamus* – tai, ką Soundscape
  darė visada, ir nuo to pradedama. *Supaprastintas* praleidžia smulkius takus ir aptarnavimo kelius ir
  rečiau kartoja. *Esminis* praneša tik apie gatves, sankryžas ir orientyrus. *Tylus* išjungia visus
  automatinius pranešimus, o švyturiai, maršrutai ir pagrindinio ekrano mygtukai toliau veikia.
  Jis pakeičia senąjį jungiklį *Leisti pranešimus*, ir jei jį buvote išjungę, Pranešimų išsamumas bus
  nustatytas į Tylus. Lygiai galioja ir važiuojant: *Supaprastintas* rečiau aprašo kelią, kuriuo važiuojate,
  o *Esminis* įvardija tik pagrindinius ir numeruotus kelius ir praleidžia autobusų stoteles.
* **Gatvės ir sankryžos** įjungia arba išjungia pranešimus apie sankryžas ir kelią, kuriuo einate.
* **Apie kurias vietas pranešti** – sąrašas, kuriame galite pažymėti: Viskas, Orientyrai, Viešasis
  transportas, Maistas ir gėrimai, Maisto ir smulkių prekių parduotuvės, Bankai ir bankomatai arba Jokių
  vietų. Pažymėkite, kiek norite – pavyzdžiui, orientyrus ir autobusų stoteles. Apie jūsų žymes pranešama visada.

Tinkamas išsamumas einant keičiasi, todėl jo keisti nereikia eiti į Nustatymus. Paspaudus ausinių mygtuką
*ankstesnis*, Pranešimų išsamumas sumažinamas vienu lygiu: nuo Išsamaus per Supaprastintą ir Esminį iki
Tylaus, o tada vėl į Išsamų. Kaskart pasakomas naujas lygis. Tai veikia abiem medijos valdymo režimais, todėl
ausinių mygtukai šiek tiek pasikeitė:

* Režimu *Pradinis* mygtukas *kitas* dabar paskelbia *Aplink mane*, kai maršrutas neleidžiamas, o *Mano vieta*
  mygtukais nebepasiekiama. Kai maršrutas leidžiamas, *kitas* ir *ankstesnis* toliau perjungia maršruto taškus.
* Režimu *Garsinis meniu* mygtukas *ankstesnis* nebegrąžina atgal per meniu. *Kitas* toliau juda per meniu, o
  *leisti arba pristabdyti* toliau pasirenka. Žymės ir maršrutai meniu dabar išvardijami pavadinimais, o
  pradėjus vieną iš jų meniu grįžta į pradžią, o ne palieka jus giliai sąraše.

### Paieška

Paieškos juosta dabar supranta ne tik vietų pavadinimus:

* **Vietų tipai.** Ieškokite „vaistinė“, „tualetas“, „bankomatas“ ir pan. savo kalba, ir Soundscape
  išvardys artimiausias tokio tipo vietas, nesvarbu, kaip jos vadinasi. Vietos be pavadinimo, kaip
  dauguma tualetų ir suolų, išvardijamos pagal tai, kas jos yra, su adresu.
* **Koordinatės, žemėlapių nuorodos ir plus kodai.** Įklijuokite skaičių porą, laipsnius ir minutes,
  nuorodą iš Google Maps, Apple Maps ar OpenStreetMap arba plus kodą, ir Soundscape pateiks tą tikslią
  vietą. Vien skaičių porą galima perskaityti abiem kryptimis, todėl, kai prasmingi abu variantai,
  pasiūlomi abu, artimesnis pirmas.
* **Paieška be interneto.** Paieška dabar visada ieško ir atsisiųstuose žemėlapiuose, ne tik internete,
  todėl randama kur kas daugiau vietų be pavadinimo. Jei ieškote be interneto ryšio ir neturite tos
  vietovės žemėlapio be interneto, Soundscape tai pasako, užuot tiesiog nieko neradusi.

### Vietos atidarymas kitoje programoje

*Vietos informacija* turi naują mygtuką **Atidaryti žemėlapių programoje**, kuris išvardija jūsų telefone
esančias žemėlapių ir navigacijos programas. Pažymėkite *Visada naudoti šią programą*, ir mygtukas
pasikeis, pavyzdžiui, į *Atidaryti programoje Google Maps* ir iškart ją atidarys; ilgai paspaudus sąrašas
vėl parodomas. Sąrašuose *Vietos netoliese* ir *Žymės* taip pat yra ekrano skaitytuvo veiksmai
atidarymui kitoje programoje ir *Bendrinti*.

### Žymės ir maršrutai

Žymės ir maršrutai anksčiau dalijosi vienu ekranu *Žymės ir maršrutai*, kuriame kiekvienas turėjo savo
skirtuką, ir buvo lengva pamiršti, kuriame skirtuke esate. Dabar tai du atskiri ekranai, kurių kiekvienas
turi savo mygtuką pagrindiniame ekrane: *Žymės* ir *Maršrutai*, iškart po *Vietos netoliese*.

Kiekviename ekrane viršuje dešinėje yra mygtukas *Kurti*. Maršrutų ekrane jis sukuria maršrutą, kaip ir
anksčiau. Žymių ekrane jis naujas: jame išvardijama *Dabartinė vieta* ir *Vietos netoliese* kategorijos,
todėl žymę galite išsaugoti negrįždami į pagrindinį ekraną pirmiausia surasti vietos. Ekrano skaitytuvai
šiuos mygtukus skelbia kaip *Nauja žymė* ir *Naujas maršrutas*.

### Švyturys ir žymės

* Švyturys pagrindiniame ekrane dabar rodo **atstumą ir kryptį**, o ekrano skaitytuvas jį perskaito,
  pavyzdžiui, „Švyturys: Milngavie biblioteka, 390 metrų, pietryčiai“. Maršrutuose taip pat rodomas
  atstumas iki dabartinio maršruto taško.
* Švyturys turi tris **ekrano skaitytuvo veiksmus**: *Pasakyti, kur švyturys* pasako, kur jis yra,
  *Daugiau informacijos* prideda gatvės adresą, o *Pridėti prie žymių* jį išsaugo.
* Vėl galite **perkelti žymę**, vilkdami žemėlapį ekrane *Redaguoti žymę*.

### Geresni adresai ir vietų pavadinimai

Daug darbo skirta tam, kad Soundscape aprašytų vietas taip, kaip tai darytų žmogus:

* Vietos, neturinčios savo adreso, dabar aprašomos pagal gatvę ir vietovę, kurioje yra, o ne paliekamos
  neaiškios.
* Namų numeriai priskiriami teisingai gatvės pusei. Anksčiau adresas galėjo būti pateiktas nuo priešingo
  šaligatvio.
* Vietos adresas nebekartoja jums pačios vietos pavadinimo.
* Autobusų stotelės Didžiojoje Britanijoje vadinamos oficialiais viešojo transporto pavadinimais, kurie
  paprastai būna tvarkaraštyje ir ant stotelės ženklo.
* Bevardžiai pėsčiųjų takai, einantys palei upę ar kanalą, dabar vadinami pagal vandenį, kurį jie lydi.
* Takai ir keliai be pavadinimo aprašomi protingiau, o jiems naudojami žodžiai tinkamai išversti, o ne
  rodomi angliškai.

### Kalbos

2.0 versijoje pridėta dvidešimt naujų kalbų: arabų, bengalų, bulgarų, katalonų, kroatų, čekų, hausų,
vengrų, indoneziečių, korėjiečių, maratų, serbų, slovakų, slovėnų, suahilių, tamilų, telugų, tajų, urdu ir
vietnamiečių. Visos šios kalbos yra alfa etape, ir labai laukiame atsiliepimų apie jų tikslumą. Iš viso
Soundscape dabar prieinama 46 kalbomis, išverstas ir šis dokumentacijos tinklalapis.

Egipto arabų kalba sujungta su arabų kalba, o ugandų (luganda) kalba pašalinta, nes nė vienoje nebuvo
pakankamai išversto teksto, kad būtų naudinga.

Vertimai – bendruomenės darbas, ir labai džiaugtumėmės jūsų pagalba ar pataisymais, kur kas nors skamba
prastai. Bet kurią eilutę galima patobulinti adresu
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Miego režimas

Miego režimas turi naują mygtuką ***Pažadinti išėjus***. Jį pasirinkus Soundscape užsnūsta: programėlė
tyli, kol išeinate iš vietovės, o tada pati pabunda. Tai naudinga, kai kur nors atvykstate ir norite
ramybės, kol vėl išeisite.

### Atstumai ir kalba

Sakomi atstumai sutrumpinti ir tapo natūralesni, o judant greitai Soundscape dabar pereina prie didesnių
vienetų – kilometrų ar mylių, o ne ilgo metrų ar pėdų skaičiaus. Kiekviena kalba pati sprendžia, kaip
pasakyti trupmeninį atstumą; anksčiau tam buvo primestas angliškas modelis.

### Žemėlapiai be interneto

Žemėlapiai be interneto atsirado 1.0 versijoje ir nuolat tobulinami:

* Atsisiųstą žemėlapį dabar galima atnaujinti vietoje, kai yra naujesnė versija, iš žemėlapio informacijos
  ekrano.
* Žemėlapiai, kurių negalima naudoti – pavyzdžiui, sugadintas atsisiuntimas – dabar aiškiai pažymimi, o ne
  tyliai neveikia.
* Atsisiuntimai patikimesni, o kol gaunamas prieinamų žemėlapių sąrašas, ekrane rodoma, kas vyksta, o ne
  per visą ekraną besisukantis indikatorius.
* Baigtas atsisiuntimas parodomas kaip baigtas tik tada, kai jis tikrai paruoštas naudoti.
* Šiame tinklalapyje yra [prieinamų regionų žemėlapis]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Prieinamumas

Daug darbo skirta ekrano skaitytuvo veikimui, ypač pirmojo paleidimo ekranuose, kur fokusas anksčiau
peršokdavo į netinkamą vietą. Kiti patobulinimai: geriau skaitomi failų dydžiai ir dešimtainiai skaičiai,
teisingos „dukart palieskite, kad…“ užuominos kalbose, kuriose veiksmažodis eina sakinio gale, ir prasmingos
užuominos ten, kur jų iš viso nebuvo.

### Meniu ir naršymas

* **Išjungti Soundscape** dabar yra pirmasis pagrindinio meniu punktas, o ne kažkur žemiau.
* Pagrindiniame meniu viename šone nebematyti ekrano juostos, dėl kurios ekrano skaitytuvo naudotojai
  turėdavo painią papildomą sritį paliesti.
* Sistemos gestas „atgal“ nebepraleidžia lygio, kai naršote kategorijas skiltyje *Vietos netoliese*.
* *Audio Tutorial* pervadinta į **Mokomoji apžvalga**.
* Nustatymai sutvarkyti, o *Atkurti numatytuosius nustatymus* dabar tinkamai išvalo viską.

### Stabilumas

2.0 versijoje ištaisyta daugybė strigčių ir užstrigimų, tarp jų programėlės užstrigimas pradžios ekrane,
užstrigimas atkuriant nustatymus, strigtys, kai atsisiųstas žemėlapis buvo sugadintas, strigtys atidarant
maršruto informaciją iš pagrindinio ekrano, strigtys keičiant kalbą ir kelios problemos, apie kurias
automatiškai pranešta per Play parduotuvę. Akumuliatoriaus ir paleidimo veikimas taip pat patikimesnis
telefonuose, kurie agresyviai išjungia fonines programas.

### Kas pašalinta
{: #things-that-have-been-removed }

* **Balso valdymas** pašalintas. Jis niekada neveikė pakankamai patikimai, kad verta būtų jį palikti, o
  ausinių medijos mygtukai atlieka didžiąją dalį to paties – žr.
  [Medijos mygtukų naudojimas]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape taip pat paruošta balso komandoms per Gemini „Android 16“ ir naujesnėse versijose, tačiau jos
  neveiks, kol Google neišleis jų palaikymo Gemini programoje.

## Kaip pranešti mums apie problemas

Jei kas nors negerai, norėtume apie tai sužinoti. Rašykite pagalbos tarnybai adresu
<{{ site.support_email }}> arba klauskite Slack, jei esate STA narys.

Jei pranešimas buvo neteisingas arba nepasigirdo, jūsų kelionės įrašas mums labai padeda – galime jį
atkurti ir pamatyti, kuo tiksliai Soundscape rėmėsi. Instrukcijos pateiktos skyriuje
[Derinimo vietos įrašo pateikimas]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace) (anglų kalba).

## Pastaba apie iPhone

Viskas, kas aprašyta aukščiau, susiję su „Android“ programėle, tačiau verta žinoti, kur dingo likęs šios
versijos darbas. Soundscape dabar veikia ir iPhone, o abi programėlės kuriamos iš to paties bendro kodo –
tie patys ekranai, ta pati formuluotė ir tie patys pranešimai, todėl nauja funkcija, kaip aukščiau aprašyti
pranešimai važiuojant, abiejose atsiranda iš karto, užuot rašius ją du kartus. Dėl šio bendro pagrindo 2.0
užtruko taip ilgai, ir būtent jis turėtų padėti būsimoms versijoms greičiau pasiekti abi platformas. iPhone
programėlė dabar yra viešojoje beta versijoje per Apple programėlę TestFlight, ir prisijungti gali bet kas:
kaip tai padaryti, žr. [Viešosios beta versijos]({{ "/" | relative_url }}#public-betas).
