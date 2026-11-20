# Block Rumput di Map Editor

Map Editor hanya tersedia di preview. World baru memakai salinan terrain Base World 50×50: empat block tanah dan satu block rumput per tile, yang dapat diedit secara independen. World lama tidak dimigrasi atau diubah ukurannya. Base World terkunci. Pilih world buatan, buka Map Editor, lalu pilih **Block → Rumput**. Pemilih block menampilkan nama lengkap dan thumbnail; pada layar kecil panel alat dapat digulir.

- **Tambah block** menempatkan satu block di atas block paling atas, atau pada sel kosong di dalam grid. Tidak memperluas batas grid; tinggi maksimum 32 block per sel. Tinggi satu block sama dengan tinggi pita rumput di sisi pulau.
- **Ganti block** (pensil) mengganti block paling atas, bukan menambah tingkat. Pada sel kosong gunakan Tambah.
- **Hapus block** menghapus block paling atas; sel menjadi kosong jika block terakhir dihapus. **Ambil jenis block** memilih jenis paling atas. **Isi area block** mengganti permukaan yang terhubung dengan jenis dan ketinggian yang sama, atau mengisi area kosong yang terhubung.
- Ketuk permukaan atau sisi block yang terlihat untuk memilih kolomnya. Gerakan seret yang melewati sel yang sama beberapa kali mengubahnya sekali; satu seret merupakan satu langkah Undo/Redo.
- Ketinggian di bawah karakter atau rumah dilindungi, dan rumah hanya dapat dipindahkan ke tapak yang rata. Jika penyimpanan gagal atau data berubah di tab lain, perubahan ditolak.

Gameplay preview menggunakan permukaan teratas: karakter dapat naik atau turun satu tingkat per langkah; sel kosong dan perbedaan dua tingkat atau lebih menghalangi rute. Jalur dapat memutar melalui permukaan bertangga. World datar lama tidak perlu migrasi. Riwayat Undo/Redo berlaku selama sesi tab.

World bertingkat memakai format JSON preview versi 4 saat diekspor; versi Android saat ini dapat mencoba mengimpor format ini; dukungan v21 tersedia tetapi belum diuji pada perangkat. Data browser dan Android terpisah dan tidak otomatis tersinkron. Efek pencahayaan, bayangan serta urutan gambar objek pada tebing tinggi masih menggunakan renderer pulau 2D yang ada, bukan mesin voxel 3D. Thumbnail dan rumpun rumput menggunakan artwork konsep pilihan.

### Tampilan tanah dasar

Rumput permukaan kembali menggunakan warna berselang-seling dengan batas tipis
antar-tile. Sisi tanah bawah memakai satu warna yang sama di kiri dan kanan;
garis hijau dari sisi rumput dasar yang tertutup block lain diganti warna tanah.
Block tambahan tetap memiliki variasi warna dan outline sisi. Aturan rendering
berlaku pada Base World dan world lama maupun baru, tanpa mengubah data world.

Format lama belum mencatat asal-usul lapisan. Tingkat nol dianggap dasar; empat
lapisan awal `[2,2,2,2]` serta rumput di atasnya dikenali sebagai template dasar.
Karena ini berdasarkan isi stack, lapisan hasil edit yang menyerupai template
juga mengikuti tampilan dasar. Perubahan belum diuji di perangkat Android.

Pada world lama dengan rumput dasar yang sudah tertutup block, renderer tidak
lagi menggambar tutup, garis, atau detail rumput pada lapisan tertutup itu;
permukaannya mengikuti warna tanah agar tidak muncul garis hijau di tengah sisi
pulau. Tanah dasar digelapkan, sementara rumput teratas tetap berpola. Tes
`tools/test_soil_green_seam_browser.js` memeriksa stack lama `[1,2,2,2,1]`,
garis pada rumput yang terlihat, serta memastikan data world tidak berubah.

Garis hijau yang masih terlihat pada Home ternyata berasal dari garis tepi pulau
terpisah yang digambar pada tinggi lama, bukan hanya dari rumput tertutup. Garis
tersebut dihapus tanpa menghapus batas tile rumput. Tes
`tools/test_soil_rim_pixels_browser.js` membuat Home baru dan memeriksa piksel
pada posisi garis itu; pengujian tidak mengubah data world.

Jembatan kayu dan batu menggunakan sprite baru yang terinspirasi referensi:
kayu berdek lurus dengan balok rendah, batu melengkung dengan pembatas rendah.
Sprite lama tetap disimpan; panjang 3, 4, dan 5 tile serta aturan penempatan,
orientasi, dan data world tidak berubah. Pipit terbang lebih tinggi dan digambar
di atas objek foreground agar membantu menandai karakter yang tertutup; saat
duduk pipit tetap bertengger pada karakter. Perilaku Android belum diuji di perangkat.

Dek jembatan dikalibrasi dari titik sudut masing-masing sprite ke proyeksi
tepat panjang 3/4/5 dan lebar 1 tile; orientasi E dicerminkan dan proyeksi
mengikuti tinggi tile. Tiang dan lengkungan tetap merupakan bagian sprite,
sehingga bisa terlihat di atas/bawah dek; tapak dan data tersimpan tidak diubah.
Pipit juga digambar di editor preview, selain gameplay; gambar yang terlambat
selesai dimuat meminta render ulang ketika animasi dikurangi.

Di editor preview, hit jembatan mengikuti bentuk dek, tiang dan pegangan,
bukan seluruh tile tapaknya. Pemilihan block menolak titik di luar permukaan
atau sisi stack yang terlihat. Rumah, karakter, tanaman dan pagar memakai
mask sprite atau geometri masing-masing.

Rencana desain jembatan lebar: 3×2, 4×2 dan 5×3; kayu mengikuti
`artwork/bridge-wood-3x2-choice.png`, batu mengikuti bentuk kayu. Penyangga
vertikal hanya di kedua ujung, tanpa tiang tengah. Studi visual disimpan di
`artwork/bridge-wood-wide-sprite-study.png` dan
`artwork/bridge-stone-wide-sprite-study.png`; keduanya belum menjadi aset
runtime (studi batu masih mengandung block sekeliling). Ukuran baru memakai format preview v22, validasi seluruh tapak, rotasi dan
traversal multi-tile; bridge 1 tile lama tetap dibaca tanpa migrasi. Versi
lama tidak boleh mengganti world yang memiliki jembatan lebar. Dek dan tiang jembatan lebar kini digambar sebagai geometri terpisah;
tiang tetap vertikal pada setiap ukuran dan arah, sementara dek sejajar
sisi block. Sprite ilustrasi tetap dipakai di katalog.
Android sudah mendukung impor dan gambar v22 pada level kode, tetapi belum
diuji di perangkat.

Katalog menempatkan kembali jembatan 3×1, 4×1, dan 5×1; pegangan
menghubungkan empat tiang ujung pada tinggi satu block. Jembatan lebar yang
sudah ada tetap lebar dan tidak dimigrasi, agar world lama aman. Geometri
jembatan dan area sentuhnya memakai dek, tiang dan pegangan yang sama.
Outline gelap pada block yang ditempati karakter ditambahkan di preview dan
Android, mengikuti tile terdekat saat bergerak. Outline mengikuti bidang ramp dan digambar setelah dek jembatan. Tampilan
ketika tile tertutup objek belum diuji khusus.

Slider manual preview ada di bawah tablet saat rumah, tanaman, pagar,
jembatan atau satu tingkat block dipilih; rentang 0–90% per 5%, disimpan
ketika perubahan selesai. Tes block, rumah dan jembatan telah dijalankan;
tes gabungan tanaman/pagar belum berhasil diverifikasi pada putaran ini.
Fade otomatis block yang menutupi objek masih belum diterapkan; Android tidak
menyediakan editor slider dan pengujiannya di perangkat masih diperlukan.

Rute normal preview dan Android kini menolak masuk ke permukaan air, termasuk
ramp air, kecuali pada dek jembatan. Karakter dari world lama yang sudah
berada di air tetap dapat meninggalkannya; pemulihan otomatis menuju daratan
terdekat mempertahankan kemampuan menyeberangi air untuk menghindari karakter
terjebak. Perubahan Android baru diuji typecheck, bukan perangkat.

Dek jembatan mengikuti lebar tapak tersimpan tepat, dengan tiang dan pegangan
di tepinya; ketebalan visual tiang dan rail dapat menjorok sedikit. Area
pemilihan preview mengikuti bentuknya. Tool ramp preview memakai
satu bentuk; tombol kiri/kanan memutar arah sebelum penempatan atau setelah
ramp dipilih, dan tombol hapus menghapus ramp terpilih. Android membaca arah
ramp yang sama, tetapi tampilan dan interaksi belum diuji di perangkat.

Prototipe tampilan air: preview dan renderer Android menggambar jalur air
terjun miring dengan dua tepi penahan, beserta kilau bergerak di permukaan air.
Arah saat ini disimpulkan dari tetangga: turunan terdekat diprioritaskan,
selain itu memakai sumbu sambungan terpanjang (seri memilih timur). Ini tidak
menambah data atau mengubah world; arah cabang dan air diam belum dapat diatur
manual. Ramp air kini menampilkan aliran menuruni bidang dan batas samping; pertemuan
beberapa air terjun serta tampilan Android masih perlu diuji di perangkat.

Penempatan jembatan baru menggunakan format preview v24: ukuran 3×1, 4×1
atau 5×1 dengan kedua ujung darat setinggi sama. Bagian tengah harus berupa
lubang satu tingkat lebih rendah atau air (baik satu tingkat lebih rendah
maupun setingkat pada data). Jalur karakter selebar satu block; gambar sisi panjang
menjorok keluar dari tepi. Posisi karakter pada dek kosong tersimpan dan
penghapusan dek di bawahnya ditolak. Jembatan lama tidak dimigrasi atau
diubah aturannya, dan berkas lama tidak bisa menimpa data v24 ini. Parser,
rotasi, routing dan renderer Android mendukungnya dalam kode, tetapi belum
diuji dalam APK/perangkat.

Permukaan block air yang terlihat diturunkan secara visual sebesar seperempat
tinggi block; stack dan tinggi data tidak diubah. Permukaan air yang tertutup
air di atasnya tidak digambar sebagai bidang terpisah, dan dinding antar-air
bertetangga di level yang sama dihilangkan. Air terjun dan ramp air tetap
visual serta masih perlu pemeriksaan sambungan yang kompleks di perangkat.

## Penyesuaian terbaru: air dan jembatan bebas

Permukaan air kembali ke tinggi block semula; tidak ada perubahan data stack. Batas tanah luar tetap ada dan sambungan air sebelumnya dipertahankan. Jembatan baru memakai format v25: tinggi dek dicatat dari tile awal dan jembatan boleh melintasi tanah, air, lubang, perbedaan tinggi, atau ramp. Tapak harus tetap di dalam grid dan tidak menimpa rumah, pohon, pagar, atau jembatan lain. Medan yang lebih tinggi dapat menutupi dek dan ujung tidak selalu dapat dicapai; penempatan bebas bukan jaminan rute aman. Jembatan lama tidak diubah otomatis. Versi lama tidak dapat menimpa world dengan jembatan v25. Android belum diuji dalam APK atau perangkat.

## Penanda wilayah Map Editor

Preview Map Editor menampilkan garis hitam setiap 10×10 tile dan nomor besar
hitam 50% pada tiap wilayah. Nomor mengikuti koordinat world: bertambah menurut
kolom terlebih dahulu, lalu baris; pada grid 50×50 ada nomor 1–25. Rotasi kamera
memutar posisi wilayah bersama pulau, tetapi tidak mengubah identitas nomornya.
World lama dengan ukuran bukan kelipatan 10 memiliki wilayah parsial di tepi.
Garis mengikuti permukaan block dan ramp; wilayah tanpa permukaan tidak memiliki
nomor. Ini hanya overlay visual editor, bukan data world dan tidak tampil dalam
gameplay atau aplikasi Android.

Kamera Map Editor preview memiliki tiga tombol kemiringan: atas dan bawah
mengubah elevasi 3° per tekan dalam rentang 20°–38°, dan tombol tengah
mengembalikan elevasi ke 25°. Zoom dan arah putar horizontal tidak direset;
titik pandang tetap terpusat saat kemiringan berubah. Kemiringan editor disimpan
sementara saat beralih mode, sedangkan gameplay tetap menggunakan 25°. Tidak ada
perubahan data world dan kontrol ini tidak ditambahkan ke Android.

Ketiga tombol kemiringan berada di kiri atas tablet pada Map Editor preview.
Tombol mata di sebelahnya menyembunyikan/menampilkan objek yang ditempatkan:
rumah, tanaman, pagar dan jembatan beserta bayangannya. Karakter, block dan
penanda wilayah tetap terlihat. Objek tersembunyi tidak bisa dipilih dari
kanvas; pilihan objek aktif dibatalkan saat disembunyikan. Memulai penempatan
melalui katalog menampilkan objek kembali. Ini hanya pengaturan tampilan
sementara di editor, bukan penghapusan atau perubahan data world; gameplay dan
Android tetap menampilkan objek seperti biasa.

Pembaruan: rentang kemiringan Map Editor preview diperluas lima langkah di
masing-masing ujung, menjadi 5°–53° (tombol 3° per tekan, dengan batas akhir
dibulatkan ke 5°/53°). Default tetap 25° dan gameplay tetap pada default.
Outline gelap yang digambar pada sisi vertikal block dan sisi ramp dihilangkan
untuk semua material di preview dan renderer Android, tanpa menghapus bidang
sisi atau mengubah data. Garis permukaan rumput, tebing luar air, dan outline
seleksi/pijakan tetap dipertahankan. Renderer Android belum diuji di perangkat.

`artwork/bridge-sloped-ends-concept.png` adalah konsep yang dipilih untuk dek
jembatan dengan ujung menurun yang kini diterapkan di preview dan renderer Android.
Gambar konsep bukan acuan ukuran; lebar mengikuti tapak tersimpan dan tiang
hanya berada di ujung.

### Jembatan dan kamera editor
Dek jembatan kayu maupun batu mempunyai ramp lurus sepanjang satu tile pada masing-masing ujung, naik setengah tinggi block ke bagian tengah; rail mengikuti profil dan tiang tetap di empat sudut luar. Tapak dan data world v23–v25 tidak diubah. Gerakan karakter mengikuti profil visual; aturan rute dan ketinggian dek tersimpan tetap sama. Karena jembatan bebas dapat ditempatkan pada tinggi berbeda dari block di ujungnya, bentuk ini tidak menjamin sambungan fisik ke setiap block di bawahnya. Pemilihan jembatan di editor mengikuti profil; pemilihan tile gameplay Android masih memakai geometri tile datar.

Zoom awal Map Editor adalah 5×, terpisah dari gameplay 7,5×. Tombol bawah tablet berada satu baris dan dapat digeser horizontal bila ruang sempit; label suasana berada di baris berikutnya selebar tablet, dan slider tetap tersedia.

### Jembatan baru selebar dua block terpusat
Format sebelumnya menyediakan dek 3×2, 4×2, atau 5×2: satu tile tengah dan setengah tile di kedua sisi. Jembatan lama tidak diubah. Format world v26 menyimpan `centered:true` pada jembatan bebas; versi lama menolak data tersebut. Preview dan Android memvalidasi area sisi secara konservatif: seluruh tile yang tersentuh setengah dek harus berada di dalam grid dan bebas rumah, tanaman, pagar, serta jembatan lain. Block dan ramp tetap boleh ada di bawah dek. Ini berarti penempatan dekat tepi grid atau objek dapat ditolak.

Jalur gameplay masih menggunakan pusat tile, bukan jalur terpisah pada kedua setengah tile. Rail dan tiang jembatan format ini dibuat lebih tipis; ketebalannya masih dapat sedikit melewati tepi dek. Pada Android, pemilihan tile gameplay masih memakai geometri tile datar; renderer dan interaksi belum diuji di perangkat.

### Jembatan satu tile dengan tiang di dalam dek
Katalog kembali membuat jembatan 3×1, 4×1 dan 5×1. Dek mengikuti tepat satu tile; tiang dan rail lebih tipis dan diposisikan di dalam batasnya, termasuk pada kemiringan editor. Ujung rail dan tiang diletakkan sedikit ke dalam agar ketebalannya tidak melampaui dek. World v27 menyimpan penanda `inset:true`; bridge v26 dua tile dan bridge lama tidak diubah otomatis. Picking editor mengikuti bentuk baru dan jalur tetap pada tile tengah. Format lama ditolak saat hendak menimpa world yang memerlukan v27. Android baru diperiksa tipe dan parsing Java, belum diuji di perangkat.
