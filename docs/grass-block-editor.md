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
Android, mengikuti tile terdekat saat bergerak. Pada ramp dan ketika tile
tertutup objek, tampilan outline belum diuji khusus.

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
terjebak. Perubahan Android baru diuji typecheck, bukan perangkat. Aturan
jembatan khusus lubang, air terjun berbentuk ramp dan arah animasi air belum
diterapkan; aturan penempatan jembatan lama masih berlaku.

Sisi panjang dek dan pegangan jembatan kini menjorok 0,1 tile keluar dari
batas pada tiap sisi; tapak logis dan world tersimpan tidak diubah. Area
pemilihan preview mengikuti dek yang lebih lebar. Tool ramp preview memakai
satu bentuk; tombol kiri/kanan memutar arah sebelum penempatan atau setelah
ramp dipilih, dan tombol hapus menghapus ramp terpilih. Android membaca arah
ramp yang sama, tetapi tampilan dan interaksi belum diuji di perangkat.

Prototipe tampilan air: preview dan renderer Android menggambar jalur air
terjun miring dengan dua tepi penahan, beserta kilau bergerak di permukaan air.
Arah saat ini disimpulkan dari tetangga: turunan terdekat diprioritaskan,
selain itu memakai sumbu sambungan terpanjang (seri memilih timur). Ini tidak
menambah data atau mengubah world; arah cabang dan air diam belum dapat diatur
manual. Render dan sambungan pada ramp air, pertemuan beberapa air terjun,
serta Android perangkat masih perlu diuji dan disempurnakan. Aturan penempatan
jembatan khusus lubang belum diterapkan: format v22 lama mengharuskan seluruh
sel ditopang, sehingga dukungan lubang juga memerlukan perubahan validasi,
penyimpanan posisi player, routing dan impor Android yang kompatibel.
