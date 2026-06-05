---
title: Soundscape nasıl çalışır
layout: page
parent: "Soundscape Kullanımı"
has_toc: false
lang: tr
permalink: /users/how-it-works.html
machine-translated: true
---
# Soundscape nasıl çalışır
Bu sayfanın amacı, Soundscape uygulamasının arka planda nasıl çalıştığına dair genel bir anlayış sağlamaktır. Uygulamayı kullanmak için bunu okumanıza gerek yok, ancak yazılmasının birkaç nedeni var:

1. Uygulamayla ilgilenen yeni başlayanların sınırlamalarının nerede olduğunu anlamalarına yardımcı olmak için
1. Kullanıcılara yeni özelliklerle başka nelerin mümkün olabileceği konusunda bir fikir vermek için
1. Geliştiricilere uygulamanın işlevine genel bir bakış sağlamak için

Uygulamayı mümkün kılan iki teknoloji parçası vardır: GPS ve OpenStreetMap verileri. GPS bize telefonun nerede olduğu ve nerede bulunduğu konusunda iyi bir fikir verir. OpenStreetMap verileri daha sonra yakında ne olduğunu bulmak için kullanılabilir ve bunu kullanıcıya anlatmak için kullanabiliriz.

## Sesli işaretler
Birçok açıdan bunlar, teknoloji açısından uygulanması en basit şeylerdir. Telefonun konumuna ve telefon için bir yöne sahip olduğumuzu varsayarsak, sesli işaretin sesini o yönden geliyormuş gibi sesleneceği şekilde değiştirebiliriz. Ses konumlandırmasını gerçekleştirmek için Steam Audio'dan bir kütüphane kullanıyoruz; bu kütüphane, mümkün olan en iyi sesli konumlandırmayı sağlamak için baş ile ilgili transfer fonksiyonlarını (head related transfer functions) kullanır. Yaptığımız tek diğer şey, kullanıcı yönü ile sesli işaretin konumu arasındaki açıya bağlı olarak farklı bir ses çalınması için sesli işaret sesini değiştirmektir. Açılar, seçilen sesli işarete bağlı olarak değişir (Tactile, Flare, Ping vb.) ve bazıları diğerlerinden daha fazla sayıda sese sahiptir. İşte en basit düzeyde sesli işaretler bunlardır.

Tek ek karmaşıklık, telefonun konumu ve kullanıcının baktığı yön hakkındaki varsayımdır. Bunları sırayla inceleyelim.

### Konum
GPS tarafından döndürülen konum oldukça büyük bir hataya sahip olabilir ve bu, gökyüzünün ne kadarının telefonun GPS'ine görünür olduğuna ve telefona ulaşırken kaç ağacın ve yüksek binanın GPS sinyalini yansıttığına bağlıdır.

Konumu filtrelemek için benimsediğimiz yaklaşım, harita eşleştirme (map matching) olarak bilinen yöntemi kullanmaktır. Bu, kullanıcının büyük olasılıkla haritalanmış bir patika veya yol boyunca ilerlediğini varsayar — tüm yolları, izleri ve patikaları kapsamak için 'Way' (Yol) terimini kullanıyoruz. Harita eşleştirme, kullanıcının nerede olduğuna bakar ve hareket yönünü yerel harita verileriyle birlikte kullanarak bir Yol üzerindeki en olası konumu seçer. Bu yaklaşım yalnızca GPS'ten kaynaklanan hataları değil, aynı zamanda harita verilerindeki hataları da hesaba katar. Tüm Yollar doğru bir şekilde haritalanmamıştır ve bu nedenle onların da hataları vardır. Kullanıcının üzerinde bulunduğu en olası Yol'un hangisi olduğunu belirlemek için algoritma şunları dikkate alır:
* Bir Yol'un GPS konumuna ve önceki GPS konumlarına ne kadar yakın olduğu
* Hareket yönü - Yol ile aynı yönde mi ilerliyorlar
* Son harita eşleştirilmiş konumdan yeni konuma Yollar ağı üzerinden ulaşmanın mümkün olup olmadığı. Bu, aslında bağlı olmayan Yollar arasında geçiş yapmayı dışlamak için gereklidir; örneğin biri bir köprüyle diğerinin üzerinden veya bir tünelle altından geçer.
Harita eşleştirme, yakında hiç Yol olmadığına ya da kullanıcının hangisinde olduğundan emin olmadığına karar verebilir; bu durumda sadece bir sonraki GPS konumunu bekler ve emin olana kadar tekrar dener.

### Yön
Yazılımda izlediğimiz birkaç yön vardır:

1. Telefonun baktığı yön. Bunu telefonun kilidi açıkken ve uygulama kullanılırken, ayrıca telefon kilitliyken de ekran gökyüzüne bakacak şekilde düz tutulduğu sürece kullanırız. Telefonunuzu çantanıza koyarken bunu akılda tutmakta fayda var. Dik tutulan bir çantanın dibine düz olarak konulursa, çantanızın baktığı rastgele yön uygulama tarafından kullanılır.
1. Telefonun ilerlediği yön.
1. Baş takibi olan kulaklıklardan gelen yön. Şu anda bunu kullanmıyoruz, ancak iOS uygulaması destekliyordu. Gelecekte eklemek için teknolojiye sahibiz.

Telefon kilitli ve bir çantadayken, uygulama hareket yönünü kullanır. Ancak kullanıcı hareket etmiyorsa hiçbir yön mevcut değildir. Bu olduğunda, sesli işaretin mevcut yönünü bilmenin mümkün olmadığını belirtmek için sesli işaretler daha sessiz olur — kullanıcı konumunu değiştirmeden dönüyor olabilir.

Uygulamadaki bazı yön kullanımları için, yön harita eşleştirilmiş Yol'un yönüne 'sabitlenir'; bu nedenle kullanıcı kabaca Yol'un yönünde yürüyorsa, Yol'un gerçek yönünün doğru olduğu varsayılır ve bu hesaplamalarda kullanılır.

### Sonuç
İlk bakışta sesli işaretler basit görünse de, konum ve yön hatalarını kaldırmaya çalışmak için harita eşleştirme kullanmak hatırı sayılır miktarda karmaşıklık getirir.

## Harita verileri
Uygulama tarafından kullanılan harita verilerinin neredeyse tamamı OpenStreetMap projesinden kaynaklanır. Tüm dünyanın haritasını birden çok yakınlaştırma düzeyinde içeren bir sunucu çalıştırıyoruz. Her yakınlaştırma düzeyi karolara (tiles) bölünmüştür. Yakınlaştırma düzeyi 0, 1 karo içerir; düzey 1, 4 karo içerir; düzey 2, 16 karo içerir ve gezegeni kaplamak için yaklaşık 268 milyon karo içeren düzey 14'e kadar bu şekilde devam eder. Her karo birden çok katman içerir ve her katmanın, grafiksel bir harita oluşturmak için çizilebilecek noktaları, çizgileri ve poligonları vardır. Bu grafiksel harita, uygulama arayüzünde kullanıcıya gösterilen şeydir. Her nokta, çizgi ve poligonun ne olduğunu açıklayan meta verileri vardır. Bu çoğunlukla doğrudan OpenStreetMap verilerinden gelir; örneğin bir çizgi `sidewalk` (kaldırım) olan bir `footway` (yaya yolu) ya da `minor` olan bir `road` (yol) olabilir.

Veriler, her katmandaki farklı nokta, çizgi ve poligonların nasıl çizileceğine ilişkin kurallara sahip bir 'stil' aracılığıyla grafiksel haritaya dönüştürülür; örneğin bir patikanın nasıl çizileceği, bir ormanın nasıl çizileceği, bir otobüs durağının nasıl çizileceği. Kurallar yakınlaştırma düzeyine göre değişebilir; bu yüzden yakınlaştırdıkça, uzaklaştırıldığında görünmeyen giderek daha fazla nokta ve çizgi görünür hale gelir, örneğin otobüs durakları ve patikalar.

Stili değiştirerek arayüz haritasının nasıl göründüğünü değiştirebiliriz; denediğimiz 'erişilebilir harita' da buradan gelir. Daha fazla kontrast ile daha kalın çizgiler ve metin sunmayı amaçlar. Stil uygulamanın içine yerleştirilmiştir, bu yüzden görünümünü değiştirmek için sunucudaki haritayı değiştirmemize gerek yoktur.

Peki harita verilerini ses için nasıl kullanıyoruz?

### Harita verilerini ses için kullanma
Şu anda sesli kullanıcı arayüzünü oluşturmak için haritalama verilerinin nispeten küçük bir kısmını kullanıyoruz. Sesli arayüzün neredeyse tamamı yalnızca maksimum yakınlaştırma düzeyindeki karoları kullanır. Uygulama, kullanıcının bulunduğu yerin etrafına 2'ye 2'lik bir karo ızgarası birleştirir ve ardından yalnızca birkaç katmana bakar:

* `transportation` - yollar, patikalar, demiryolları ve tramvay yolları dahil tüm Yol türleri için.
* `poi` - ilgi noktaları, örneğin dükkanlar, spor merkezleri, banklar, posta kutuları, otobüs durakları vb.
* `building` - bu, sadece bir noktadan fazlası olarak haritalanmış `poi` içindir; örneğin büyük süpermarketler veya belediye binaları.

Çizgileri ve poligonları karo sınırları boyunca birleştirir ve tüm Yolları bağlı Yol segmentlerine ve kavşaklara dönüştürür. Bu önemlidir çünkü bir Yol boyunca arama yaparak nereye ulaşabileceğimizi bulmamıza olanak tanır.

Ayrıştırılan tüm veriler, uygulamanın haritanın hangi öğelerinin yakında olduğunu kolayca bulabilmesi için kolay aranabilir bir biçime de konulur. Bu noktada veriler kategorilere ayrılır. Mevcut kategoriler şunlardır:

* Yollar
* Yollar ve patikalar (tüm Yollar)
* Kavşaklar - Yolların kesiştiği noktalar
* Girişler - bunlar bir binada giriş olarak işaretlenmiş noktalardır.
* Geçitler - yol geçitleri
* POI'ler - tüm ilgi noktaları
* Toplu taşıma durakları - otobüs durakları, demiryolu istasyonları, tramvay durakları vb.
* POI'lerin alt kategorileri:
  * Bilgi
  * Nesne
  * Yer
  * Simge Yapı
  * Hareketlilik
  * Güvenlik
* Yerleşimler ve alt kategorileri (sonraki bölüme bakın)
  * Şehirler
  * Kasabalar
  * Köyler
  * Mezralar

 Bu sayede uygulama, herhangi bir konum için kolayca şunları bulabilir

 * "50m içindeki tüm toplu taşıma durakları" veya
 * "Önümdeki en yakın kavşak" veya
 * "En yakın mezra, köy, kasaba veya şehir"

Bu sayede, sesli anonsları oluşturmak yalnızca mevcut konum ve yöne göre verileri sorgulamaktan ibarettir. Kullanıcı bir karo ızgarası boyunca hareket ettikçe, mevcut konum etrafında merkezlenecek şekilde güncellenir.

### Daha fazla veri
Çok yerel harita verisi ızgaramızla ilgili sorunlardan biri, herhangi bir yönde en fazla yaklaşık 1 km 'görebilmemiz' anlamına gelmesidir. Önümüzde ne olduğunu anlatırken bu sorun değildir, ancak bazen daha fazla bağlam vermek isteriz. Bunun temel örneği, uygulamanın kullanıldığı ve kullanıcının yürümediği durumdur.

Uygulama, kullanıcının saniyede 5 metreden fazla hızla ilerlediğini algıladığında, dünyayı tanımlama şeklini değiştirir. Her kavşağı ve POI'yi anons etmek yerine daha seyrek ve yalnızca yakındaki yolları anons eder. Bununla ilgili sorun, bir yol adını bilmenin, onun hangi kasabada olduğunu bilmiyorsanız pek faydalı olmamasıdır.

Bunu gidermeye çalışmak için artık harita verilerini daha düşük bir yakınlaştırma düzeyinde de ayrıştırıyor ve `place` katmanından veri çıkarıyoruz. Bu, kasaba, şehir, mahalle, köy vb. adlarını içerir. Bir şeyleri haritalamayla ilgili bir sorun, bu yerler arasında her zaman belirgin bir sınır olmamasıdır. OpenStreetMap'in veritabanında bazen şehir sınırları bulunur, ancak bu durumda bile karolu haritamıza ulaştığında bu bilgi çoğu zaman kaybolur. Sahip olduğumuz şey, yer adlarının harita üzerinde çizildiği konumdur. Bunlar kategorilere ayrılır ve ardından uygulama, kullanıcıya en yakın mezra, köy, kasaba veya şehri bulur ve bunu bildirir.

Birçok şehir için gerçek şehir adı asla anons edilmeyecektir, çünkü çoğu şehir mahalle gibi daha küçük bölümlere ayrılmıştır, ancak bunlar ekstra bağlam sağladığı için çok kullanışlıdır. Sadece şunu unutmayın: uygulamanın belirli bir mahalledeki bir caddenin yakınında olduğunuzu bildirmesi, yalnızca o mahallenin etiketinin en yakın nokta olduğu anlamına gelir ve yanlış olabilir, hatta bir nehrin karşısında olabilir.

### Daha fazla bağlam
Açıklamalara eklenebilecek bağlam ne kadar fazlaysa o kadar iyidir, yeter ki kısa ve öngörülebilir tutulsun. Kavşakları tanımlarken gördüğümüz sorunlardan biri, çoğu zaman 'adsız' Yolların söz konusu olmasıydı. Bunlar adı olmayan Yollardır. Haritalama verilerinde bunlar sadece bir iz, bir patika ya da bir servis yolu olabilir, ancak daha fazla bağlam olmadan metin açıklamalarında pek faydalı değildir. Neyse ki daha iyisini yapabiliriz; uygulamanın yaptığı şey, adsız bir Yol'u anons etmek üzereyken, onun için biraz daha fazla bağlam bulup bulamayacağına bakmasıdır.

* **Bu bir kaldırım mı?**
OpenStreetMap'in birçok bölgesinde artık kaldırımlar yollardan ayrı olarak haritalanmıştır. Bunlar genellikle `sidewalk` olarak etiketlenir, ancak normalde hangi yolun kaldırımı olduklarını belirtmezler.

    Uygulama adsız bir kaldırımla karşılaştığında, yanında uzandığını düşündüğü bir yol arar ve kaldırımı adlandırmak için bunu kullanır. Bunun anonslarımız için çok önemli olduğu ortaya çıkıyor. Her kaldırım kavşağını anons etmek yerine, bir kaldırım boyunca ilerledikçe anonslar, ilişkili yol boyunca ilerliyormuşuz gibi yapılır. *"Patika boyunca batıya doğru ilerliyorsunuz"* yerine *"Moor Road boyunca batıya doğru ilerliyorsunuz"* deriz. Kullanıcı haritalanmış kaldırımdadır, ancak açıklama daha anlamlıdır.

* **Adlandırılmış bir Yol'da mı sona eriyor?**
Çoğu zaman iki yolu birbirine bağlayan yaya patikaları vardır. Patikanın her iki ucuna bakarak bu bağlamı kolayca ekleyebiliriz; böylece bir yönde *"Moor Road'a giden patika"* ve diğer uçtan yaklaşırken *"Roselea Drive'a giden patika"* olabilir. Bu yalnızca patika bölünmediğinde yapılır; iki adsız patikaya bölünürse bu bağlamı eklemeye çalışmayız.

* **Bir Kayıtlı Noktanın yakınında mı sona eriyor?**
Adsız bir Yol bir Kayıtlı Noktanın yakınında başlar veya biterse, onu tanımlamak için bu kullanılır; örneğin *"Büyük ağaç kavşağına giden patika"*. Kullanıcı istediği yere Kayıtlı Noktalar ekleyebilir ve patika ağları boyunca Kayıtlı Noktalar ekleyerek tüm bir rotaya bağlam ekleyebilir.

* **Bir POI'ye giriyor veya çıkıyor mu?**
Adsız bir Yol bir POI'nin dışında başlayıp içinde sona eriyorsa (veya tam tersi), bu bağlamı ekleyebiliriz; örneğin *"Lennox Park'a giden iz"*.

* **Bir Girişin yakınında mı sona eriyor?**
Adsız bir Yol bir Girişe daha yakın başlar veya biterse, bu bağlamı ekleyebiliriz; örneğin *"Best Buy'a giden servis yolu"*.

* **Bir Simge Yapı veya Yerin yakınında mı sona eriyor?**
Adsız bir Yol bir Simge Yapıya daha yakın başlar veya biterse, bu bağlamı da ekleyebiliriz; örneğin *"St. Giles Katedrali'ne giden servis yolu"*.

* **Çıkmaz yol mu?**
Uygulama, hiçbir yere ulaşmayan adsız Yolları çıkmaz yol olarak işaretler.

* **Herhangi bir merdivenden geçiyor mu?**
Adsız Yol bir köprüden, bir tünelden ya da merdivenlerden yukarı/aşağı geçiyorsa, bu not edilir ve bağlama eklenir. Bu, hedef etiketlemesinden ayrıdır; bu nedenle *"Lennox Park'a köprü üzerinden patika"* gibi bir bağlam mümkündür.

Bu bağlamlar sırayla eklenir; bu nedenle bir yönde *"Merdivenlerden Park Lane'e giden patika"* ve diğer yönde *"Merdivenlerden Lennox Park'a giden patika"* olması mümkündür. Adlandırılmış cadde, parktan ayrılırken önceliklidir, ancak parka girerken park kullanılır.

#### Gelecekteki bağlam
Gelecekte eklemeyi umduğumuz çeşitli ek bağlamlar vardır:
* Doğrusal su özelliklerini takip eden Yollar için bağlam, örneğin *"River Dee'nin yanındaki patika"*
* Su kütlelerinin kenarını takip eden Yollar için bağlam *"Milngavie barajının yanındaki patika"*
* Demiryollarını takip eden Yollar için bağlam, örneğin *"Demiryolunun yanındaki patika"*. Bu, demiryolu hattının adını bile içerebilir
* Köprülere ve tünellere ekstra içerik ekleme, neyin üzerinde veya altında olduklarını belirtme, örneğin *"Moor Road'a demiryolu üzerinden köprüyle giden patika"*

## Sesli anonslar
Artık harita verilerini kolayca kullanabileceğimiz bir biçime sahip olduğumuza göre, anonsları oluşturmak gerçekten oldukça basittir.

### Yürürken yapılan anonslar
Yürürken gerçekleşebilecek sesli anonslar şunlardır (öncelik sırasına göre):

1. Mevcut hedefin ne kadar uzakta olduğunu açıklama
1. Yaklaşan bir kavşağı açıklama
1. En yakın 5 ilgi noktasını açıklama

Tüm anonslar, çok sık tekrarlanmamaları için hız sınırlamasına tabidir. Kullanıcı hareket etmeyi bırakırsa anonslar duracaktır ve hareket halindeyken bile bir anons her yeni GPS konumunda tekrarlanmaz. iOS uygulamasındaki sıklık şöyledir:

* Mevcut hedef için her 60 saniyede bir
* Yaklaşan bir kavşak için her 30 saniyede bir
* Bir ilgi noktası için her 60 saniyede bir

Anonslar ayarlar menüsünden filtrelenebilir ve bu davranışı genişletmek için kesinlikle alan var.

### Daha hızlı seyahat ederken yapılan anonslar
Saniyede 5 metreden fazla hızla seyahat ederken, mevcut hedefe yönelik anons hâlâ gerçekleşir, ancak kavşak ve ilgi noktası anonsları, kullanıcının kabaca nerede olduğunu açıklayan bir anonsla değiştirilir. Bu, yakındaki bir toplu taşıma durağını, bizi içine alan bir ilgi noktasını (örneğin büyük bir parkın içinde) ya da yakındaki bir yolu ve yerleşimi verir. Bunlar daha önce açıklanan verileri kullanır ve gelecekte bunun özelleştirilmesine izin vermek için açık bir alan vardır.

## Kayıtlı Noktalar ve Rotalar
Çoğunlukla kayıtlı noktalar ve rotalar, ne GPS'e ne de gerçekten harita verilerine dayanan yalnızca bir kullanıcı arayüzü özelliğidir. Kayıtlı noktalar, kullanıcının saklamak istediği adlandırılmış konumlardır ve rotalar, bu kayıtlı noktaların sıralı bir listesidir. Her ikisini oluşturmaya yönelik kullanıcı arayüzü doğrudan iOS sürümünden alınmıştır.

### Rota oynatma
Rota oynatma, rotaların hayata geçtiği yerdir. Bir rota oynatıldığında, rotadaki ilk kayıtlı noktada bir sesli işaret oluşturulur. Kullanıcı o kayıtlı noktaya yaklaştığında, rota otomatik olarak sesli işareti rotadaki bir sonraki kayıtlı noktaya taşır. Başka kayıtlı nokta yoksa rota oynatma sona erer.

## Sonuç
Umarız bu, uygulamanın nasıl çalıştığına dair biraz fikir vermiştir. Uygulama, kullanıcılardan gelen geri bildirimlere göre sürekli olarak geliştirilmektedir; bu nedenle eklenebileceğini düşündüğünüz bir şey varsa lütfen bizimle iletişime geçin.
