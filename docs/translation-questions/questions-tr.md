---
title: "Türkçe"
layout: default
nav_exclude: true
search_exclude: true
permalink: /translation-questions/questions-tr/
---

# Soundscape Türkçe çevirisi — birkaç soru

> Yanıtlarınızı **soundscapeAndroid@scottishtecharmy.support** adresine e-postayla gönderin ve konu satırında dili belirtin.


*Turkish translation — questions for native-speaker reviewers. English
glosses in italics are for the maintainer.*

Merhaba, göz atmayı kabul ettiğiniz için teşekkürler.

Türkçe çevirinin çoğu anadili Türkçe olan biri tarafından gözden geçirilmedi, bu
yüzden görüşünüz bizim için çok değerli. **Uygulamayı tanımanız ya da yüklemeniz
gerekmez:** her soruda metnin ne zaman duyulduğu, İngilizcesinin ne olduğu ve şu
an Türkçede nasıl duyulduğu yazıyor.

## Soundscape nedir?

Soundscape, görme engelli ve az gören kişiler için ücretsiz bir telefon
uygulamasıdır. Yürürken, kulaklıkla kullanılır; telefon çoğu zaman cepte durur.
Navigasyon gibi adım adım yol tarifi vermez ("sola dönün"); yakında ne olduğunu
sesli olarak söyler, böylece kişi yönünü kendisi bulur.

Sorularda geçen birkaç kavram:

- **Anons** *(callout)*: yanından geçtiğiniz bir şey hakkında kısa bir sesli mesaj;
  örneğin "Kafe", "Bağdat Caddesi'nin yanındaki kaldırım" ya da "Bağdat Caddesi
  boyunca kuzeye yöneliniyor". O şeyin bulunduğu yönden, üç boyutlu sesle duyulur.
- **Sesli İşaret** *(audio beacon)*: bir varış yeri seçtiğinizde, kulaklıkta o
  yönden gelen düzenli, tekrarlanan bir ses çalar. Döndüğünüzde ses de "kayar";
  böylece varış yerine kulağınızla gidebilirsiniz.
- **Kayıtlı Nokta** ve **rota** *(marker, route)*: kaydedilmiş yerler ve bunların bir
  sırası; Sesli İşaret sizi bunlardan tek tek geçirir.

Görme engelliler telefonu **ekran okuyucu** ile kullanır (Android'de TalkBack,
iPhone'da VoiceOver): her düğmeyi ve her metni sentetik bir ses okur. Bu yüzden
uygulamanın metinleri neredeyse her zaman *dinlenir*, okunmaz; çoğu zaman da sokak
gürültüsünün içinde. En önemlisi kısa, açık ve dinlerken doğal olmalarıdır.

Kavramlar (İngilizce olarak)
[bu sayfada]({{ "/developers/translation-terminology.html" | relative_url }}) daha
ayrıntılı anlatılıyor.

## Nasıl yanıt verilir

Soru numarasını belirterek e-postayla yanıt verin ("Q1: bence…"). Hepsini
yanıtlamanız gerekmez. Bir şey zaten iyiyse kısa bir "Tamam" da işe yarar.

---

### Q1 — Yer adlarına eklenen ekler *(Automatic suffixes on names)*

**Ne zaman duyulur:** sokak ya da yer adı geçen hemen her anonsta, yürürken.

**İngilizcesi:** "At Main Street", "Sidewalk next to Main Street" gibi.

**Şu an nasıl:** uygulama artık eki adın kendisine göre seçiyor: "İstanbul'da",
"Kadıköy'de", "Park'ta", "Ankara'ya". "Caddesi", "Parkı", "Mahallesi" gibi
adlarda "n" ekleniyor ("Atatürk Caddesi'nde", "Moda Parkı'ndan"). Sayılarda
okunuşa göre ("saat 3'te", "40'ta", "5'in 2. ara noktası"), kısaltmalarda harf
adına göre ("TRT'ye", "ABD'de") seçiyor.

**Bizi düşündüren:** kurallar ders kitabındaki gibi, ama istisnaları (örneğin
"Kemal'e" gibi kalın ünlüye rağmen ince ek alan adlar) bilemiyor.

**Soru:** bunlar doğru mu? Yanlış ek aldığını duyduğunuz bir ad olursa bize yazın.

### Q2 — "Patika'dan Moor Road'a" *(Path to X)*

**Ne zaman duyulur:** yürürken bir yan yolun yanından geçtiğinizde; uygulama o
yolun nereye gittiğini söyler.

**İngilizcesi:** "Path to Moor Road".

**Şu an nasıl:** "Patika'dan Moor Road'a" (yani "patikadan Moor Road'a").

**Bizi düşündüren:** bu, patikanın *başlangıç* noktası olduğunu ima ediyor.

**Soru:** "Moor Road'a giden patika" daha doğru mu?

### Q3 — VoiceOver ipuçları *(VoiceOver hint form)*

**Ne zaman duyulur:** VoiceOver her düğmenin adından sonra kısa bir kullanım ipucu
okur.

**İngilizcesi:** "Double tap to mute the audio beacon". Cümle iki parçadan oluşur:
"Double tap to …" ve onlarca ipucundan biri.

**Şu an nasıl:** "Sesli işareti sessize al için çift dokunun".

**Bizi düşündüren:** dilbilgisi açısından doğru değil.

**Soru:** "Sesli işareti sessize almak için çift dokunun" doğru mu?

### Q4 — "Sesli İşaret" *(Beacon)*

**Ne zaman duyulur:** düğmelerde, ayarlarda ve tanıtım turunda; örneğin "Sesli
işareti artık duyabilirsiniz. Ses, hedefinizin bulunduğu yönden gelir."

**İngilizcesi:** "Audio Beacon".

**Şu an nasıl:** "Sesli İşaret".

**Bizi düşündüren:** terim makine çevirisiyle seçildi; "işaret" yön gösteren bir
sesi doğal olarak çağrıştırıyor mu bilmiyoruz.

**Soru:** doğal mı? Değilse ne derdiniz?

### Q5 — Siri komutları *(Siri phrases)*

**Ne zaman duyulur:** duyulmaz; bu komutları siz *söylersiniz*. iPhone'da
Soundscape'i telefona dokunmadan Siri ile yönetebilirsiniz.

**Şu an nasıl:** "Soundscape çevre", "Soundscape rota", "Soundscape rota başlat",
"Soundscape işaret", "Soundscape işareti durdur", "Soundscape liste",
"Soundscape ayrıntı".

**Bizi düşündüren:** komutlar doğal söylenmezse akılda kalmaz.

**Soru:** kulağa doğal geliyor mu? Başka türlü söyler miydiniz?

### Q6 — "Anons" *(Callout)*

**Ne zaman duyulur:** yukarıda anlatılan kısa sesli mesajların adıdır. En çok
ayarlarda geçer, örneğin "Otomatik Anonslar".

**İngilizcesi:** "Callout", "Automatic Callouts".

**Şu an nasıl:** "Anons", "Otomatik Anonslar".

**Bizi düşündüren:** terim makine çevirisiyle seçildi; "anons" istasyon ya da
havaalanı anonslarını çağrıştırabilir.

**Soru:** doğal mı? Değilse ne derdiniz?

### Q7 — Başka bir şey? *(Anything else)*

İngilizceden çevrilmiş gibi duran, çok uzun ya da anlaşılmayan bir cümle varsa
bize bildirin.

Çok teşekkürler!
