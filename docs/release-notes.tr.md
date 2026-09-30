---
title: Sürüm notları
layout: page
nav_order: 5
has_toc: false
lang: tr
permalink: /release-notes.html
machine-translated: true
---

# Sürüm notları

Soundscape 2.0 büyük bir sürüm ve şu anda kapalı beta aşamasında. En önemli değişiklik, Soundscape'in
artık yalnızca yürürken değil, arabayla, otobüsle veya trenle yolculuk ederken de söyleyecek yararlı
bir şeyinin olması. Bunun yanında yerlerin nasıl tarif edildiğine dair pek çok küçük çalışma, yirmi
yeni dil ve uzun bir düzeltme listesi var.

Daha eski sürümlerin notları
[1.x sürüm notları]({{ "/v1.0-release-notes.html" | relative_url }}) sayfasında.

## 2.0'daki yenilikler

* **Arabayla, otobüsle veya trenle yolculuk sırasında bildirimler.** Soundscape hızla hareket
  ettiğinizi algılar ve yakın çevrenizi anlatmak yerine yolculuğunuzu tarif eder.
* **Su yollarını ve demiryollarını geçerken bildirim.** Nehirler, kanallar, körfezler ve demiryolu
  hatları, üzerlerinden geçerken duyurulur — hem yürürken hem yolculuk ederken.
* **Soundscape'in ne kadar konuşacağını seçin.** Yeni *Anons Ayrıntısı* ayarı kalabalık yerlerde
  Soundscape'i daha sessiz yapar, *Anons Edilecek Yerler* ise hangi tür yerleri duyacağınızı seçmenizi
  sağlar. Ayrıntı düzeyini yürürken kulaklığınızın düğmeleriyle değiştirebilirsiniz.
* **Bir sonraki kavşağın ne kadar uzakta olduğunu bilin.** Kavşaklar yaklaştıkça sabit bir mesafede
  anons edilir ve anons artık kaldırım kenarının ne kadar uzakta olduğunu söyler.
* **Bir yer türünü ya da koordinatları arayın.** Adları ne olursa olsun en yakınlarını bulmak için
  «eczane» ya da «otobüs durağı» diye arayın veya koordinat, harita bağlantısı ya da Plus Code
  yapıştırın.
* **Bir yeri başka bir harita uygulamasında açın**, örneğin Google Haritalar'da; Konum Ayrıntıları'ndan
  ya da listelerden.
* **Ana ekrandaki sesli işaretten daha fazlası.** Artık mesafeyi ve yönü gösteriyor; sesli işareti
  anons etmek, hakkında daha fazla bilgi almak ya da kayıtlı nokta olarak kaydetmek için ekran okuyucu
  eylemleri var.
* **Daha iyi adresler ve yer adları.** Kendi adresi olmayan yerler artık bulundukları sokağı ve
  bölgeyi alıyor, kapı numaraları sokağın doğru tarafıyla eşleştiriliyor ve Büyük Britanya'daki otobüs
  durakları resmi adlarını kullanıyor.
* **Yirmi yeni dil**, böylece toplam 46 oldu. Bu belge sitesi de çevrildi.
* **Ayrılınca uyanma.** Uyku modu artık, onu uyuttuğunuz yerden ayrıldığınızda Soundscape'i yeniden
  uyandırabiliyor.
* **Daha kısa, daha doğal mesafeler**, hızlı hareket ederken daha büyük birimlerle.
* **Daha hızlı çıkış.** *Soundscape'ten çık* artık ana menünün en üstünde.
* **Çevrimdışı harita iyileştirmeleri**, indirilmiş bir haritanın yerinde güncellenmesi ve bu sitede
  mevcut bölgelerin haritası dahil.
* **Çok sayıda erişilebilirlik çalışması** TalkBack ile, özellikle tanıtım ekranları çevresinde.
* **Pek çok çökme ve kararlılık düzeltmesi.**

2.0'da iki şey **kaldırıldı**: sesli denetim ve uygulama içindeki dil menüsü. Bunların yerine ne
yapabileceğiniz için aşağıdaki
[Kaldırılan özellikler](#things-that-have-been-removed) bölümüne bakın.

---

## Daha ayrıntılı olarak

### Arabayla, otobüsle veya trenle yolculuk

Bu, mevcut kullanıcılar için en büyük yenilik. Önceden Soundscape, bir araca bindiğiniz anda söyleyecek
çok az şeye sahipti: yakın çevrenizi anlatmayı sürdürüyordu; bu da hızda, çoktan geçtiğiniz şeylerin
akışı anlamına geliyordu.

Soundscape artık yürüme hızından daha hızlı gittiğinizi fark ediyor ve size söylediklerini değiştiriyor.
Açmanız gereken bir şey yok; yavaşladığınızda ya da inip yürümeye başladığınızda her şey kendiliğinden
normale dönüyor.

Yolculuk sırasında şunları duyacaksınız:

* **Nerede olduğunuzu**, zaman zaman — üzerinde gittiğiniz yolu ve yönünüzü, örneğin "M8 üzerinde
  kuzeye doğru". Numaralı yollar numaralarıyla duyurulur ve Soundscape, sokak adı her değiştiğinde aynı
  yolu yeniden duyurmaz.
* **Yöneldiğiniz kasaba ve köyleri**, mesafesiyle birlikte; ayrıca uzaklaştığınız ya da yalnızca
  yanından geçtiğiniz yerleri.
* **Otoyol kavşaklarını ve çıkışlarını**, onlara ulaştığınızda.
* **Büyük yer imlerini** yanlarından geçerken: parklar, hastaneler, stadyumlar ve alışveriş merkezleri
  gibi.
* **Otobüs, tramvay ve tren duraklarını** yanlarından geçerken. Soundscape yalnızca yolun sizin
  tarafınızdaki durakları söyler, çünkü karşı taraftakiler ters yönü hizmet eder.
* **Geçtiğiniz nehirleri, kanalları ve demiryollarını.**
* **Tünelleri**; bu esas olarak Soundscape'in neden birazdan susacağını açıklar — içeride GPS sinyali
  yoktur.

**Trende** Soundscape, yolda değil demiryolunda olduğunuzu anlar ve hangi yerleşimlerden geçtiğinizi ve
son istasyondan bu yana ne kadar yol aldığınızı söyler. Bunu anlamak kulağa geldiğinden daha zordur,
çünkü otoyollar ve demiryolu hatları çoğu zaman kilometrelerce yan yana inşa edilir; bu yüzden bu
sürümdeki çalışmanın önemli bir bölümü, birini diğeriyle karıştırmamaya ayrıldı.

Yürüyüşe ilişkin olağan bildirimler — yakındaki dükkânlar, yaya geçitleri vb. — yolculuk sırasında
bilinçli olarak tutulur ve şeylerin duyurulduğu mesafeler epeyce genişletilmiştir; böylece bir şeyi
geçmeden önce ondan haberdar olursunuz.

### Kavşaklar

Kavşak anonslarıyla ilgili en sık sorulan soru, kavşağın aslında ne kadar uzakta olduğuydu. Artık
Soundscape bunu söylüyor: «Kavşak 30 metre uzaklıkta». Mesafe, kavşağın ortasına değil, geçmek üzere
olduğunuz sokağın kaldırım kenarına kadar ölçülür, çünkü gerçekte durduğunuz yer orasıdır.

Anons ayrıca daha tutarlı bir noktada gelir. Önceden 45 metre ya da 10 metre önceden gelebiliyordu ve
ikisini ayırt etmenin bir yolu yoktu. Artık kavşak yaklaşık 30 metre uzakta olana kadar bekliyor;
böylece mesafe her seferinde aşağı yukarı aynı anlama geliyor.

### Su yollarını ve demiryollarını geçmek

Soundscape artık bir nehri, kanalı, körfezi, koyu veya demiryolu hattını geçtiğinizde size söyler. Bu
hem yürürken hem yolculuk ederken çalışır ve altından geçmeyi de üstünden geçmeyi de kapsar; böylece
hem bir yaya köprüsü hem de bir alt geçit tarif edilir.

### Soundscape'in ne kadar konuşacağını seçin

Soundscape hakkında en sık duyduğumuz şey, şehir merkezi gibi kalabalık yerlerde fazla konuşmasıdır.
*Ayarlar*'daki *Anonsları yönet* bölümünde artık eski anahtar listesi yerine üç ayar var:

* **Anons Ayrıntısı** Sessiz, Temel, Sadeleştirilmiş ya da Ayrıntılı olabilir. *Ayrıntılı*, Soundscape'in her
  zaman yaptığı şeydir ve başlangıç ayarıdır. *Sadeleştirilmiş* küçük patikaları ve servis yollarını atlar ve
  kendini daha seyrek tekrarlar. *Temel* yalnızca sokakları, kavşakları ve simge yapıları anons eder.
  *Sessiz* hiç otomatik anons yapmaz; sesli işaretler, rotalar ve ana ekran düğmeleri ise çalışmaya
  devam eder. Eski *Anonsları Etkinleştir* anahtarının yerini alır; o anahtar kapalıysa Anons Ayrıntısı
  artık Sessiz'e ayarlıdır.
* **Sokaklar ve Kavşaklar**, kavşaklarla ve üzerinde bulunduğunuz yolla ilgili anonsları açar ya da
  kapatır.
* **Anons Edilecek Yerler** işaretlenebilir bir listedir: Her Şey, Simge Yapılar, Toplu Taşıma, Yiyecek
  ve İçecek, Market ve Bakkallar, Bankalar ve ATM'ler ya da Hiçbir Yer. İstediğiniz kadarını işaretleyin;
  örneğin simge yapılar ve otobüs durakları. Kayıtlı noktalarınız her zaman anons edilir.

Uygun ayrıntı düzeyi yürüdükçe değişir; bu yüzden değiştirmek için Ayarlar'a girmeniz gerekmez.
Kulaklığınızdaki *Geri* düğmesine her basış Anons Ayrıntısı'nı bir düzey indirir: Ayrıntılı'dan
Sadeleştirilmiş ve Temel üzerinden Sessiz'e, sonra yeniden Ayrıntılı'ya. Yeni düzey her seferinde söylenir. Bu,
medya kontrollerinin iki modunda da çalışır ve bu yüzden kulaklık düğmeleri biraz değişti:

* *Orijinal Mod*'da, hiçbir rota oynatılmıyorken *İleri* artık *Etrafımda* anonsunu yapar ve *Konumum*
  artık düğmelerde değildir. Bir rota oynatılırken *İleri* ve *Geri* yine ara noktalar arasında
  geçiş yapar.
* *Sesli Menü* modunda *Geri* artık menüde geriye gitmez. *İleri* yine menüde ilerler ve
  *Oynat/Duraklat* yine seçer. Menüdeki kayıtlı noktalar ve rotalar artık ada göre sıralanır ve
  birini başlattığınızda menü, sizi listenin derinliklerinde bırakmak yerine başa döner.

### Arama

Arama çubuğu artık yer adlarından fazlasını anlıyor:

* **Yer türleri.** Kendi dilinizde «eczane», «tuvalet», «ATM» gibi aramalar yapın; Soundscape, adları
  ne olursa olsun o türdeki en yakın yerleri listeler. Çoğu tuvalet ve bank gibi adı olmayan yerler,
  ne olduklarıyla ve adresleriyle birlikte listelenir.
* **Koordinatlar, harita bağlantıları ve Plus Code'lar.** Bir sayı çifti, derece ve dakika, Google
  Haritalar, Apple Haritalar ya da OpenStreetMap bağlantısı veya bir Plus Code yapıştırın; Soundscape
  size tam o noktayı verir. Yalnız bir sayı çifti iki yönlü okunabilir; ikisi de anlamlıysa ikisi de
  size sunulur, en yakını önce.
* **Çevrim dışı arama.** Arama artık çevrim içinin yanı sıra her zaman indirdiğiniz haritalara da
  bakıyor ve böylece çok daha fazla adsız yer buluyor. İnternet bağlantısı olmadan arama yaptığınızda
  bulunduğunuz yerin çevrim dışı haritası yoksa Soundscape, hiçbir şey bulamamakla kalmak yerine bunu
  size söyler.

### Bir yeri başka bir uygulamada açmak

Konum Ayrıntıları'nda telefonunuzdaki harita ve navigasyon uygulamalarını listeleyen yeni bir
**Harita Uygulamasında Aç** düğmesi var. *Her zaman bu uygulamayı kullan* seçeneğini işaretlerseniz
düğme örneğin *Google Haritalar ile Aç* olur ve uygulamayı hemen açar; uzun basış listeyi geri getirir.
*Yakındaki Yerler* ve *Kayıtlı Noktalar* listelerinde de *Sesli İşareti Başlat*'ın yanında *… ile Aç*
ve *Paylaş* ekran okuyucu eylemleri var.

### Sesli işaret ve kayıtlı noktalar

* Ana ekrandaki sesli işaret artık **mesafesini ve yönünü** gösteriyor; bir ekran okuyucu onu örneğin
  «Milngavie Library'de Sesli İşaret, 390 metre, güneydoğu» diye okur. Rotalar da aynı şekilde geçerli
  ara noktaya olan mesafeyi gösterir.
* Sesli işaretin üç **ekran okuyucu eylemi** var: *İşareti Anons Et* nerede olduğunu söyler, *Daha
  Fazla Bilgi* adresi ekler ve *Kayıtlı Noktalara Ekle* onu kaydeder.
* *Kayıtlı Noktayı Düzenle* ekranında haritayı sürükleyerek bir **kayıtlı noktayı yeniden
  taşıyabilirsiniz**.

### Daha iyi adresler ve yer adları

Soundscape'in yerleri bir insanın tarif edeceği gibi tarif etmesi için çok çalışıldı:

* Kendi adresi olmayan yerler artık belirsiz kalmak yerine bulundukları sokak ve bölgeyle tarif edilir.
* Kapı numaraları sokağın doğru tarafıyla eşleştirilir. Önceden bir adres karşı kaldırımdan
  bildirilebiliyordu.
* Bir yerin adresi artık o yerin kendi adını tekrarlamıyor.
* Büyük Britanya'daki otobüs durakları resmi toplu taşıma adlarını kullanır; bunlar genellikle tarifede
  ve duraktaki tabelada yazan adlardır.
* Bir nehir ya da kanal boyunca uzanan adsız yaya yolları artık izledikleri suyun adıyla anılır.
* Adı olmayan patikalar ve yollar daha anlamlı biçimde tarif edilir ve onlar için kullanılan sözcükler
  İngilizce görünmek yerine düzgün çevrilmiştir.

### Diller

2.0'da yirmi yeni dil eklendi: Arapça, Bengalce, Bulgarca, Katalanca, Hırvatça, Çekçe, Hausa dili,
Macarca, Endonezce, Korece, Marathi, Sırpça, Slovakça, Slovence, Svahili, Tamilce, Telugu, Tayca, Urduca
ve Vietnamca. Bu dillerin tamamı alfa aşamasındadır ve doğrulukları hakkında geri bildirim almayı çok
isteriz. Toplamda Soundscape artık 46 dilde kullanılabiliyor ve bu belge sitesi de çevrildi.

Mısır Arapçası Arapçaya dahil edildi ve Luganda geri çekildi; ikisinde de yararlı olacak kadar çevrilmiş
metin yoktu.

Çeviriler topluluk çalışmasıdır; yardımınızı ya da bir şeyin kötü okunduğu yerlerde düzeltmelerinizi
memnuniyetle karşılarız. Her metin şu adreste iyileştirilebilir:
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Uyku modu

Uyku modu **ayrılınca uyanma** özelliğini kazandı. Soundscape'i uyuttuğunuzda, bölgeden ayrılır
ayrılmaz uyanmasını isteyebilirsiniz; bu, bir yere vardığınızda ve yeniden yola çıkana kadar sessizlik
istediğinizde işe yarar.

### Mesafeler ve konuşma

Seslendirilen mesafeler kısaltıldı ve daha doğal hale getirildi; Soundscape hızlı hareket ederken artık
daha büyük birimlere geçiyor — fit ya da metre cinsinden uzun bir sayım yerine mil veya kilometre. Her
dil, kesirli bir mesafenin nasıl söyleneceğine kendisi karar veriyor; bu daha önce İngilizce biçimli bir
kalıba sıkıştırılmıştı.

### Çevrimdışı haritalar

Çevrimdışı haritalar 1.0 ile geldi ve sürekli iyileştirildi:

* İndirilmiş bir harita, daha yeni bir sürüm mevcut olduğunda artık yerinde güncellenebiliyor; bu,
  bölüt ayrıntıları ekranından yapılır.
* Kullanılamayan haritalar — örneğin bozulmuş bir indirme — sessizce başarısız olmak yerine artık açıkça
  işaretleniyor.
* İndirmeler daha güvenilir ve ekran, mevcut haritaların listesi alınırken tam ekran bir yükleme
  göstergesi yerine ne olup bittiğini gösteriyor.
* Tamamlanmış bir indirme, ancak gerçekten kullanıma hazır olduğunda tamamlanmış olarak görünüyor.
* Bu sitede
  [mevcut bölgelerin haritası]({{ "/users/help-offline-map-extracts.html" | relative_url }}) var.

### Erişilebilirlik

Ekran okuyucu davranışı üzerinde çok büyük bir çalışma yapıldı; özellikle odağın önceden yanlış yere
atladığı tanıtım ekranlarında. Diğer iyileştirmeler arasında dosya boyutlarının ve ondalık sayıların
daha iyi okunması, fiili sona koyan dillerde doğru "etkinleştirmek için iki kez dokunun" ipuçları ve
hiç ayarlanmamış yerlerde anlamlı ipuçları yer alıyor.

### Menüler ve gezinme

* **Soundscape'ten çık** artık daha aşağıda değil, ana menünün ilk öğesi.
* Ana menü artık bir kenarda ekranın bir şeridini görünür bırakmıyor; bu, ekran okuyucu kullananlara
  kafa karıştırıcı fazladan bir dokunma alanı veriyordu.
* Sistemin geri hareketi, Yakındaki Yerler içinde kategorilere göz atarken artık bir düzeyi atlamıyor.
* *Sesli öğretici*, **rehberli öğretici** olarak yeniden adlandırıldı.
* Ayarlar düzenlendi ve *Varsayılanlara sıfırla* artık her şeyi doğru biçimde temizliyor.

### Kararlılık

2.0, düzeltilmiş çökme ve donmalardan oluşan uzun bir liste içeriyor: uygulamanın açılış ekranında
donması, ayarlar sıfırlanırken donmalar, bozuk indirilmiş harita nedeniyle çökmeler, ana ekrandan rota
ayrıntıları açılırken çökmeler, dil değiştirirken çökmeler ve Play Store üzerinden otomatik bildirilen
birkaç sorun. Pil ve başlangıç davranışı da arka plan uygulamalarını agresif biçimde kapatan telefonlarda
daha sağlam hale getirildi.

### Kaldırılan özellikler
{: #things-that-have-been-removed }

* **Sesli denetim** kaldırıldı. Tutulmaya değecek kadar güvenilir çalışmadı ve kulaklıklardaki medya
  düğmeleri büyük ölçüde aynı işi görüyor — bkz.
  [Medya denetimlerini kullanma yardımı]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape, Android 16 ve sonrasında Gemini üzerinden sesli komutlara da hazır; ancak bu komutlar,
  Google Gemini'de bunlar için desteği yayımlayana kadar çalışmayacak.
* **Uygulama içindeki dil menüsü** kaldırıldı. Soundscape artık telefonunuzda ayarlı dili izliyor; çoğu
  kişinin zaten beklediği buydu. Değiştirmek için telefonun dilini değiştirin ya da sunuyorsa telefon
  ayarlarından uygulama başına bir dil belirleyin.

## Sorunları bize nasıl bildirirsiniz

Bir şey yolunda değilse bunu duymak isteriz. Help Desk'e
<soundscapeAndroid@scottishtecharmy.support> adresinden yazın ya da STA üyesiyseniz Slack'te sorun.

Bir bildirim yanlışsa veya hiç gelmediyse, yolculuğunuzun bir kaydı bize çok yardımcı olur — kaydı
yeniden oynatıp Soundscape'in tam olarak neye dayandığını görebiliriz. Yönergeler şurada:
[Hata ayıklama için konum kaydı sağlama]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## iPhone hakkında bir not

Yukarıdakilerin tümü Android uygulamasıyla ilgili; ancak bu sürümdeki çalışmanın geri kalanının nereye
gittiğini bilmekte yarar var. Soundscape artık iPhone'da da çalışıyor ve her iki uygulama da aynı ortak
koddan derleniyor — aynı ekranlar, aynı ifadeler ve aynı bildirimler. Yukarıdaki yolculuk bildirimleri
gibi bir yenilik böylece iki kez yazılmak yerine ikisine birden geliyor. Bu ortak temel, 2.0'ın neden
bu kadar uzun sürdüğünü açıklıyor ve gelecekteki sürümlerin her iki platforma da daha hızlı ulaşmasını
sağlamalı. iPhone uygulaması şu anda davetle TestFlight üzerinden edinilebiliyor: STA üyesiyseniz
Slack'te sorun ya da Help Desk'e yazın.
