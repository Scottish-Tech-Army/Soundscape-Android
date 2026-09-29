---
title: Catatan rilis
layout: page
nav_order: 5
has_toc: false
lang: id
permalink: /release-notes.html
machine-translated: true
---

# Catatan rilis

Soundscape 2.0 adalah rilis besar dan saat ini berada dalam tahap beta tertutup. Perubahan paling
menonjol adalah kini Soundscape punya sesuatu yang berguna untuk dikatakan ketika Anda bepergian
dengan mobil, bus, atau kereta, bukan hanya ketika berjalan kaki. Selain itu ada banyak pekerjaan
yang lebih kecil pada cara tempat dideskripsikan, dua puluh bahasa baru, dan daftar perbaikan yang
panjang.

Catatan untuk versi lama ada di halaman
[Catatan rilis untuk 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Yang baru di 2.0

* **Pengumuman saat bepergian dengan mobil, bus, atau kereta.** Soundscape mengenali bahwa Anda
  bergerak dengan kecepatan tinggi dan mendeskripsikan perjalanan Anda alih-alih lingkungan terdekat.
* **Pemberitahuan saat Anda melintasi perairan dan rel kereta.** Sungai, kanal, teluk, dan jalur rel
  diumumkan saat Anda melintasinya, baik ketika berjalan maupun saat bepergian.
* **Pilih seberapa banyak yang dikatakan Soundscape.** Pengaturan baru *Tingkat Detail Pemberitahuan*
  membuat Soundscape lebih tenang di tempat ramai, dan *Tempat yang Diberitahukan* memungkinkan Anda
  memilih jenis tempat yang ingin Anda dengar. Anda dapat mengubah tingkat detail dengan tombol
  headphone sambil berjalan.
* **Ketahui seberapa jauh persimpangan berikutnya.** Persimpangan diumumkan pada jarak yang tetap saat
  Anda mendekat, dan pemberitahuannya kini menyebutkan jarak ke tepi trotoar.
* **Cari jenis tempat, atau koordinat.** Cari "apotek" atau "halte bus" untuk menemukan yang terdekat,
  apa pun namanya, atau tempelkan koordinat, tautan peta, atau Plus Code.
* **Buka tempat di aplikasi peta lain**, seperti Google Maps, dari Detail Lokasi atau dari daftar.
* **Lebih banyak dari suar di layar utama.** Kini suar menampilkan jarak dan arah, dan punya aksi
  pembaca layar untuk memberitahukan suar, mendengar lebih banyak tentangnya, atau menyimpannya sebagai
  penanda.
* **Alamat dan nama tempat yang lebih baik.** Tempat yang tidak punya alamat sendiri kini mendapat
  nama jalan dan kawasan tempatnya berada, nomor rumah dicocokkan dengan sisi jalan yang benar, dan
  halte bus di Britania Raya memakai nama resminya.
* **Dua puluh bahasa baru**, sehingga totalnya menjadi 46. Situs dokumentasi ini juga diterjemahkan.
* **Bangun saat pergi.** Mode tidur kini dapat membangunkan Soundscape lagi ketika Anda meninggalkan
  tempat Anda menidurkannya.
* **Jarak yang lebih ringkas dan wajar**, dengan satuan lebih besar saat Anda bergerak cepat.
* **Jalan keluar yang lebih cepat.** *Keluar dari Soundscape* kini berada di bagian atas menu utama.
* **Peningkatan peta luring**, termasuk memperbarui peta yang sudah diunduh di tempatnya dan peta
  wilayah yang tersedia di situs ini.
* **Banyak pekerjaan aksesibilitas** dengan TalkBack, terutama di sekitar layar awal.
* **Sangat banyak perbaikan mogok dan kestabilan.**

Dua hal **dihapus** di 2.0: kendali suara dan menu bahasa di dalam aplikasi. Lihat
[Fitur yang dihapus](#things-that-have-been-removed) di bawah untuk mengetahui apa yang bisa Anda
lakukan sebagai gantinya.

---

## Lebih rinci

### Bepergian dengan mobil, bus, atau kereta

Ini adalah fitur baru terbesar bagi pengguna yang sudah ada. Sebelumnya Soundscape hampir tidak punya
apa pun untuk dikatakan begitu Anda masuk ke kendaraan: ia terus mendeskripsikan lingkungan terdekat,
yang pada kecepatan tinggi berarti aliran hal-hal yang sudah lama Anda lewati.

Soundscape kini menyadari bahwa Anda bergerak lebih cepat daripada kecepatan berjalan dan mengubah apa
yang disampaikannya. Tidak ada yang perlu dinyalakan, dan semuanya kembali normal dengan sendirinya
begitu Anda melambat atau turun dan berjalan.

Selama perjalanan Anda akan mendengar:

* **Di mana Anda berada**, sesekali — jalan yang Anda lalui dan arah Anda, misalnya «Melaju ke utara
  di M8». Jalan bernomor diumumkan dengan nomornya, dan Soundscape tidak mengumumkan ulang jalan yang
  sama setiap kali nama jalannya berubah.
* **Kota dan desa** yang Anda tuju, lengkap dengan jaraknya, serta yang Anda tinggalkan atau sekadar
  Anda lewati.
* **Simpang susun dan pintu keluar jalan tol** saat Anda mencapainya.
* **Penanda besar** yang Anda lewati, seperti taman, rumah sakit, stadion, dan pusat perbelanjaan.
* **Halte bus, trem, dan stasiun kereta** yang Anda lewati. Soundscape hanya menyebut halte di sisi
  jalan Anda, karena yang di seberang melayani arah sebaliknya.
* **Sungai, kanal, dan rel kereta yang Anda lintasi.**
* **Terowongan**, yang terutama menjelaskan mengapa Soundscape sebentar lagi akan senyap — di dalamnya
  tidak ada sinyal GPS.

Di **kereta**, Soundscape mengetahui bahwa Anda berada di jalur rel, bukan di jalan raya, dan
memberitahu kota-kota yang Anda lewati serta seberapa jauh Anda telah melaju sejak stasiun terakhir.
Menentukan hal ini lebih sulit daripada kedengarannya, karena jalan tol dan jalur rel sering dibangun
berdampingan sepanjang berkilo-kilometer, sehingga sebagian besar pekerjaan pada rilis ini dicurahkan
untuk tidak salah mengira yang satu sebagai yang lain.

Pengumuman biasa untuk pejalan kaki — toko di dekat sini, penyeberangan jalan, dan sebagainya —
sengaja ditahan selama perjalanan, dan jarak saat sesuatu diumumkan diperlebar cukup jauh agar Anda
mengetahuinya sebelum terlewat.

### Persimpangan

Pertanyaan yang paling sering tentang pemberitahuan persimpangan adalah seberapa jauh sebenarnya
persimpangan itu. Kini Soundscape memberitahukannya: "Persimpangan berjarak 30 meter". Jarak diukur ke
tepi trotoar jalan yang akan Anda seberangi, bukan ke tengah persimpangan, karena di situlah Anda
benar-benar berhenti.

Pemberitahuan juga datang pada titik yang lebih tetap. Sebelumnya pemberitahuan bisa datang 45 meter
atau 10 meter sebelumnya, tanpa ada yang membedakan keduanya. Kini pemberitahuan menunggu sampai
persimpangan berjarak sekitar 30 meter, sehingga jaraknya selalu berarti kurang lebih sama.

### Melintasi perairan dan rel kereta

Soundscape kini memberitahu Anda saat melintasi sungai, kanal, teluk, atau jalur rel. Ini berlaku baik
saat berjalan maupun saat bepergian, dan mencakup lewat di bawah sebagaimana lewat di atas, sehingga
jembatan penyeberangan maupun terowongan pejalan kaki sama-sama dideskripsikan.

### Memilih seberapa banyak yang dikatakan Soundscape

Hal yang paling sering kami dengar tentang Soundscape adalah terlalu banyak bicara di tempat ramai
seperti pusat kota. Bagian *Kelola Pemberitahuan* di *Pengaturan* kini punya tiga pengaturan sebagai
pengganti daftar sakelar yang lama:

* **Tingkat Detail Pemberitahuan** berupa Senyap, Ringkas, Seimbang, atau Rinci. *Rinci* adalah yang
  selama ini dilakukan Soundscape, dan menjadi pengaturan awal. *Seimbang* melewatkan jalan setapak
  kecil dan jalan servis serta lebih jarang mengulang. *Ringkas* hanya menyebutkan jalan, persimpangan,
  dan landmark. *Senyap* tidak memberikan pemberitahuan otomatis sama sekali, sementara suar, rute,
  dan tombol di layar utama tetap berfungsi. Pengaturan ini menggantikan sakelar lama *Izinkan
  Pemberitahuan*; jika sakelar itu mati, Tingkat Detail Pemberitahuan kini diatur ke Senyap.
* **Jalan dan Persimpangan** menyalakan atau mematikan pemberitahuan tentang persimpangan dan jalan
  yang sedang Anda lalui.
* **Tempat yang Diberitahukan** adalah daftar yang bisa dicentang: Semua, Landmark, Transportasi Umum,
  Makanan dan Minuman, Toko Kelontong dan Swalayan, Bank dan ATM, atau Tanpa Tempat. Centang sebanyak
  yang Anda mau, misalnya landmark dan halte bus. Penanda Anda selalu diberitahukan.

Tingkat detail yang pas berubah saat Anda berjalan, jadi Anda tidak perlu masuk ke Pengaturan untuk
mengubahnya. Menekan *Sebelumnya* di headphone menurunkan Tingkat Detail Pemberitahuan satu tingkat
setiap kali, dari Rinci ke Seimbang dan Ringkas lalu Senyap, kemudian kembali ke Rinci. Tingkat yang
baru diucapkan setiap kali. Ini berfungsi di kedua mode kontrol media, sehingga tombol headphone sedikit
berubah:

* Di *Mode asli*, *Berikutnya* kini menyebutkan *Di Sekitar Saya* saat tidak ada rute yang diputar,
  dan *Lokasi Saya* tidak lagi ada di tombol. Saat rute diputar, *Berikutnya* dan *Sebelumnya* tetap
  berpindah antar titik rute.
* Di mode *Menu audio*, *Sebelumnya* tidak lagi mundur di menu. *Berikutnya* tetap menelusurinya dan
  *Putar/Jeda* tetap memilih. Penanda dan rute di menu kini diurutkan berdasarkan nama, dan setelah
  Anda memulai salah satunya, menu kembali ke awal alih-alih meninggalkan Anda di dalam daftar.

### Pencarian

Bilah pencarian kini memahami lebih dari sekadar nama tempat:

* **Jenis tempat.** Cari "apotek", "toilet", "ATM", dan seterusnya dalam bahasa Anda sendiri, dan
  Soundscape menampilkan tempat terdekat dari jenis itu, apa pun namanya. Tempat tanpa nama, seperti
  kebanyakan toilet dan bangku, ditampilkan menurut jenisnya, beserta alamatnya.
* **Koordinat, tautan peta, dan Plus Code.** Tempelkan sepasang angka, derajat dan menit, tautan dari
  Google Maps, Apple Maps, atau OpenStreetMap, atau Plus Code, dan Soundscape memberi Anda titik yang
  tepat itu. Sepasang angka saja bisa dibaca dua arah, jadi jika keduanya masuk akal, Anda ditawari
  keduanya, yang terdekat lebih dulu.
* **Pencarian offline.** Pencarian kini selalu juga melihat peta yang telah Anda unduh, selain online,
  sehingga menemukan jauh lebih banyak tempat tanpa nama. Jika Anda mencari tanpa koneksi internet dan
  tidak punya peta offline untuk tempat Anda berada, Soundscape memberitahukannya alih-alih tidak
  menemukan apa pun begitu saja.

### Membuka tempat di aplikasi lain

Detail Lokasi punya tombol baru, **Buka di Aplikasi Peta**, yang menampilkan aplikasi peta dan navigasi
di ponsel Anda. Centang *Selalu gunakan aplikasi ini* dan tombol berubah menjadi, misalnya, *Buka di
Google Maps*, yang langsung membukanya; tekan lama untuk memunculkan daftar lagi. Daftar *Tempat di
Sekitar* dan *Penanda* juga punya aksi pembaca layar *Buka di…* dan *Bagikan*, di samping *Mulai Suar
Audio*.

### Suar dan penanda

* Suar di layar utama kini menampilkan **jarak dan arahnya**, dan pembaca layar membacanya, misalnya,
  sebagai "Suar di Milngavie Library, 390 meter, tenggara". Rute menampilkan jarak ke titik rute saat
  ini dengan cara yang sama.
* Suar punya tiga **aksi pembaca layar**: *Beritahukan Suar* menyebutkan lokasinya, *Info
  Selengkapnya* menambahkan alamat, dan *Tambahkan ke Penanda* menyimpannya.
* Anda bisa lagi **memindahkan penanda** dengan menyeret peta dari layar *Edit Penanda*.

### Alamat dan nama tempat yang lebih baik

Banyak upaya dicurahkan agar Soundscape mendeskripsikan tempat sebagaimana manusia melakukannya:

* Tempat yang tidak punya alamat sendiri kini dideskripsikan dengan jalan dan kawasan tempatnya
  berada, alih-alih dibiarkan samar.
* Nomor rumah dicocokkan dengan sisi jalan yang benar. Sebelumnya sebuah alamat bisa dilaporkan dari
  trotoar seberang.
* Alamat sebuah tempat tidak lagi mengulang nama tempat itu sendiri.
* Halte bus di Britania Raya memakai nama resmi angkutan umum, biasanya yang tercantum di jadwal dan
  di papan halte.
* Jalan setapak tanpa nama yang menyusuri sungai atau kanal kini dinamai sesuai perairan yang
  diikutinya.
* Jalan setapak dan jalan tanpa nama dideskripsikan dengan lebih masuk akal, dan kata-kata yang
  dipakai untuknya diterjemahkan dengan benar alih-alih muncul dalam bahasa Inggris.

### Bahasa

Dua puluh bahasa baru ditambahkan di 2.0: Arab, Bengali, Bulgaria, Katalan, Kroasia, Ceko, Hausa,
Hungaria, Indonesia, Korea, Marathi, Serbia, Slovakia, Slovenia, Swahili, Tamil, Telugu, Thai, Urdu,
dan Vietnam. Semua bahasa ini masih dalam tahap alfa, dan kami sangat ingin mendapat masukan tentang
ketepatannya. Secara keseluruhan Soundscape kini tersedia dalam 46 bahasa, dan situs dokumentasi ini
juga telah diterjemahkan.

Bahasa Arab Mesir digabungkan ke dalam bahasa Arab, dan bahasa Luganda ditarik, karena keduanya tidak
punya cukup teks terjemahan untuk bisa berguna.

Terjemahan adalah kerja komunitas dan kami menyambut bantuan Anda, atau koreksi di tempat yang terbaca
janggal. Setiap teks dapat diperbaiki di
<https://hosted.weblate.org/projects/soundscape-android/androidkmp/>.

### Mode tidur

Mode tidur kini punya **bangun saat pergi**. Ketika Anda menidurkan Soundscape, Anda dapat memintanya
bangun begitu Anda meninggalkan kawasan tersebut. Ini berguna ketika Anda tiba di suatu tempat dan
ingin tenang sampai berangkat lagi.

### Jarak dan ucapan

Jarak yang diucapkan dibuat lebih ringkas dan wajar, dan Soundscape kini beralih ke satuan yang lebih
besar saat Anda bergerak cepat — mil atau kilometer alih-alih hitungan panjang dalam kaki atau meter.
Setiap bahasa menentukan sendiri cara menyebut jarak pecahan, yang sebelumnya dipaksakan ke pola
berbentuk Inggris.

### Peta luring

Peta luring hadir di 1.0 dan terus disempurnakan:

* Peta yang sudah diunduh kini dapat diperbarui di tempatnya ketika tersedia versi lebih baru, dari
  layar detail petikan peta.
* Peta yang tidak dapat dipakai — misalnya unduhan yang rusak — kini ditandai dengan jelas alih-alih
  gagal diam-diam.
* Unduhan lebih andal, dan layar menunjukkan apa yang sedang terjadi saat daftar peta yang tersedia
  diambil, alih-alih pemutar muat satu layar penuh.
* Unduhan yang selesai baru tampil sebagai selesai ketika benar-benar siap dipakai.
* Di situs ini tersedia
  [peta wilayah yang tersedia]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Aksesibilitas

Banyak sekali pekerjaan dicurahkan pada perilaku pembaca layar, terutama di layar awal tempat fokus
dulu melompat ke tempat yang keliru. Peningkatan lain mencakup pembacaan ukuran berkas dan angka
desimal yang lebih baik, petunjuk «ketuk dua kali untuk...» yang benar pada bahasa yang meletakkan
kata kerja di akhir, serta petunjuk yang masuk akal di tempat yang sebelumnya tidak diatur sama sekali.

### Menu dan navigasi

* **Keluar dari Soundscape** kini menjadi butir pertama di menu utama, bukan berada lebih ke bawah.
* Menu utama tidak lagi menyisakan pita layar di satu sisi, yang dulu memberi pengguna pembaca layar
  area sentuh tambahan yang membingungkan.
* Gestur kembali sistem tidak lagi melewati satu tingkat saat Anda menelusuri kategori di «Tempat
  Terdekat».
* *Tutorial audio* berganti nama menjadi **tutorial terpandu**.
* Pengaturan telah dirapikan, dan *Setel ulang ke bawaan* kini benar-benar membersihkan semuanya.

### Kestabilan

2.0 memuat daftar panjang perbaikan mogok dan macet, di antaranya aplikasi macet di layar pembuka,
macet saat menyetel ulang pengaturan, mogok karena peta unduhan yang rusak, mogok saat membuka detail
rute dari layar utama, mogok saat mengganti bahasa, serta beberapa masalah yang dilaporkan otomatis
melalui Play Store. Perilaku terkait baterai dan pengaktifan juga dibuat lebih tangguh pada ponsel
yang menutup aplikasi latar secara agresif.

### Fitur yang dihapus
{: #things-that-have-been-removed }

* **Kendali suara** telah dihapus. Fitur ini tidak pernah bekerja cukup andal untuk dipertahankan, dan
  tombol media pada headphone mencakup sebagian besar kebutuhan yang sama — lihat
  [Bantuan penggunaan tombol media]({{ "/users/help-using-media-controls.html" | relative_url }}).
  Soundscape juga sudah siap untuk perintah suara melalui Gemini di Android 16 atau lebih baru, tetapi
  perintah itu baru akan berfungsi setelah Google merilis dukungannya di Gemini.
* **Menu bahasa di dalam aplikasi** telah hilang. Soundscape kini mengikuti bahasa yang Anda atur di
  ponsel, yang memang diharapkan kebanyakan orang. Untuk mengubahnya, ganti bahasa ponsel, atau atur
  bahasa per aplikasi di pengaturan ponsel jika tersedia.

## Cara memberi tahu kami tentang masalah

Jika ada yang tidak beres, kami ingin mendengarnya. Kirim surel ke Help Desk di
<soundscapeAndroid@scottishtecharmy.support>, atau bertanyalah di Slack jika Anda anggota STA.

Jika sebuah pengumuman keliru atau tidak muncul sama sekali, rekaman perjalanan Anda sangat membantu
kami — kami dapat memutarnya ulang dan melihat persis data apa yang dipakai Soundscape. Petunjuknya
ada di
[Menyediakan rekaman lokasi untuk penelusuran galat]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Catatan tentang iPhone

Semua hal di atas adalah tentang aplikasi Android, tetapi ada baiknya mengetahui ke mana sisa pekerjaan
rilis ini pergi. Soundscape kini juga berjalan di iPhone, dan kedua aplikasi dibangun dari kode bersama
yang sama — layar yang sama, kata-kata yang sama, dan pengumuman yang sama. Fitur baru seperti
pengumuman perjalanan di atas dengan demikian tiba di keduanya sekaligus alih-alih ditulis dua kali.
Fondasi bersama itulah yang menjelaskan mengapa 2.0 memakan waktu selama ini, dan itu pula yang
seharusnya membuat rilis mendatang tiba lebih cepat di kedua platform. Aplikasi iPhone saat ini
tersedia melalui TestFlight dengan undangan: bertanyalah di Slack jika Anda anggota STA, atau kirim
surel ke Help Desk.
